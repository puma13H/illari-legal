@echo off
setlocal
cd /d "%~dp0\..\.."
if not exist ".env" call ".\scripts\windows\02-configurar-entorno.bat"
docker compose up -d --build
if errorlevel 1 (
  echo.
  echo ERROR: no se pudo levantar el entorno PROD Docker.
  pause
  exit /b 1
)
echo.
echo PROD levantado correctamente.
echo Config : http://localhost:28888
echo Eureka : http://localhost:28761
echo Gateway: http://localhost:28080
pause
