# Demostración S5 — orden recomendado

La demostración está preparada para realizarse en pocos minutos y cubrir los criterios acumulativos.

## 1. Configuración centralizada

Mostrar:

- `http://localhost:18888/orden-legal-ms/dev`
- `http://localhost:18888/orden-legal-ms/prod`

Explicar que ambos perfiles están en `config-repo` y que usuario/contraseña de BD provienen del entorno.

## 2. Eureka

Abrir `http://localhost:18761`.

Se debe observar:

- `CLIENTE-MS`
- `SERVICIO-LEGAL-MS`
- `ORDEN-LEGAL-MS` con **dos instancias**
- `PAGO-MS`
- `LEGAL-GATEWAY`

## 3. CRUD por Gateway

Ejecutar `scripts/windows/04-demo-s5.bat`.

El script crea una orden, la consulta, la actualiza y finalmente la elimina. También genera una solicitud inválida para demostrar el manejo de errores.

La URL usada es siempre:

`http://localhost:18080/api/v1/ordenes/...`

Así se demuestra el punto único de acceso.

## 4. Balanceo

El mismo script realiza varias llamadas consecutivas e imprime:

- `X-Instance-ID`
- `X-Trace-ID`

En DEV deben aparecer:

- `orden-legal-ms:8083`
- `orden-legal-ms:8085`

También puede ejecutarse únicamente `scripts/windows/05-probar-balanceo.bat`.

## 5. Tolerancia a caída

Ejecutar:

`scripts/windows/06-detener-instancia-orden-2.bat`

Esperar a que Eureka retire la instancia caída y ejecutar otra vez:

`scripts/windows/05-probar-balanceo.bat`

Las peticiones deben seguir llegando a la instancia 8083.

En Docker PROD, el equivalente es:

```powershell
docker stop orden-legal-ms-2
```

y luego:

`scripts/windows/09-probar-balanceo-prod.bat`

## 6. Persistencia y Flyway

Mostrar, por ejemplo:

`services/orden-legal-ms/src/main/resources/db/migration/`

y explicar:

- `V1__...` crea el esquema.
- `V2__...` carga datos mínimos.
- Hibernate usa `ddl-auto: validate`, por lo que no crea tablas.

## 7. Documentación

Mostrar `docs/u1-producto.md` o el sitio MkDocs publicado.

## Capturas recomendadas

Guardar como evidencias:

1. Config Server DEV.
2. Config Server PROD.
3. Eureka con dos instancias de orden.
4. Swagger con endpoints CRUD.
5. POST/PUT/DELETE exitosos.
6. Error HTTP 400 o 404.
7. Consola con alternancia de `X-Instance-ID`.
8. Balanceo funcionando después de detener una instancia.
9. Estructura Flyway.
10. Sitio MkDocs.
