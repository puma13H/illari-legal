@echo off
setlocal
cd /d "%~dp0\..\.."
docker compose -f ".\obs\compose.yml" up -d prometheus
if errorlevel 1 exit /b %errorlevel%
echo.
echo Prometheus: http://localhost:19090
echo Targets   : http://localhost:19090/targets
pause
