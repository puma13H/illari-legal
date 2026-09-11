$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

if (-not (Test-Path ".env")) {
    & "$PSScriptRoot\configurar-entorno.ps1"
}

Get-Content ".env" | ForEach-Object {
    if ($_ -and -not $_.StartsWith("#")) {
        $parts = $_ -split "=", 2
        if ($parts.Count -eq 2) {
            [Environment]::SetEnvironmentVariable($parts[0], $parts[1], "Process")
        }
    }
}

function Wait-Url([string]$url, [int]$seconds = 300) {
    $deadline = (Get-Date).AddSeconds($seconds)
    do {
        try {
            Invoke-WebRequest -UseBasicParsing -Uri $url -TimeoutSec 3 | Out-Null
            return
        } catch {
            Start-Sleep -Seconds 2
        }
    } while ((Get-Date) -lt $deadline)
    throw "No respondió a tiempo: $url"
}

function Start-JavaApp([string]$title, [string]$relativeDir, [hashtable]$extraEnv = @{}) {
    $dir = Join-Path $root $relativeDir
    $exports = @()
    $exports += "`$Host.UI.RawUI.WindowTitle='$title'"
    $exports += "Set-Location '$($dir.Replace("'","''"))'"
    foreach ($key in @("DB_USERNAME","DB_PASSWORD")) {
        $value = [Environment]::GetEnvironmentVariable($key, "Process")
        if ($value) { $exports += "`$env:$key='$($value.Replace("'","''"))'" }
    }
    foreach ($key in $extraEnv.Keys) {
        $value = [string]$extraEnv[$key]
        $exports += "`$env:$key='$($value.Replace("'","''"))'"
    }
    $exports += "& '.\mvnw.cmd' spring-boot:run"
    $command = $exports -join "; "
    Start-Process powershell -ArgumentList "-NoExit","-ExecutionPolicy","Bypass","-Command",$command
}

Write-Host "1/5 Levantando PostgreSQL DEV..." -ForegroundColor Cyan
docker compose -f compose-dev.yml up -d
if ($LASTEXITCODE -ne 0) { throw "No se pudo iniciar PostgreSQL con Docker Compose." }

Write-Host "2/5 Iniciando Config Server (18888)..." -ForegroundColor Cyan
Start-JavaApp "Illari - Config Server :18888" "infra\legal-config"
Wait-Url "http://localhost:18888/actuator/health"

Write-Host "3/5 Iniciando Eureka (18761)..." -ForegroundColor Cyan
Start-JavaApp "Illari - Eureka :18761" "infra\legal-eureka"
Wait-Url "http://localhost:18761/actuator/health"

Write-Host "4/5 Iniciando microservicios..." -ForegroundColor Cyan
Start-JavaApp "Illari - Cliente :8081" "services\cliente-ms"
Start-JavaApp "Illari - Servicio Legal :8082" "services\servicio-legal-ms"
Start-JavaApp "Illari - Orden instancia 1 :8083" "services\orden-legal-ms" @{
    "SERVER_PORT" = "8083"; "EUREKA_INSTANCE_ID" = "orden-legal-ms:8083"
}
Start-JavaApp "Illari - Orden instancia 2 :8085" "services\orden-legal-ms" @{
    "SERVER_PORT" = "8085"; "EUREKA_INSTANCE_ID" = "orden-legal-ms:8085"
}
Start-JavaApp "Illari - Pago :8084" "services\pago-ms"

Wait-Url "http://localhost:8081/actuator/health"
Wait-Url "http://localhost:8082/actuator/health"
Wait-Url "http://localhost:8083/actuator/health"
Wait-Url "http://localhost:8085/actuator/health"
Wait-Url "http://localhost:8084/actuator/health"

Write-Host "5/5 Iniciando Gateway (18080)..." -ForegroundColor Cyan
Start-JavaApp "Illari - Gateway :18080" "infra\legal-gateway"
Wait-Url "http://localhost:18080/actuator/health"

Write-Host ""
Write-Host "UNIDAD I LEVANTADA EN DEV" -ForegroundColor Green
Write-Host "Config : http://localhost:18888"
Write-Host "Eureka : http://localhost:18761"
Write-Host "Gateway: http://localhost:18080"
Write-Host "Swagger cliente : http://localhost:8081/swagger-ui.html"
Write-Host "Swagger servicio: http://localhost:8082/swagger-ui.html"
Write-Host "Swagger orden   : http://localhost:8083/swagger-ui.html"
Write-Host "Swagger pago    : http://localhost:8084/swagger-ui.html"
Write-Host ""
Write-Host "Ejecuta scripts\windows\04-demo-s5.bat para probar CRUD, errores y balanceo."
