# Illari Legal — Unidad I de Sistemas Distribuidos

Proyecto académico de microservicios para las sesiones **S1–S4** y la **S5 — Evaluación de Unidad I**.

## Arquitectura

- **Java 21 + Spring Boot 4.0.7**
- **PostgreSQL 16 + Flyway**
- **Spring Cloud Config Server** con perfiles `dev` y `prod`
- **Eureka Server** para registro y descubrimiento
- **Spring Cloud Gateway** como punto único de acceso
- Cuatro microservicios REST: clientes, servicios legales, órdenes y pagos
- Dos instancias simultáneas de `orden-legal-ms` para demostrar balanceo
- Swagger/OpenAPI, Actuator y trazabilidad mediante `X-Instance-ID` / `X-Trace-ID`
- Observabilidad opcional de S4 con **Prometheus + Grafana**, dashboard provisionado automáticamente

## Estructura principal

```text
illari-legal/
├── infra/
│   ├── legal-config/
│   ├── legal-eureka/
│   └── legal-gateway/
├── services/
│   ├── cliente-ms/
│   ├── servicio-legal-ms/
│   ├── orden-legal-ms/
│   └── pago-ms/
├── docs/
├── obs/
│   ├── prometheus/
│   └── grafana/
├── scripts/
│   └── windows/
├── compose-dev.yml
├── docker-compose.yml
├── mkdocs.yml
└── pom.xml
```

## Ejecución en Windows

Los lanzadores están agrupados en `scripts/windows/` para mantener limpia la raíz del repositorio.

1. `scripts/windows/01-verificar-compilacion.bat`
2. `scripts/windows/02-configurar-entorno.bat`
3. `scripts/windows/03-iniciar-dev.bat`
4. Abrir Eureka: `http://localhost:18761`
5. `scripts/windows/04-demo-s5.bat`

Punto único de acceso en DEV: `http://localhost:18080`

Para detener el entorno: `scripts/windows/07-detener-dev.bat`.

### Observabilidad opcional (S4)

Con los servicios Java ya levantados, Prometheus y Grafana pueden iniciarse por separado:

```powershell
docker compose -f .\obs\compose.yml up -d prometheus
docker compose -f .\obs\compose.yml up -d grafana
```

- Prometheus: `http://localhost:19090/targets`
- Grafana: `http://localhost:13000`

El dashboard **Illari Legal — Unidad I** se carga automáticamente. Más detalles en `docs/observabilidad.md`.

## Documentación de la evaluación

La documentación principal está en:

- `docs/u1-producto.md`
- `docs/guia-ejecucion.md`
- `docs/demo-s5.md`
- `docs/brief-tecnico.md`

El sitio MkDocs se configura mediante `mkdocs.yml`.

> Este repositorio contiene únicamente los componentes necesarios para la Unidad I. El backend actual está distribuido entre `services/` e `infra/`; la interfaz web se incorporará en una etapa posterior.
