@echo off
setlocal
cd /d "%~dp0\..\.."
docker compose -f ".\obs\compose.yml" down
echo Observabilidad detenida. PostgreSQL y los microservicios no fueron detenidos.
pause
