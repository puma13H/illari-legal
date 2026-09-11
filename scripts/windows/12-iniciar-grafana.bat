@echo off
setlocal
cd /d "%~dp0\..\.."
docker compose -f ".\obs\compose.yml" up -d grafana
if errorlevel 1 exit /b %errorlevel%
echo.
echo Grafana: http://localhost:13000
echo Dashboard provisionado: Illari Legal - Unidad I
pause
