# Observabilidad de ILLARI LEGAL

Grafana consulta las métricas de Prometheus y los registros de Loki. Prometheus lee las métricas de Spring Boot y de Keycloak, y recoge las sondas de disponibilidad de Blackbox Exporter. Alloy recoge los registros de las aplicaciones y los envía a Loki.

## Enlaces del sistema

El comando `proyecto.ps1 iniciar` muestra la URL HTTPS del portal si se configuró el túnel de Cloudflare. Los enlaces de abajo se abren desde la computadora que ejecuta Docker Desktop.

| Componente | Enlace local | Qué muestra |
| --- | --- | --- |
| Portal Angular | [Portal local](http://127.0.0.1:4200/) | Interfaz de cliente, abogado y administrador. |
| Gateway | [Catálogo a través del Gateway](http://127.0.0.1:18080/api/v1/servicios) | Respuesta de Catálogo enrutada por el Gateway. |
| Grafana | [Tablero de ILLARI LEGAL](http://127.0.0.1:3000/d/illari-s12-overview/illari-legal-observabilidad) | Disponibilidad, tiempos de respuesta, pedidos, pagos, eventos y logs correlacionados. |
| Prometheus | [Objetivos de monitoreo](http://127.0.0.1:9090/targets) | Siete orígenes de métricas y siete sondas de disponibilidad. |
| Blackbox Exporter | [Sondas HTTP](http://127.0.0.1:19115/) | Exportador que verifica las rutas de disponibilidad. |
| Alloy | [Recolector de logs](http://127.0.0.1:12345/) | Componentes de lectura de archivos y envío a Loki. |
| Loki | [Disponibilidad de Loki](http://127.0.0.1:13100/ready) | Estado de Loki. Para buscar registros, abre el tablero de Grafana. |
| Keycloak | [Consola de Keycloak](http://localhost:18180/admin/master/console/) | Gestión del realm, cuentas y roles. Usa los valores de `KEYCLOAK_ADMIN` y `KEYCLOAK_ADMIN_PASSWORD` en `credenciales/runtime.env`. |

En Grafana, el usuario es `admin`. La contraseña inicial se genera en `credenciales/grafana-admin.txt` y se conserva entre arranques. En una instalación preparada con `-Aislado`, los puertos de Grafana, Prometheus, Blackbox, Alloy y Loki son `23000`, `29090`, `29115`, `22345` y `23100`, respectivamente. Los puertos del portal, el Gateway y Keycloak se detallan en el [manual de operación](OPERACION.md#primera-instalación).

Eureka, Config Server y los microservicios usan puertos locales asignados por Docker. Ejecuta `proyecto.ps1 estado` para ver el puerto de cada contenedor. Abre `/` en el registro de servicios y `/actuator/health` en la configuración central. Las API de Clientes, Catálogo, Pedidos, Pagos y Expedientes ofrecen `/actuator/health/readiness`. Kafka y PostgreSQL se conectan por sus protocolos en la red de Docker.

## Cómo mostrar la conexión en la exposición

1. Abre **Prometheus → Targets**. Los objetivos son el Gateway, Clientes, Catálogo, Pedidos, Pagos, Expedientes y Keycloak. El grupo `illari-readiness` muestra las sondas que Prometheus recibe de Blackbox para esos mismos servicios.
2. Abre **Grafana → ILLARI LEGAL - Observabilidad**. La primera fila muestra la disponibilidad de Keycloak, el uso de memoria de su JVM y las conexiones a PostgreSQL. Dentro de Docker, Prometheus consulta `keycloak:9000/metrics` y Blackbox verifica `keycloak:9000/health/ready`. Los paneles de Kafka muestran métricas registradas por los servicios que procesan los eventos.
3. Genera una consulta de solo lectura al catálogo y copia el identificador que imprime PowerShell.

   ```powershell
   $traceId = [guid]::NewGuid().ToString()
   $catalogo = Invoke-WebRequest -UseBasicParsing -Uri 'http://127.0.0.1:18080/api/v1/servicios' -Headers @{ 'X-Trace-ID' = $traceId }
   $catalogo.StatusCode
   $traceId
   ```

4. Pega el identificador en el campo `traceId` de Grafana y confirma con Enter. El panel **Logs correlacionados por traceId** muestra la solicitud en `legal-gateway` y `servicio-legal-ms`. Si acaba de ejecutarse, espera unos segundos para que Alloy envíe los registros a Loki.

El 19 de noviembre de 2026 se verificó el acceso al tablero, las conexiones de ambas fuentes de datos, siete sondas exitosas y una consulta HTTP 200 con registros de los dos servicios para la traza `89f11f8a-3a92-483d-bc0b-7b7282f2ed50`. Eureka mostró seis aplicaciones registradas con estado UP. Kafka listó los topics `illari.orden.creada` e `illari.pago.resultado`, y Prometheus devolvió métricas de eventos procesados en Pedidos y Pagos. PostgreSQL aceptó conexiones. La consulta al catálogo puede repetirse para la defensa.
