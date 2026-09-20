THE INFECTED HOUR - AUDIO PRODUCTION FOLDER
==========================================

Purpose
-------
This folder is the single handoff/workspace for audio production. Keep source
and delivery MP3 files here together with dialogue scripts and the integration
tracker.

Folder layout
-------------
dialogues/
  ALL_DIALOGUES.txt              Recording script for every active voice line.
  DISABLED_DIALOGUES.txt         Archived lines that must not be enabled yet.

mp3/music/
  Put the mastered music MP3 files here.

mp3/sfx/
  Put the mastered sound-effect MP3 files here.

mp3/voice/
  Put the mastered narration/dialogue MP3 files here.

AUDIO_CINEMATIC_IMPLEMENTATION_TRACKER.txt
  Records every cinematic image, audio filename, runtime destination, code
  owner, and current integration status.

Important runtime format rule
-----------------------------
The Java/libGDX game expects OGG files, not MP3 files. MP3 is kept here as the
production/delivery format requested by the team. Before an asset is used by
the game, export or convert a matching OGG copy and place it under:

  assets/audio/music/
  assets/audio/sfx/
  assets/audio/voice/

Example:

  audio/mp3/voice/intro_01.mp3
      -> assets/audio/voice/intro_01.ogg

Do not rename the base filename during conversion. The code already refers to
the OGG names recorded in the tracker.

Asset rules
-----------
1. Do not commit empty placeholder .mp3 or .ogg files.
2. Record the creator/source and license in ASSETS_CREDITS.md.
3. Prefer 44.1 kHz or 48 kHz audio with consistent loudness across each group.
4. Keep narration clear and leave headroom so music can sit underneath it.
5. Update the tracker whenever an asset is added, converted, connected, moved,
   or replaced.
6. Level 6 gameplay and its owned files are teammate-controlled. Do not edit
   them unless the user explicitly requests a Level 6 change.

