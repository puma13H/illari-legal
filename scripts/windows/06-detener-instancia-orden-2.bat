@echo off
setlocal
cd /d "%~dp0\..\.."
powershell -NoProfile -ExecutionPolicy Bypass -File ".\scripts\detener-instancia-orden-2.ps1"
pause
