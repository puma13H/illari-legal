# Operación del proyecto

## Requisitos

- Docker Desktop con el motor Linux iniciado.
- Java 21 y Maven para compilar los módulos del servidor.
- Node.js 24 y npm para compilar Angular.
- PowerShell y acceso a Internet para descargar las dependencias y las imágenes.

Las credenciales, respaldos, cargas de documentos y archivos generados están excluidos de Git. Los volúmenes de Docker guardan las bases de datos y los documentos entre reinicios.

## Primera instalación

Abre PowerShell en la raíz de una copia del repositorio. Comprueba que Docker, Java, Maven y Node estén disponibles con `docker compose version`, `java -version`, `mvn -v` y `node -v`. En una sesión que bloquee los scripts locales, usa `Set-ExecutionPolicy -Scope Process -ExecutionPolicy RemoteSigned`.

```powershell
.\herramientas\proyecto.ps1 preparar
.\herramientas\proyecto.ps1 compilar
.\herramientas\proyecto.ps1 iniciar
.\herramientas\proyecto.ps1 estado
```

`preparar` genera `credenciales/runtime.env` y las contraseñas de la nueva instalación a partir de `.env.example`. `compilar` descarga las dependencias, genera los JAR y las imágenes, y compila Angular. `iniciar` prepara las bases de datos, configura Keycloak, crea el primer administrador del portal y espera a que los servicios estén listos. El pago Yape queda desactivado en la configuración local inicial.

Si vas a verificar el manual en la misma computadora que ya ejecuta Illari Legal, descarga o extrae el código en otra carpeta y usa **`preparar -Aislado`** en el primer paso. Esa copia usa sus propios contenedores, bases de datos y volúmenes. Los puertos de esa prueba son **14200** para el portal, **28080** para el Gateway y **28180** para Keycloak. Mantén una sola copia de revisión activa a la vez.

### Comprobar el arranque

