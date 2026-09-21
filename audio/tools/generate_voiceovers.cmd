@echo off
setlocal EnableDelayedExpansion

set "BUNDLED_PYTHON=%USERPROFILE%\.cache\codex-runtimes\codex-primary-runtime\dependencies\python\python.exe"
if exist "%BUNDLED_PYTHON%" (
  "%BUNDLED_PYTHON%" "%~dp0generate_voiceovers.py" %*
  exit /b !ERRORLEVEL!
)

where python >nul 2>nul
if !ERRORLEVEL! EQU 0 (
  python "%~dp0generate_voiceovers.py" %*
  exit /b !ERRORLEVEL!
)

where py >nul 2>nul
if !ERRORLEVEL! EQU 0 (
  py "%~dp0generate_voiceovers.py" %*
  exit /b !ERRORLEVEL!
)

echo ERROR: Python 3 could not be found. 1>&2
exit /b 1
