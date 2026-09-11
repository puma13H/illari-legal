# Scripts de ejecución

Los archivos de esta carpeta automatizan la demostración de Unidad I.

- `windows/`: lanzadores `.bat` para Windows.
- `*.ps1`: implementación PowerShell usada por los lanzadores.

Para la secuencia normal usa los archivos numerados de `windows/` empezando por `01-verificar-compilacion.bat`.

## Observabilidad opcional

- `windows/11-iniciar-prometheus.bat`
- `windows/12-iniciar-grafana.bat`
- `windows/13-detener-observabilidad.bat`

Prometheus y Grafana se ejecutan en Docker y no abren consolas externas.
