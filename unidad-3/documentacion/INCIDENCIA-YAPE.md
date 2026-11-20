# Rechazo del primer intento real con Yape

El 19 de noviembre de 2026 se intentó pagar la Guía de bienvenida digital por S/ 1.00. El proveedor devolvió HTTP 400 con el código `invalid_transaction_amount`. El cuerpo de la solicitud tenía tanto el total como el importe de la transacción en `"1.00"` y se usaron las credenciales del ambiente live.

La búsqueda por la referencia exacta del intento no devolvió una orden ni un pago en Mercado Pago. El titular indicó que no aparecía un descuento en Yape. La evidencia técnica, sin número de celular, código de aprobación ni token, está en el archivo local ignorado `credenciales/revision-yape-14.json`.

## Qué se corrigió

- Un rechazo explícito de importe ya se distingue de un fallo de conexión o de una respuesta desconocida.
- Se conservan el código del rechazo, su referencia de soporte y su fecha en la base de pagos. No se registran OTP, tokens de pago ni cuerpos completos de respuestas en los logs.
- El portal informa que Mercado Pago no admitió el importe y evita solicitar otro código para ese pedido. La API también bloquea repetir el cargo del pedido rechazado.
- Un timeout o una respuesta incompleta mantiene la reserva del intento y exige conciliación; no se declara automáticamente como rechazo.
- No se marcó el pedido como pagado ni se habilitó la descarga del PDF.

La migración compatible está en `base-de-datos/07-diagnostico-pagos-yape.sql` y se aplica también con el comando de inicio.

## Límites de la comprobación

El código `invalid_transaction_amount` demuestra el rechazo de ese importe en ese intento. Por sí solo no demuestra un mínimo distinto ni permite asegurar que subir el precio resolverá el problema. La API de medios de pago de la cuenta anunció un mínimo de 1 para Yape; se conserva esa discrepancia para el diagnóstico, sin inventar otra cifra.

La [documentación de Yape con Orders API](https://www.mercadopago.com.pe/developers/es/docs/checkout-api-orders/payment-integration/websites/yape?scope=prod) indica el formato del importe y el flujo del pago. Las respuestas se pueden revisar en Logs de la aplicación de Mercado Pago, dentro del ambiente correspondiente.

Se añadieron ocho pruebas locales para comprobar la clasificación del rechazo, la conservación del bloqueo, la protección ante respuestas desconocidas, el mínimo antes de enviar una solicitud y la ausencia de confirmaciones de pago falsas. Esas pruebas usan un servidor simulado y no cobran dinero.

## Comprobación aislada sin dinero real

Durante el ensayo se validó la etiqueta `test_user` y el sitio MPE de la cuenta de prueba. Su archivo local de credenciales se retiró al limpiar los ensayos. Se usaron exclusivamente esa cuenta y el celular/OTP ficticios oficiales de Mercado Pago; la configuración live del portal se conservó.

| Importes de laboratorio | Respuesta observada |
| --- | --- |
| 1.00, 1.01, 1.10, 1.20 y 1.23 | HTTP 400, `invalid_transaction_amount` |
| 1.24, 1.25, 1.50 y 2.00 | HTTP 201, `processed` / `accredited` en prueba |
| 1.00 con una cuota explícita | El mismo rechazo de importe |

El resultado del ensayo quedó resumido en la tabla anterior; el archivo temporal de diagnóstico se retiró durante la limpieza. Después de confirmar la operación real, se retiró el script aislado `diagnosticar-yape-sandbox.mjs`; no forma parte del funcionamiento del portal. Sus archivos locales de ensayo también se retiraron durante la limpieza. Las pruebas de regresión de pagos se conservan. Utiliza el [procedimiento oficial de laboratorio de Yape](https://www.mercadopago.com.pe/developers/es/docs/checkout-api-orders/integration-test/yape).

Se configuraron 124 céntimos como umbral de este portal mediante `MERCADOPAGO_YAPE_MIN_CENTIMOS`. Angular avisa y bloquea continuar por debajo del umbral; el servidor lo comprueba antes de reservar un intento o contactar al proveedor. Esta configuración se basa en el límite observado en la cuenta de prueba: no se presenta como mínimo universal de Mercado Pago ni como garantía de aprobación en producción.

Inicialmente se conservó el precio de S/ 1.00 de la guía y no se crearon cobros reales para superar el presupuesto autorizado.

El 19 de noviembre el titular autorizó cambiar la guía y preparar un nuevo pedido por S/ 1.24. El pedido anterior de S/ 1.00 mantiene su total, su rechazo y el bloqueo de reintento. La autorización nueva corresponde exclusivamente a S/ 1.24, con confirmación del titular desde el formulario de Yape. La preparación no envió un cargo ni utilizó códigos de aprobación.

## Confirmación real por S/ 1.24

El 19 de noviembre de 2026, a las 09:58 de Lima, el titular confirmó el pago del pedido 15 e indicó que Yape descontó S/ 1.24. La lectura posterior de Mercado Pago devolvió `processed` / `accredited`, importe `1.24` y medio `yape` en el ambiente de producción. La base de pagos registra VALIDADO y la de pedidos PAGADA.

Desde la cuenta del titular se verificaron la pantalla de pago acreditado, el botón Descargar guía y la descarga de un PDF de 184615 bytes. En la captura inicial el recurso todavía no se mostraba; después de recargar el pedido ya estaba disponible. La comprobación posterior solo realizó lecturas y la descarga: no envió solicitudes de cargo ni produjo cobros adicionales. La evidencia sin datos de contacto ni tokens está en el archivo local ignorado `credenciales/confirmacion-yape-15.json`.

La operación real por S/ 1.24 quedó comprobada. Este resultado no demuestra que 1.24 sea el mínimo universal de Mercado Pago ni garantiza la aprobación de otros pagos. No se autorizaron nuevas compras ni reintentos después de esta confirmación.
