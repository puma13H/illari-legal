# Revisión final S1–S5

Fecha de preparación: 10 de septiembre de 2026.

## Estado del código

| Sesión | Estado de implementación | Evidencia en el repositorio |
|---|---|---|
| S1 | Implementada | CRUD, PostgreSQL, Flyway, Swagger, Actuator, validaciones |
| S2 | Implementada | Config Server + perfiles DEV/PROD externos |
| S3 | Implementada | Eureka + IDs de instancia + 2 instancias de orden |
| S4 | Implementada | Gateway + `lb://` + encabezado de instancia |
| S5 | Preparada | producto U1, demo CRUD/error/balanceo, MkDocs |

## Comprobaciones estáticas realizadas

- Los POM de los componentes de Unidad I usan Java 21 y Spring Boot 4.0.7.
- Los cuatro microservicios incluyen Flyway y Springdoc.
- Existen migraciones `V1` y `V2` para los cuatro microservicios.
- El Config Server contiene archivos `-dev.yml` y `-prod.yml`.
- No existen archivos `-docker.yml` en `config-repo`.
- El Gateway posee cuatro rutas mediante `lb://`.
- Docker Compose declara dos contenedores de `orden-legal-ms`.
- `.env` está ignorado por Git.
- No se almacenan contraseñas reales en los perfiles de Unidad I.
- Los archivos YAML de la aplicación y los POM fueron validados sintácticamente.
- El código Java fue pasado por el parser de `javac`; no se detectaron errores sintácticos.

## Comprobación pendiente en el equipo del estudiante

El entorno usado para preparar este ZIP no dispone de acceso de red desde la terminal ni de Docker/Maven con dependencias cacheadas, por lo que no fue posible ejecutar aquí un `mvn package` real ni levantar contenedores.

En el equipo final se debe ejecutar:

1. `scripts/windows/02-configurar-entorno.bat`
2. `scripts/windows/03-iniciar-dev.bat`
3. `scripts/windows/04-demo-s5.bat`

Si esas tres etapas terminan correctamente, capturar las evidencias indicadas en `docs/evidencias/README.md`.

## Acciones externas que un ZIP no puede completar

- Publicar el repositorio en GitHub.
- Configurar los Topics del repositorio.
- Ejecutar GitHub Pages.

El repositorio incluye `docs/guias/configurar-github.md` y el workflow `.github/workflows/docs.yml` para completar esas acciones al hacer push.
