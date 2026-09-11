$ports = 18888,18761,18080,8081,8082,8083,8084,8085
foreach ($port in $ports) {
    $connections = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue
    foreach ($connection in $connections) {
        if ($connection.OwningProcess -gt 0) {
            Write-Host "Deteniendo PID $($connection.OwningProcess) del puerto $port"
            Stop-Process -Id $connection.OwningProcess -Force -ErrorAction SilentlyContinue
        }
    }
}
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root
docker compose -f compose-dev.yml down
Write-Host "Entorno DEV detenido." -ForegroundColor Green
