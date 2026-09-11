# Docker — Illari Legal Unidad I

La configuración Docker vigente está en `docker-compose.yml`.

## Inicio PROD local

1. Ejecutar `scripts/windows/02-configurar-entorno.bat`.
2. Ejecutar `scripts/windows/08-iniciar-prod-docker.bat`.
3. Usar el Gateway en `http://localhost:28080`.

Administración:
- Config Server: `http://localhost:28888`
- Eureka: `http://localhost:28761`

No se versionan contraseñas. `.env` se genera localmente.

Para el entorno de exposición DEV consulta `docs/guia-ejecucion.md`.
