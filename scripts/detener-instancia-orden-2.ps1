$connection = Get-NetTCPConnection -LocalPort 8085 -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
if (-not $connection) {
    Write-Host "No hay una instancia escuchando en 8085." -ForegroundColor Yellow
    exit 0
}
Stop-Process -Id $connection.OwningProcess -Force
Write-Host "Instancia orden-legal-ms:8085 detenida. Espera unos segundos y vuelve a ejecutar scripts\windows\05-probar-balanceo.bat." -ForegroundColor Green
