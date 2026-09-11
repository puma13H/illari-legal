@echo off
setlocal
cd /d "%~dp0\..\.."
powershell -NoProfile -ExecutionPolicy Bypass -File ".\scripts\detener-u1-dev.ps1"
pause
