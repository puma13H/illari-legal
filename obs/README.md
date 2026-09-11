# Observabilidad — Prometheus + Grafana

Este módulo es un **anexo opcional de S4** y no modifica el funcionamiento de los microservicios. Los servicios Java continúan ejecutándose manualmente desde VS Code; Prometheus y Grafana se ejecutan en Docker.

## Flujo

```text
Microservicios / Gateway
        │ /actuator/prometheus
        ▼
    Prometheus
        │
        ▼
      Grafana
```

Prometheus consulta las métricas de los procesos que corren en Windows mediante `host.docker.internal`.

## Puertos

- Prometheus: `http://localhost:19090`
- Grafana: `http://localhost:13000`

Ambos puertos están enlazados solo a `127.0.0.1`, por lo que son accesibles únicamente desde la computadora local.

## Inicio manual, uno por uno

Desde la raíz del proyecto:

```powershell
docker compose -f .\obs\compose.yml up -d prometheus
```

Comprueba los targets en `http://localhost:19090/targets`. Deben aparecer `UP` cuando los microservicios y el Gateway estén ejecutándose.

Luego:

```powershell
docker compose -f .\obs\compose.yml up -d grafana
```

Abre `http://localhost:13000`. No requiere inicio de sesión en DEV; el dashboard **Illari Legal — Unidad I** queda provisionado automáticamente en modo lectura.

## Qué muestra el dashboard

- Estado UP/DOWN de Gateway y microservicios.
- Las dos instancias de `orden-legal-ms` (`8083` y `8085`) por separado.
- Tasa de peticiones HTTP.
- Uso de memoria JVM.
- Uso de CPU por proceso.
- Tiempo activo de cada proceso.

Para detener solo observabilidad:

```powershell
docker compose -f .\obs\compose.yml down
```

Los microservicios y PostgreSQL no se detienen con ese comando.
