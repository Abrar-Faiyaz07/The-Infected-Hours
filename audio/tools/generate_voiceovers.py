#!/usr/bin/env python3
"""Generate, cache, mix, and encode cinematic voice-over assets.

No third-party Python package is required. The script calls the OpenAI Speech
API directly, caches every spoken paragraph as WAV, joins each scene with
speaker-aware pauses, then uses ffmpeg to produce production MP3 and runtime
OGG copies.
"""

from __future__ import annotations

import argparse
import concurrent.futures
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import urllib.error
import urllib.request
import wave


ROOT = Path(__file__).resolve().parents[2]
SOURCE = ROOT / "audio" / "dialogues" / "CINEMATIC_DIALOGUE_VERIFIED.txt"
CAST_FILE = ROOT / "audio" / "voice-production" / "voice_cast.json"
WORK_ROOT = ROOT / "audio" / "voice-production" / "generated"
SEGMENT_ROOT = WORK_ROOT / "segments"
MIX_ROOT = WORK_ROOT / "scene-wav"
MP3_ROOT = ROOT / "audio" / "mp3" / "voice"
OGG_ROOT = ROOT / "assets" / "audio" / "voice"
PORTABLE_FFMPEG_ROOT = ROOT / "audio" / "voice-production" / "tools" / "package"
API_URL = "https://api.openai.com/v1/audio/speech"

SCENE_RE = re.compile(r"^SCENE\s+(\d+)\s*[-—]\s*(.+?\.png)\s*$", re.IGNORECASE)
SPEAKER_RE = re.compile(r"^Speaker:\s*(.+?)\s*$", re.IGNORECASE)


def parse_source(path: Path) -> list[dict]:
    scenes: list[dict] = []
    current_scene: dict | None = None
    current_speaker: str | None = None

    for raw_line in path.read_text(encoding="utf-8-sig").splitlines():
        line = raw_line.strip()
        if not line:
            continue

        scene_match = SCENE_RE.match(line)
        if scene_match:
            image_name = scene_match.group(2)
            current_scene = {
                "number": int(scene_match.group(1)),
                "image": image_name,
                "stem": Path(image_name).stem,
                "segments": [],
            }
            scenes.append(current_scene)
            current_speaker = None
            continue

        speaker_match = SPEAKER_RE.match(line)
        if speaker_match:
            current_speaker = speaker_match.group(1).replace("—", "-").strip()
            continue

        if line.lower() == "computer":
            current_speaker = "Computer"
            continue

        if current_scene is None or line.startswith("GAMEPLAY:"):
            continue

        if (line.startswith('"') and line.endswith('"')) or (
            line.startswith("“") and line.endswith("”")
        ):
            if current_speaker is None:
                raise ValueError(f"Dialogue has no speaker in scene {current_scene['number']}: {line}")
            text = line[1:-1].strip()
            current_scene["segments"].append({"speaker": current_speaker, "text": text})

    if not scenes:
        raise ValueError("No scenes were found in the verified dialogue source")
    for scene in scenes:
        if not scene["segments"]:
            raise ValueError(f"Scene {scene['number']} has no spoken dialogue")
    return scenes


def load_cast(path: Path) -> dict:
    config = json.loads(path.read_text(encoding="utf-8"))
    required = {"model", "format", "cast"}
    missing = required.difference(config)
    if missing:
        raise ValueError(f"Voice cast config is missing: {', '.join(sorted(missing))}")
    return config


def safe_speaker_name(speaker: str) -> str:
    return re.sub(r"[^a-z0-9]+", "_", speaker.lower()).strip("_")


def segment_paths(scene: dict, index: int, speaker: str) -> tuple[Path, Path]:
    base = f"{index:02d}_{safe_speaker_name(speaker)}"
    scene_dir = SEGMENT_ROOT / scene["stem"]
    return scene_dir / f"{base}.wav", scene_dir / f"{base}.json"


def cache_signature(model: str, voice: str, instructions: str, text: str) -> str:
    value = json.dumps(
        {"model": model, "voice": voice, "instructions": instructions, "text": text},
        sort_keys=True,
        ensure_ascii=False,
    )
    return hashlib.sha256(value.encode("utf-8")).hexdigest()


