# Guía de ejecución

## Requisitos

- Windows 11 o equivalente.
- Java 21.
- Docker Desktop con Docker Compose.
- Acceso a Internet la primera vez para descargar dependencias Maven e imágenes Docker.

No es obligatorio tener Maven instalado globalmente: cada proyecto incluye `mvnw.cmd`, que usa Maven instalado si existe y, en caso contrario, descarga Maven 3.9.16.

## Opción A — DEV, recomendada para la exposición

1. Ejecutar `scripts/windows/02-configurar-entorno.bat` una sola vez.
2. Ejecutar `scripts/windows/03-iniciar-dev.bat`.
3. Esperar hasta que la consola principal muestre `UNIDAD I LEVANTADA EN DEV`.
4. Abrir Eureka en `http://localhost:18761`.
5. Ejecutar `scripts/windows/04-demo-s5.bat`.

Puertos:

| Componente | Puerto |
|---|---:|
| Config Server | 18888 |
| Eureka | 18761 |
| Gateway | 18080 |
| cliente-ms | 8081 |
| servicio-legal-ms | 8082 |
| orden-legal-ms instancia 1 | 8083 |
| pago-ms | 8084 |
| orden-legal-ms instancia 2 | 8085 |
| PostgreSQL | 15432 |

Para detener todo: `scripts/windows/07-detener-dev.bat`.

## Opción B — PROD local completamente en Docker

1. Ejecutar `scripts/windows/02-configurar-entorno.bat`.
2. Ejecutar `scripts/windows/08-iniciar-prod-docker.bat`.
3. Abrir:
   - Config Server: `http://localhost:28888`
   - Eureka: `http://localhost:28761`
   - Gateway: `http://localhost:28080`
4. Probar balanceo con `scripts/windows/09-probar-balanceo-prod.bat`.

Los microservicios de negocio no exponen puertos al host en PROD.

## Swagger en DEV

- Cliente: `http://localhost:8081/swagger-ui.html`
- Servicio legal: `http://localhost:8082/swagger-ui.html`
- Orden: `http://localhost:8083/swagger-ui.html`
- Pago: `http://localhost:8084/swagger-ui.html`

## Config Server

Pruebas útiles:

- `http://localhost:18888/orden-legal-ms/dev`
- `http://localhost:18888/orden-legal-ms/prod`
- `http://localhost:18888/legal-gateway/dev`

## Restablecer las BD DEV

Si necesitas iniciar Flyway desde cero:

```powershell
docker compose -f compose-dev.yml down -v
docker compose -f compose-dev.yml up -d
```

Esto elimina únicamente el volumen DEV de esta Unidad I.
