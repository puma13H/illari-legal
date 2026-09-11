# Brief técnico

## Nombre

**Illari Legal — Plataforma distribuida para servicios legales**

## Objetivo

Construir una base distribuida que permita gestionar clientes, catálogo jurídico, órdenes de atención y pagos, aplicando persistencia, configuración centralizada, descubrimiento, ejecución concurrente, Gateway y balanceo.

## Stack

- Java 21.
- Spring Boot 4.0.7.
- Spring Cloud 2025.1.3.
- PostgreSQL 16.
- Flyway.
- Eureka.
- Spring Cloud Config.
- Spring Cloud Gateway Server Web MVC.
- Springdoc OpenAPI.
- Actuator + Prometheus.
- Docker Compose.

## Decisiones

1. Cada microservicio de negocio es dueño de una base lógica.
2. Flyway es dueño del esquema; Hibernate usa `ddl-auto=validate`.
3. No se versionan contraseñas; se inyectan mediante variables de entorno.
4. `orden-legal-ms` es el servicio seleccionado para demostrar concurrencia y balanceo.
5. En PROD los servicios de negocio no publican puertos al host; el Gateway es el punto de acceso.
6. La entrega de Unidad I contiene únicamente la arquitectura distribuida actual. El frontend se incorporará posteriormente y consumirá los servicios a través del Gateway.
