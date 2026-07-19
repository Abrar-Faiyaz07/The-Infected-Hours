@echo off
REM /init entry point — resume work on The Infected Hour with Claude Code.

echo === The Infected Hour v2.0 (6-day sprint) — resume point ===
echo.
echo Modules: shared / core / lwjgl3 / fx-launcher / backend
echo Source specs are in docs/ (PRD, TRD, UI-UX, App Flow, Backend Schema) — treat as authoritative.
echo.
echo Run backend:        gradlew :backend:bootRun
echo Run full game (FX):  gradlew :fx-launcher:run
echo Run dev shortcut:    gradlew :lwjgl3:run   (host-solo, skips JavaFX launcher)
echo Run all tests:       gradlew test
echo.
echo Definition of Done checklist lives in README.md.
