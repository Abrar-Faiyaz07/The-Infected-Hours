# Project collaboration boundaries

## Level 6 / `level_final` ownership

The final level is owned and implemented by the user's teammate. Treat the
teammate's Level 6 work as authoritative and accept it as-is.

- Do not rewrite, replace, revert, or "clean up" `level_final` gameplay, map,
  boss logic, or assets unless the user explicitly asks for a Level 6 change.
- This includes `core/src/main/resources/maps/level_final.map`,
  `assets/map_final.png`, `BossScreen.java`, and Level 6-specific sections in
  shared files such as `GameScreen.java` and `LevelDefinition.java`.
- When incoming teammate work conflicts with Codex changes, preserve the
  teammate's Level 6 implementation and adapt Levels 1–5 around it.
- Shared-file edits must stay outside Level 6-specific branches whenever
  possible. Never use broad rewrites that could erase teammate changes.
- If Level 6 work is unavailable, incomplete, or cannot be reviewed because of
  limits, continue with other levels and leave `level_final` untouched.

