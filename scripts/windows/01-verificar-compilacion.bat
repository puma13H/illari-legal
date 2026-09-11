@echo off
setlocal
cd /d "%~dp0\..\.."
echo ==============================================
echo  ILLARI LEGAL - Verificacion de compilacion
echo ==============================================
echo.
call mvnw.cmd clean package -DskipTests
if errorlevel 1 (
  echo.
  echo ERROR: la compilacion no termino correctamente.
  pause
  exit /b 1
)
echo.
echo COMPILACION CORRECTA.
pause
