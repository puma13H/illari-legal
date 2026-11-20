# Organización del código

## Carpetas activas

| Carpeta | Qué contiene |
| --- | --- |
| `frontend/portal-web/src/app/` | Único frontend Angular, organizado por cliente, abogado, administración, servicios, pagos y componentes compartidos. |
| `backend/expedientes/` | Expedientes, profesionales, documentos, agenda y registro de accesos. |
| `backend/microservicios/clientes/` | Perfil de la persona que compra servicios. |
| `backend/microservicios/catalogo-servicios/` | Catálogo y precios. |
| `backend/microservicios/pedidos/` | Creación del pedido, sus detalles y entrega de la guía comprada. |
| `backend/microservicios/pagos/` | Cobro de Yape mediante Mercado Pago y publicación del resultado. |
| `backend/compartido/` | Una sola implementación de correlación, registros seguros, filtro HTTP, validación JWT y formato de eventos. |
| `infraestructura/` | Keycloak, Gateway, Config Server, Eureka, Docker, túnel HTTPS y observabilidad. |
| `base-de-datos/` | Esquemas y migraciones; son necesarios para instalar y actualizar las bases. |
| `herramientas/` | Operación, verificación, sincronización de URL y generación del PDF. |

`pom.xml` reúne los nueve módulos Java. La biblioteca `illari-compartido` se incluye dentro de los JAR de los servicios; no es otro contenedor ni otra base de datos.

## Duplicidad retirada

Se sustituyeron seis copias de `Correlation`, seis de `SafeObservationLayout`, dos de `EventEnvelope` y dos de `ObservationCommit` por cuatro clases del módulo compartido. También se centralizó el filtro de trazabilidad que estaba copiado en cinco API y la política de validación JWT que repetían seis módulos; cada aplicación conserva su configuración HTTP o reactiva. Las importaciones y las referencias de Logback usan la ubicación nueva. La lógica de pago, los identificadores de eventos y el contenido de los mensajes se conservan.

Se retiraron ocho Dockerfile antiguos por módulo, cinco `.dockerignore` locales que quedaron sin uso, el descargador de Maven de la versión anterior y el script aislado de ensayo de Yape. Docker Compose utiliza `infraestructura/docker/Dockerfile.jar` para Java y `frontend/portal-web/Dockerfile` para Angular. Los POM se formatearon para facilitar su lectura y todos los módulos usan Java 21.

Los repositorios y entidades de pedidos y pagos representan bases de datos distintas. Aunque algunos métodos se parezcan, no son copias intercambiables. El filtro HTTP se comparte entre las cinco API; el Gateway conserva su filtro reactivo y ambos reutilizan la correlación. La política JWT se comparte, con un decodificador HTTP o reactivo según corresponda. Los nombres internos de servicio se conservan porque las rutas y el descubrimiento dependen de ellos.

Se eliminaron diez archivos locales de ensayos, accesos anteriores y una copia de credenciales de `credenciales/`. Antes de retirar la copia de producción se comprobó que sus cinco variables coincidían con `runtime.env`, que es la configuración activa. Se conservaron las variables activas, los tres accesos permanentes y los comprobantes de los pagos reales.

## Compilar y verificar

Desde la raíz:

```powershell
# Java completo, incluidas las pruebas de reglas y recursos
mvn verify

# Un módulo junto con su biblioteca compartida
mvn -pl backend/microservicios/pagos -am verify

# Java, Angular y configuración de Docker, sin emitir cargos
.\herramientas\proyecto.ps1 verificar

# Generar los JAR, Angular y las imágenes de Docker
.\herramientas\proyecto.ps1 compilar
```

Las pruebas en `src/test/` comprueban comportamiento real del código: idempotencia de eventos, privacidad de registros, rechazo de importes, bloqueo de cargos inciertos y acceso a recursos pagados. Se conservan como parte del mantenimiento del proyecto. No realizan compras reales.

## Archivos locales y limpieza

`target/`, `dist/`, `.angular/` y `node_modules/` son salidas de compilación y dependencias locales, excluidas de Git. Los JAR de `target/` se usan al construir las imágenes. No se deben confundir con otro frontend o con otro microservicio.

La limpieza quedó completada el 19 de noviembre de 2026. El titular ejecutó el script desde PowerShell y se comprobó que desaparecieron `backend-legacy-casos/`, `infra/`, `services/`, `respaldos/`, las herramientas anteriores de `.github/modernize/`, la caché de Angular y la copia de `output/`. También se retiraron los PNG intermedios; los JPEG optimizados y sus prompts se conservan. El portal respondió HTTP 200 y el pedido 15 mantuvo su estado PAGADA. El script sigue disponible para previsualizar su lista cerrada:

```powershell
.\herramientas\limpiar-proyecto.ps1
.\herramientas\limpiar-proyecto.ps1 -Aplicar
```

El listado es cerrado. Incluye las copias antiguas identificadas, caché de Angular, resultados de ensayos y PNG originales que tienen JPEG optimizados. Conserva `runtime.env`, `public-demo.env`, los usuarios permanentes, los comprobantes y metadatos de las operaciones reales, los volúmenes y los documentos subidos. La primera orden solo previsualiza; la segunda realiza el borrado desde PowerShell local.

La guía PDF tiene una sola ubicación distribuida: `backend/microservicios/pedidos/src/main/resources/guias/guia-bienvenida.pdf`. El generador escribe ahí directamente. Para regenerarla se instala `herramientas/requirements.txt` en un entorno Python; entregar el PDF ya incluido no requiere Python.

## Ubicación de las entregas y solicitudes

| Parte | Archivo o carpeta principal |
| --- | --- |
| Clasificación de entrega (asesoría/PDF) | `backend/compartido/src/main/java/com/illarilegal/compartido/catalogo/EntregaServicio.java` |
| Carga y versiones del catálogo | `backend/microservicios/catalogo-servicios/.../ArchivosCatalogoService.java` |
| Recursos de una compra confirmada | `backend/microservicios/pedidos/.../RecursosPedidoController.java` |
| Asignación de una asesoría pagada | `backend/expedientes/.../SolicitudesService.java` |
| Pantalla de solicitudes del administrador | `frontend/portal-web/src/app/administracion/solicitudes.component.*` |
| Recuperación de intentos Yape inciertos | `backend/microservicios/pagos/.../ConciliacionYapeProgramada.java` |
| Política de contraseñas y preparación de accesos | `herramientas/configurar-registros.mjs` |

No se agregaron otra aplicación, una carpeta de entrega ni un ZIP para este cierre. Las pruebas de regresión se conservan en `src/test/`; los datos y archivos temporales de las comprobaciones se retiran.

Último cierre: la revisión automática de ejecución bloqueó retirar la caché local de Angular y una captura de revisión. Ambos elementos quedaron identificados en la lista cerrada de `limpiar-proyecto.ps1`; no son código ni datos del negocio. Para retirarlos, el titular puede ejecutar el script con `-Aplicar` desde su propia terminal. No se intentó sortear el bloqueo con otro método de borrado.
