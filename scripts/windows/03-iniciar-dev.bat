@echo off
setlocal
cd /d "%~dp0\..\.."
powershell -NoProfile -ExecutionPolicy Bypass -File ".\scripts\iniciar-u1-dev.ps1"
if errorlevel 1 (
  echo.
  echo Ocurrio un error al iniciar la Unidad I.
  pause
  exit /b 1
)
