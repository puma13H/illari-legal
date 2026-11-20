# Verificación del portal

Revisión realizada el 19 de noviembre de 2026 sobre Angular, las API y los contenedores locales publicados por el túnel HTTPS.

## Recorrido entre roles

| Comprobación | Resultado |
| --- | --- |
| Administrador registra un cliente y un abogado con acceso a Keycloak | Correcto |
| Nuevas cuentas cambian la contraseña inicial e ingresan con su rol | Correcto |
| Administrador crea y edita un expediente y asigna sus responsables | Correcto |
| Abogado adjunta un documento y actualiza el seguimiento | Correcto |
| Cliente consulta su expediente y descarga el documento real | Correcto |
| Edición de datos se refleja en el perfil local y en el nombre de acceso | Correcto |
| Reasignación retira el acceso del abogado anterior y lo otorga al nuevo | Correcto |
| Otro cliente no puede consultar ni descargar los documentos del caso | Rechazado con 403 |
| Cliente intenta modificar un expediente, incluso con cuerpo inválido | Rechazado con 403 |

Se usaron cuentas y un expediente temporales, retirados al cerrar la revisión. Los tres accesos permanentes del proyecto se conservan.

## Interfaz

Se revisaron portada, formularios de registro, creación y edición de expedientes, listado y detalle del cliente y pago de la guía. Las comprobaciones incluyeron anchuras de 320, 390, 768 y 1440 px, descarga de documentos y ausencia de errores por estilos bloqueados por la política de seguridad. Las capturas locales de revisión están en `infraestructura/puerta-enlace/target/revision-general/` y no forman parte del código distribuido.

## Guía de bienvenida digital: revisión inicial de S/ 1

- Catálogo: artículo activo por S/ 1.00, con descripción y PDF de dos páginas revisado visualmente.
- Pedido: total calculado por el servidor, medio YAPE y estado PENDIENTE_PAGO.
- Idempotencia: repetir la creación con la misma clave devuelve el mismo pedido.
- Configuración de pago consultada: ambiente live e importe de 100 céntimos.
- Descarga antes de pagar: rechazada con 409.
- Pruebas del controlador de recursos: cuatro casos aprobados, incluidos otro titular, pedido pendiente, artículo diferente y entrega del PDF al titular de un pedido pagado.

El pedido de S/ 1 quedó preparado para Mateo Flores; su identificador y enlace vigente se guardan en `credenciales/compra-guia.json`. No se ejecutó un cargo real durante la automatización. Falta que el titular confirme desde su Yape y comprobar después el resultado real y la descarga. No se alteraron estados de producción para aparentar un pago.

Actualización del 19 de noviembre: el primer intento del titular fue rechazado por Mercado Pago con `invalid_transaction_amount`. No se encontró una orden ni un pago para su referencia y el titular informó que no hubo descuento. El flujo exitoso con dinero real sigue pendiente; el diagnóstico y la corrección del mensaje están en [Incidencia de Yape](INCIDENCIA-YAPE.md).

## Nuevo pedido autorizado por S/ 1.24

El titular autorizó el nuevo total de S/ 1.24. Se actualizó el artículo 15 desde la API de administración y se preparó el pedido 15 para Mateo Flores, con pago 15, método YAPE y estado PENDIENTE_PAGO. El servidor devuelve 124 céntimos y ambiente live para ese pedido. El pedido 14 conserva S/ 1.00 y su rechazo registrado.

Se comprobó que la selección de la guía permite continuar, que el formulario muestra S/ 1.24 con el código vacío y que la descarga sigue bloqueada con 409 antes de pagar. La pantalla se revisó a 390 y 1440 px sin desbordamiento. La preparación no envió solicitudes de cargo ni códigos de aprobación. El enlace y la clave idempotente se guardaron en el archivo local ignorado `credenciales/compra-guia-124.json`.

## Pago real y entrega confirmados

El 19 de noviembre de 2026 el titular confirmó el pedido 15 desde Yape e informó el descuento de S/ 1.24. A las 10:00 de Lima se verificó mediante una consulta de solo lectura que Mercado Pago tenía esa operación en `processed` / `accredited`, con importe 1.24 y medio yape. El pago local 15 quedó VALIDADO y el pedido 15 PAGADA.

En una sesión del cliente Mateo Flores se abrió el mismo pedido, se comprobó la pantalla de pago acreditado y se descargó la guía mediante su botón. El archivo tiene cabecera PDF y 184615 bytes. La guía se mostró al recargar el pedido después de la confirmación. No se ingresaron códigos ni se hicieron nuevas solicitudes de cargo durante esta revisión. El comprobante técnico resumido está en `credenciales/confirmacion-yape-15.json`, excluido de Git.

## Compilación y persistencia

Angular y los módulos Java modificados compilaron correctamente. La migración `06-registros-y-presentacion.sql` se aplicó conservando expedientes y documentos. El servicio de registro de accesos se configuró y se verificó su preparación idempotente. Iniciar y detener el proyecto conserva los volúmenes de PostgreSQL y documentos.

## Revisión después de ordenar el código

El 19 de noviembre de 2026 se añadió el POM central y la biblioteca compartida de eventos, registros, trazabilidad y validación JWT. `mvn verify` completó 20 pruebas sin fallos; Angular compiló y Compose validó su configuración. Se inspeccionaron los seis JAR de las API y el Gateway: incluyen la biblioteca compartida y no conservan las clases antiguas de correlación, registros, eventos o filtro HTTP en sus propios paquetes.

