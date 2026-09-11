# Illari Legal — Unidad I

Proyecto académico de sistema distribuido para gestión de servicios legales.

Esta versión integra de forma acumulativa los resultados de las sesiones **S1, S2, S3 y S4** y deja preparada la demostración correspondiente a **S5 — Evaluación de la Unidad I**.

## Componentes

| Componente | Función |
|---|---|
| `legal-config` | Configuración centralizada DEV/PROD |
| `legal-eureka` | Registro y descubrimiento de servicios |
| `legal-gateway` | Punto único de acceso y balanceo con `lb://` |
| `cliente-ms` | CRUD persistente de clientes |
| `servicio-legal-ms` | CRUD persistente de servicios legales |
| `orden-legal-ms` | CRUD persistente y dos instancias concurrentes |
| `pago-ms` | CRUD persistente de pagos |
| PostgreSQL 16 | Servidor de BD con una base lógica por microservicio |
| Flyway | Versionado del esquema de cada microservicio |

## Acceso DEV

- Config Server: `http://localhost:18888`
- Eureka: `http://localhost:18761`
- Gateway: `http://localhost:18080`
- Segunda instancia de órdenes: puerto `8085`

Para la evaluación se debe consumir el negocio mediante el **Gateway**, no mediante los puertos internos de cada servicio.
