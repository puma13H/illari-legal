param(
    [string]$Gateway = "http://localhost:18080",
    [string]$ConfigServer = "http://localhost:18888",
    [string]$Eureka = "http://localhost:18761"
)

$ErrorActionPreference = "Stop"

function Print-Step([string]$text) {
    Write-Host ""
    Write-Host ("=" * 72) -ForegroundColor DarkGray
    Write-Host $text -ForegroundColor Cyan
}

function Json($obj) { return ($obj | ConvertTo-Json -Depth 8 -Compress) }

Print-Step "1. CONFIG SERVER: DEV y PROD"
$dev = Invoke-RestMethod "$ConfigServer/orden-legal-ms/dev"
$prod = Invoke-RestMethod "$ConfigServer/orden-legal-ms/prod"
Write-Host "DEV  -> perfiles: $($dev.profiles -join ', ')"
Write-Host "PROD -> perfiles: $($prod.profiles -join ', ')"
Write-Host "Configuración centralizada disponible." -ForegroundColor Green

Print-Step "2. EUREKA: registro"
$headers = @{ Accept = "application/json" }
$apps = Invoke-RestMethod "$Eureka/eureka/apps" -Headers $headers
$names = @($apps.applications.application | ForEach-Object { $_.name })
Write-Host ("Aplicaciones: " + ($names -join ", "))
$ordenApp = @($apps.applications.application | Where-Object { $_.name -eq "ORDEN-LEGAL-MS" })
if ($ordenApp) {
    $instances = @($ordenApp.instance)
    Write-Host "ORDEN-LEGAL-MS registradas: $($instances.Count)" -ForegroundColor Green
    $instances | ForEach-Object { Write-Host " - $($_.instanceId) / $($_.status)" }
} else {
    Write-Warning "ORDEN-LEGAL-MS todavía no aparece en Eureka."
}

Print-Step "3. CRUD COMPLETO DE ORDEN vía GATEWAY"
$createBody = @{
    idCliente = 1
    servicios = @(1,2)
    total = 330.00
    estado = "CREADA"
}
$created = Invoke-RestMethod -Method Post -Uri "$Gateway/api/v1/ordenes" -ContentType "application/json" -Body (Json $createBody)
$id = $created.id
Write-Host "POST 201/OK -> orden creada ID=$id" -ForegroundColor Green

$got = Invoke-RestMethod "$Gateway/api/v1/ordenes/$id"
Write-Host "GET -> estado=$($got.estado), total=$($got.total)" -ForegroundColor Green

$updateBody = @{
    idCliente = 1
    servicios = @(1,2)
    total = 350.00
    estado = "EN_PROCESO"
}
$updated = Invoke-RestMethod -Method Put -Uri "$Gateway/api/v1/ordenes/$id" -ContentType "application/json" -Body (Json $updateBody)
Write-Host "PUT -> estado=$($updated.estado), total=$($updated.total)" -ForegroundColor Green

Print-Step "4. BALANCEO: varias peticiones al mismo endpoint"
1..10 | ForEach-Object {
    $r = Invoke-WebRequest -UseBasicParsing "$Gateway/api/v1/ordenes/$id"
    $instance = $r.Headers["X-Instance-ID"]
    $trace = $r.Headers["X-Trace-ID"]
    Write-Host ("Petición {0,2}: instancia={1} trace={2}" -f $_,$instance,$trace)
}
Write-Host "Debes observar las instancias 8083 y 8085 en DEV." -ForegroundColor Yellow

Print-Step "5. CASO DE ERROR 400"
try {
    $invalid = @{ servicios = @(); total = -1; estado = "INVALIDO" }
    Invoke-RestMethod -Method Post -Uri "$Gateway/api/v1/ordenes" -ContentType "application/json" -Body (Json $invalid)
    Write-Warning "No se produjo el error esperado."
} catch {
    if ($_.Exception.Response) {
        Write-Host "HTTP $([int]$_.Exception.Response.StatusCode) recibido correctamente." -ForegroundColor Green
    } else { Write-Host $_.Exception.Message }
}

Print-Step "6. DELETE y verificación 404"
Invoke-WebRequest -UseBasicParsing -Method Delete "$Gateway/api/v1/ordenes/$id" | Out-Null
Write-Host "DELETE -> 204 esperado." -ForegroundColor Green
try {
    Invoke-RestMethod "$Gateway/api/v1/ordenes/$id" | Out-Null
    Write-Warning "La orden todavía existe."
} catch {
    if ($_.Exception.Response) {
        Write-Host "GET posterior -> HTTP $([int]$_.Exception.Response.StatusCode), esperado 404." -ForegroundColor Green
    } else { Write-Host $_.Exception.Message }
}

Print-Step "7. PRUEBA DE TOLERANCIA A CAÍDA"
Write-Host "DEV: ejecuta scripts\windows\06-detener-instancia-orden-2.bat y luego scripts\windows\05-probar-balanceo.bat."
Write-Host "Docker PROD: docker stop orden-legal-ms-2 ; luego ejecuta scripts\windows\09-probar-balanceo-prod.bat."
Write-Host "El Gateway debe seguir respondiendo mediante la instancia restante." -ForegroundColor Yellow