def request_speech(api_key: str, payload: dict, output_path: Path) -> None:
    request = urllib.request.Request(
        API_URL,
        data=json.dumps(payload, ensure_ascii=False).encode("utf-8"),
        headers={
            "Authorization": f"Bearer {api_key}",
            "Content-Type": "application/json",
        },
        method="POST",
    )
    output_path.parent.mkdir(parents=True, exist_ok=True)
    temporary_path = output_path.with_suffix(".wav.part")
    try:
        with urllib.request.urlopen(request, timeout=240) as response:
            temporary_path.write_bytes(response.read())
        temporary_path.replace(output_path)
    except urllib.error.HTTPError as error:
        detail = error.read().decode("utf-8", errors="replace")
        raise RuntimeError(f"Speech API returned HTTP {error.code}: {detail}") from error
    finally:
        temporary_path.unlink(missing_ok=True)


def generate_segment(task: dict, api_key: str, model: str, force: bool) -> str:
    scene = task["scene"]
    index = task["index"]
    segment = task["segment"]
    cast = task["cast"]
    wav_path, metadata_path = segment_paths(scene, index, segment["speaker"])
    signature = cache_signature(model, cast["voice"], cast["instructions"], segment["text"])

    if not force and wav_path.exists() and metadata_path.exists():
        metadata = json.loads(metadata_path.read_text(encoding="utf-8"))
        if metadata.get("signature") == signature:
            return f"cached {scene['stem']} #{index:02d}"

    payload = {
        "model": model,
        "voice": cast["voice"],
        "input": segment["text"],
        "instructions": cast["instructions"],
        "response_format": "wav",
    }
    request_speech(api_key, payload, wav_path)
    metadata_path.write_text(
        json.dumps(
            {
                "signature": signature,
                "model": model,
                "voice": cast["voice"],
                "speaker": segment["speaker"],
                "text": segment["text"],
            },
            indent=2,
            ensure_ascii=False,
        )
        + "\n",
        encoding="utf-8",
    )
    return f"generated {scene['stem']} #{index:02d}"


def silence_frames(parameters: wave._wave_params, milliseconds: int) -> bytes:
    frame_count = round(parameters.framerate * milliseconds / 1000)
    return bytes(frame_count * parameters.nchannels * parameters.sampwidth)


def mix_scene(scene: dict) -> Path:
    inputs: list[tuple[Path, str]] = []
    for index, segment in enumerate(scene["segments"], start=1):
        wav_path, _ = segment_paths(scene, index, segment["speaker"])
        inputs.append((wav_path, segment["speaker"]))

    MIX_ROOT.mkdir(parents=True, exist_ok=True)
    output = MIX_ROOT / f"{scene['stem']}.wav"
    with wave.open(str(inputs[0][0]), "rb") as first:
        parameters = first.getparams()
        expected = (parameters.nchannels, parameters.sampwidth, parameters.framerate, parameters.comptype)

    with wave.open(str(output), "wb") as destination:
        destination.setparams(parameters)
        previous_speaker: str | None = None
        for wav_path, speaker in inputs:
            with wave.open(str(wav_path), "rb") as source:
                actual = (source.getnchannels(), source.getsampwidth(), source.getframerate(), source.getcomptype())
                if actual != expected:
                    raise ValueError(f"Incompatible WAV format in {wav_path}: {actual} != {expected}")
                if previous_speaker is not None:
                    pause_ms = 650 if speaker != previous_speaker else 350
                    destination.writeframes(silence_frames(parameters, pause_ms))
                destination.writeframes(source.readframes(source.getnframes()))
                previous_speaker = speaker
        destination.writeframes(silence_frames(parameters, 700))
    return output


def encode_scene(ffmpeg: str, wav_path: Path, stem: str) -> None:
    MP3_ROOT.mkdir(parents=True, exist_ok=True)
    OGG_ROOT.mkdir(parents=True, exist_ok=True)
    loudness = "loudnorm=I=-16:TP=-1.5:LRA=11"
    common = [ffmpeg, "-hide_banner", "-loglevel", "error", "-y", "-i", str(wav_path), "-af", loudness]
    subprocess.run(common + ["-c:a", "libmp3lame", "-b:a", "192k", str(MP3_ROOT / f"{stem}.mp3")], check=True)
    subprocess.run(common + ["-c:a", "libvorbis", "-q:a", "5", str(OGG_ROOT / f"{stem}.ogg")], check=True)