Se reconstruyeron y actualizaron los seis contenedores afectados, todos con estado healthy. La revisión del navegador confirmó el ingreso de los tres usuarios permanentes, acceso a sus consultas y cabecera de trazabilidad. El cliente recibió 403 al intentar consultar el directorio de administración. No hubo errores de JavaScript ni desbordamiento a 390 px. El pedido 15 sigue PAGADA por S/ 1.24, el pago sigue VALIDADO y la guía se puede descargar. Las consultas de revisión no escribieron datos de negocio ni hicieron nuevos cobros.

Se retiraron archivos fuente obsoletos y diez archivos locales de ensayos o configuración duplicada. El borrado recursivo inicialmente rechazado por la revisión automática se completó después por el titular con `herramientas/limpiar-proyecto.ps1 -Aplicar`. Se confirmó la ausencia de las carpetas antiguas, respaldos retirados y salidas revisadas. El portal respondió HTTP 200, los contenedores siguieron funcionando y se conservaron el pedido pagado, el PDF, los usuarios y el comprobante real. Véase [Organización del código](ORGANIZACION-DEL-CODIGO.md).

## Cierre de entregas y atención — 19 de noviembre de 2026

- 37 pruebas Java: utilidades y seguridad compartidas, recursos pagados, versiones PDF, solicitudes, persistencia de asignación y conciliación de pagos. La prueba de asignación persiste en H2, verifica idempotencia y comprueba acceso del comprador y abogado y rechazo de otro cliente; no utiliza compras reales para crear expedientes de ensayo.
- Migraciones 08–11 ejecutadas dos veces sobre esquemas PostgreSQL aislados y retirados al terminar. Después se aplicaron a la instalación conservando sus datos.
- Política comprobada en Keycloak: una contraseña de seis caracteres fue rechazada y una larga aceptada. La cuenta usada para comprobarlo fue eliminada.
- Carga de PDF superior a 1 MB comprobada a través de HTTPS. Se verificaron el bloqueo de publicación sin archivo, rechazo de contenido no PDF, descarga de administración y dos versiones distintas. El servicio temporal y sus dos archivos fueron retirados sin crear pedidos ni cargos.

El pago real que acredita la integración continúa siendo únicamente la operación de S/ 1.24 confirmada por el titular. Las comprobaciones posteriores no deben realizar otros cobros ni convertir pedidos pendientes en pagados.

Verificación final sobre HTTPS: los tres roles iniciaron sesión, las solicitudes respondieron 200 para ADMIN y 403 para los otros perfiles, y las pantallas revisadas no desbordaron a 320, 390, 768 y 1440 px. El pedido 15 conservó PAGADA, su pago VALIDADO y la descarga PDF. Una respuesta de pedido retrasada se simuló únicamente en el navegador para comprobar que la guía aparece sin recargar cuando llega PAGADA; no se modificó la base ni se emitieron cargos. El comando `proyecto.ps1 verificar` terminó correctamente con Java, Angular y Docker.

## Instalación desde una copia limpia

El 19 de noviembre de 2026 se extrajo una copia de los archivos fuente de la versión de trabajo en otra carpeta de Windows. Se partió de volúmenes Docker vacíos y se instalaron las dependencias desde el código y sus archivos de versiones. Se usaron contraseñas generadas para esa copia y los puertos locales de revisión.

| Hallazgo | Corrección | Prueba de corrección |
| --- | --- | --- |
| En la primera copia, `iniciar` falló por ausencia de `credenciales/runtime.env`. | La acción `preparar` genera el archivo desde `.env.example`. | La copia final ejecutó `preparar -Aislado` y compiló desde el código fuente. |
| La base nueva detenía la migración 11 porque faltaba `yape_intento`. | Los esquemas iniciales ahora incluyen los campos de pagos y transferencias que espera la aplicación. | PostgreSQL completó la inicialización y la migración. Pagos y Pedidos pasaron sus comprobaciones de salud. |
| El realm importado y la base nueva carecían de la primera cuenta administrativa del portal. | El inicio vincula la cuenta de administrador inicial con la base jurídica al preparar una instalación nueva. | El administrador inició sesión y registró cuentas con permisos de cliente y abogado. |

Se siguió el [manual de primera instalación](OPERACION.md#primera-instalación) hasta el arranque. El portal y el catálogo por Gateway respondieron 200. El catálogo devolvió 15 servicios, incluida la guía digital con ID 15. Se creó un expediente ficticio en la instalación aislada y se verificó que la abogada asignada y el titular podían verlo tras cambiar su contraseña inicial. Una cuenta de otro cliente recibió 403 al consultar ese expediente. `proyecto.ps1 verificar` completó 37 pruebas Java, la compilación Angular y la validación de Compose. El entorno aislado usó Yape desactivado.

`proyecto.ps1 detener` detuvo la instalación aislada y se comprobó la persistencia de su volumen antes de retirar los recursos temporales. La instalación de la exposición seguía operativa y el pedido 15 conservó su estado `PAGADA` y total de S/ 1.24.

Esta prueba corresponde a los archivos locales que se preparan para entregar. La comprobación desde el enlace público del repositorio se repetirá cuando esa versión esté publicada.