| Qué comprobar | Cómo | Resultado esperado |
| --- | --- | --- |
| Contenedores | Ejecuta `proyecto.ps1 estado`. | PostgreSQL, Keycloak, Kafka, Config Server, Eureka, las API, el Gateway y el portal se muestran en ejecución. Los dos trabajos de preparación terminan con código 0. Abre el [tablero de Grafana](http://127.0.0.1:3000/d/illari-s12-overview/illari-legal-observabilidad) para comprobar métricas, disponibilidad y logs. |
| Portal y sesión | Abre [el portal local](http://localhost:4200) o [la copia aislada](http://localhost:14200). Ingresa con `admin.illari` y el valor local de `ILLARI_ADMIN_INICIAL_PASSWORD` en `credenciales/runtime.env`. | Keycloak abre la sesión y el portal muestra el menú de administración. |
| Catálogo por el Gateway | Ejecuta `Invoke-RestMethod http://localhost:18080/api/v1/servicios` o usa el puerto 28080 en la copia aislada. | Devuelve los servicios del catálogo inicial. El mismo catálogo aparece en **Servicios** del administrador. |
| Puesta en marcha del negocio | En **Equipo**, registra un cliente y un abogado. Usa **Expedientes** para asignarles un caso. | El administrador puede guardar el expediente y los usuarios creados lo consultan al completar el cambio de su contraseña inicial. |
| Detención | Ejecuta `proyecto.ps1 detener` desde la misma carpeta. | Se detienen los servicios de esa instalación y sus datos quedan disponibles para el siguiente arranque. |

### Problemas conocidos

| Síntoma | Causa probable y acción |
| --- | --- |
| Falta `credenciales/runtime.env` | Ejecuta `preparar` en la raíz de la copia. |
| No se encuentra un JAR o una imagen | Ejecuta `compilar` antes de iniciar por primera vez. |
| Docker informa que un puerto o nombre está ocupado | Verifica qué instalación está activa. Para una prueba simultánea, prepara una copia nueva con `-Aislado`. |
| Falla la inicialización de la base en Windows | Revisa la salida de PostgreSQL. El script `00-inicializar-bases.sh` debe conservar finales de línea LF, tal como indica `.gitattributes`. |
| El catálogo carga pero no aparece el botón de pago | La configuración local inicial desactiva Yape. Para validar un pago de laboratorio, configura las credenciales de un entorno de pruebas independiente. |

## Comandos

Ejecutar desde PowerShell, dentro del proyecto:

```powershell
# Iniciar los servicios y sincronizar la URL pública configurada
.\herramientas\proyecto.ps1 iniciar

# Consultar contenedores, incluidos los trabajos de inicialización
.\herramientas\proyecto.ps1 estado

# Detener los servicios conservando los datos
.\herramientas\proyecto.ps1 detener

# Reconstruir y actualizar solamente Angular/Nginx
.\herramientas\proyecto.ps1 actualizar-web

# Iniciar Grafana, Prometheus, Loki, Alloy y Blackbox Exporter
.\herramientas\proyecto.ps1 observabilidad

# Compilar los módulos Java, Angular y las imágenes Docker
.\herramientas\proyecto.ps1 compilar

# Verificar Java con sus pruebas, Angular y configuración de Docker
.\herramientas\proyecto.ps1 verificar

# Sincronizar la URL actual del túnel y aplicarla a los servicios
.\herramientas\proyecto.ps1 url
```

Los dos contenedores cuyo nombre empieza por `illari-preparar-` son trabajos de inicialización: **finalizar con código 0 es correcto**.

Los puertos de monitoreo, el acceso a Grafana y la consulta de una traza están en la [guía de observabilidad](OBSERVABILIDAD.md).

El POM de la raíz compila los nueve módulos Java y resuelve la biblioteca `backend/compartido/` antes de los servicios. Para verificar un módulo aislado se ejecuta, desde la raíz, `mvn -pl backend/microservicios/pagos -am verify`. La lista de carpetas activas y la limpieza local están en [Organización del código](ORGANIZACION-DEL-CODIGO.md).

## Desarrollo de Angular

```powershell
cd frontend/portal-web
npm ci
npm run build
```

El resultado está en `dist/portal/browser/`. Para verlo en el contenedor, volver a la raíz y ejecutar `actualizar-web`.

Para desarrollo con recarga automática, usar `npm start -- --port 4300`. La configuración local de Keycloak permite el portal desplegado en `http://localhost:4200`; para iniciar sesión desde otro puerto deben añadirse ese origen y su callback en la configuración del cliente de desarrollo. El proxy de desarrollo está en `proxy.conf.json`.

## URL pública y HTTPS

`infraestructura/acceso-publico/` contiene el proxy Nginx y el túnel rápido de Cloudflare. Cloudflare entrega una URL HTTPS que reenvía las peticiones al proyecto local. Los contenedores y Docker deben permanecer encendidos para usarla.

Cuando se reinicia el túnel, puede cambiar su URL. `actualizar-url.mjs` lee el enlace del contenedor, actualiza los callbacks del cliente de Keycloak y escribe `credenciales/public-demo.env`. El comando `proyecto.ps1 url` aplica ese valor al portal, la entrada pública y las API. No modifica las contraseñas de los usuarios.

El comando `iniciar` prepara PostgreSQL y Keycloak, aplica la migración compatible de expedientes, configura el servicio de registro de personas y hace la sincronización de URL al final. `actualizar-web` conserva el túnel encendido y no lo recrea.

## Registro de personas y accesos

Desde Equipo, el administrador puede registrar clientes y abogados y editar sus datos de contacto. La cuenta nueva exige cambiar la contraseña inicial en su primer ingreso. La contraseña no vuelve en las respuestas de la API ni se guarda en el navegador.

`herramientas/configurar-registros.mjs` crea de forma idempotente el cliente confidencial `illari-registros` y guarda su secreto en `credenciales/runtime.env`. Su cuenta de servicio tiene permisos de usuarios del realm `illari-legal`; la contraseña de administración de Keycloak solo se usa en la preparación local, no en la API de registros. Si esa preparación falla, el comando de inicio muestra el error y no simula un registro exitoso.

La configuración utiliza la [cuenta de servicio y gestión de usuarios de Keycloak](https://www.keycloak.org/docs/26.2.5/server_admin/index.html) y su [API de administración](https://www.keycloak.org/docs-api/latest/rest-api/).

## Guía de bienvenida digital y Yape

El artículo cuesta S/ 1.24 e incluye un PDF de dos páginas. Su generador está en `herramientas/generar-guia-portal.py` y el archivo distribuido está en `backend/microservicios/pedidos/src/main/resources/guias/guia-bienvenida.pdf`. El nombre anterior del artículo se migra conservando su identificador. `ILLARI_GUIA_SERVICIO_ID` debe coincidir con ese identificador del catálogo (15 en esta instalación). La actualización de precio fue autorizada por el titular; los pedidos existentes mantienen el total con el que se crearon.

La API de pedidos verifica titularidad, estado PAGADA y presencia de ese artículo antes de entregar el PDF. Un pedido pendiente devuelve 409 al intentar descargarlo; otro usuario no recibe el recurso. El pago y la entrega no dependen de una marca visual del frontend.

`MERCADOPAGO_YAPE_MIN_CENTIMOS=124` configura el umbral del portal para continuar a Yape. Docker entrega el mismo valor al servidor y a la configuración pública de Angular. La validación no modifica los precios ni los importes de pedidos existentes. Su origen y los límites de la comprobación están en `INCIDENCIA-YAPE.md`.

Para el cobro real, el titular revisa el total y escribe su número y código de aprobación en el formulario de Yape. Crear el pedido no cobra dinero. No cambiar manualmente un pedido a PAGADA para aparentar una confirmación y no compartir códigos de aprobación en chats. El enlace de un pedido pendiente permite retomarlo con su misma cuenta.

Para regenerar la guía PDF se instala `herramientas/requirements.txt` en un entorno Python y se ejecuta `python herramientas/generar-guia-portal.py`. El generador escribe directamente en los recursos de pedidos; no requiere carpetas temporales de revisión.

## Archivos de configuración

| Archivo | Responsabilidad |
| --- | --- |
| `docker-compose.yml` | Servicios principales, puertos, volúmenes y nombres de contenedores. |
| `infraestructura/observabilidad/compose.observabilidad.yml` | Logs, métricas y disponibilidad. |
| `infraestructura/acceso-publico/compose.yml` | Túnel HTTPS y variables del entorno público. |
| `credenciales/runtime.env` | Secretos y configuración local del servidor. |
| `credenciales/public-demo.env` | URL y host actuales del túnel. |
| `infraestructura/configuracion-central/config-repo/` | Configuración compartida de Spring. |

Si falla la entrada de un rol, revisar primero `estado`, la URL que muestra el comando `url` y el registro del contenedor correspondiente. Los expedientes marcados Referencia contienen información ilustrativa.

## Entregas, asignación y recuperación de pagos

El inicio aplica también las migraciones `08-entrega-catalogo.sql`, `09-entrega-pedidos.sql`, `10-expedientes-de-pedidos.sql` y `11-conciliacion-yape.sql`. Conservan los identificadores, compras, pagos y documentos existentes. Se pueden aplicar de nuevo sin duplicar columnas ni índices.

El volumen `catalogo_archivos` guarda versiones PDF con UUID: catálogo escribe y pedidos solo lee. No borrar ese volumen al reiniciar. La guía incorporada mantiene su archivo distribuido en el JAR de pedidos. Los productos digitales sin PDF quedan inactivos hasta completar su contenido.

`GET /api/solicitudes` y `POST /api/solicitudes/{pedido}/{posicion}/asignacion` están reservados a ADMIN. La asignación verifica el pedido pagado en la API de pedidos, resuelve la identidad del comprador y guarda la relación con el expediente. El índice único de pedido/detalle y el bloqueo del cliente impiden duplicar una atención. Las compras históricas sin perfil vinculado se muestran como pendientes de revisión y no se asignan automáticamente.

La política de Keycloak exige un mínimo de 12 caracteres tanto en el alta como en los cambios de contraseña. La actualización solicita un cambio al siguiente inicio de sesión de las cuentas registradas anteriormente; conserva los tres accesos permanentes documentados. No se cambian contraseñas ni se muestran en las respuestas.

Después de una respuesta de pago incierta, una tarea del servidor busca el intento existente cada 30 segundos y consulta su orden cuando la encuentra. Verifica referencia, ambiente, moneda, importe y medio antes de confirmar. Las consultas no generan nuevos cargos. Una búsqueda vacía no se interpreta como rechazo ni libera el intento para cobrar de nuevo. Se respeta `Retry-After` cuando el proveedor limita consultas.

Angular actualiza el estado pendiente durante un periodo acotado y espera la confirmación del pedido antes de consultar sus recursos. Si tarda más, conserva el botón de consulta manual. Los rechazos explícitos de S/ 1 anteriores siguen bloqueados; no se reintentan con otro importe.