def select_scenes(scenes: list[dict], requested: list[str] | None) -> list[dict]:
    if not requested:
        return scenes
    wanted = set(requested)
    available = {scene["stem"] for scene in scenes}
    unknown = wanted.difference(available)
    if unknown:
        raise ValueError(f"Unknown scene name(s): {', '.join(sorted(unknown))}")
    return [scene for scene in scenes if scene["stem"] in wanted]


def validate(scenes: list[dict], config: dict) -> None:
    configured = config["cast"]
    speakers = {segment["speaker"] for scene in scenes for segment in scene["segments"]}
    missing = speakers.difference(configured)
    if missing:
        raise ValueError(f"No voice configured for: {', '.join(sorted(missing))}")
    stems = [scene["stem"] for scene in scenes]
    if len(stems) != len(set(stems)):
        raise ValueError("Duplicate cinematic filenames were found")


def find_ffmpeg() -> str | None:
    configured = os.environ.get("FFMPEG_PATH")
    if configured:
        return configured
    system_ffmpeg = shutil.which("ffmpeg")
    if system_ffmpeg:
        return system_ffmpeg
    if PORTABLE_FFMPEG_ROOT.exists():
        portable = next(PORTABLE_FFMPEG_ROOT.rglob("ffmpeg.exe"), None)
        if portable:
            return str(portable)
    return None


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--dry-run", action="store_true", help="Validate and print the production plan without calling the API")
    parser.add_argument("--scene", action="append", dest="scenes", help="Generate only this scene stem; may be repeated")
    parser.add_argument("--force", action="store_true", help="Regenerate cached speech segments")
    parser.add_argument("--jobs", type=int, default=4, help="Maximum simultaneous API calls (default: 4)")
    args = parser.parse_args()

    scenes = select_scenes(parse_source(SOURCE), args.scenes)
    config = load_cast(CAST_FILE)
    validate(scenes, config)
    segment_count = sum(len(scene["segments"]) for scene in scenes)
    print(f"Validated {len(scenes)} scenes and {segment_count} spoken segments.")
    for scene in scenes:
        cast_names = ", ".join(dict.fromkeys(s["speaker"] for s in scene["segments"]))
        print(f"  {scene['stem']}: {len(scene['segments'])} segments ({cast_names})")
    if args.dry_run:
        return 0

    api_key = os.environ.get("OPENAI_API_KEY")
    if not api_key:
        raise RuntimeError("OPENAI_API_KEY is not set. No API requests were made.")
    ffmpeg = find_ffmpeg()
    if not ffmpeg:
        raise RuntimeError(
            "ffmpeg is required for MP3 and OGG delivery. Install it or set FFMPEG_PATH. "
            "No API requests were made."
        )

    tasks: list[dict] = []
    for scene in scenes:
        for index, segment in enumerate(scene["segments"], start=1):
            tasks.append(
                {
                    "scene": scene,
                    "index": index,
                    "segment": segment,
                    "cast": config["cast"][segment["speaker"]],
                }
            )

    workers = max(1, min(args.jobs, 8))
    with concurrent.futures.ThreadPoolExecutor(max_workers=workers) as executor:
        futures = [
            executor.submit(generate_segment, task, api_key, config["model"], args.force)
            for task in tasks
        ]
        for future in concurrent.futures.as_completed(futures):
            print(future.result())

    for scene in scenes:
        wav_path = mix_scene(scene)
        encode_scene(ffmpeg, wav_path, scene["stem"])
        print(f"mixed and encoded {scene['stem']}")

    print(f"Runtime OGG files: {OGG_ROOT}")
    print(f"Production MP3 files: {MP3_ROOT}")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except (OSError, ValueError, RuntimeError, subprocess.CalledProcessError) as error:
        print(f"ERROR: {error}", file=sys.stderr)
        raise SystemExit(1)
