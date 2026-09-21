ASHGROVE CINEMATIC VOICE PRODUCTION
===================================

Source dialogue
---------------
audio/dialogues/CINEMATIC_DIALOGUE_VERIFIED.txt

Cast and performance directions
-------------------------------
audio/voice-production/voice_cast.json

Generator
---------
audio/tools/generate_voiceovers.py

The generator uses OpenAI gpt-4o-mini-tts. It creates and caches one WAV per
spoken paragraph so an individual line can be revised without regenerating a
whole scene. It then mixes each cinematic and exports:

  audio/mp3/voice/<scene>.mp3       production/delivery master
  assets/audio/voice/<scene>.ogg   libGDX runtime asset

Requirements
------------
1. Python 3 (no third-party Python package is needed).
2. An OPENAI_API_KEY environment variable with Speech API access.
3. ffmpeg with libmp3lame and libvorbis encoders. The generator automatically
   finds the portable copy under audio/voice-production/tools when present.

Safe validation (no API cost)
-----------------------------
audio\tools\generate_voiceovers.cmd --dry-run

Generate everything
-------------------
audio\tools\generate_voiceovers.cmd

Generate or revise one cinematic
--------------------------------
audio\tools\generate_voiceovers.cmd --scene intro_jane --force

If ffmpeg is kept as a portable executable instead of being installed on PATH,
set FFMPEG_PATH to its full path before running the generator.

The game must disclose that these voices are AI-generated. Keep that notice in
the game credits and distribution documentation.

Manual recording import
-----------------------
User-created MP3 parts are copied from E:\HP into:

  audio/mp3/voice/segments/

Scene mixes are normalized to -16 LUFS, separated by 650 ms between speakers,
and exported to the production MP3 and runtime OGG folders listed above.
The original files in E:\HP are never moved, renamed, or deleted.
