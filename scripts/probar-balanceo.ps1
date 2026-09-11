param([string]$Gateway = "http://localhost:18080")
$ErrorActionPreference = "Stop"
$orders = Invoke-RestMethod "$Gateway/api/v1/ordenes"
if (-not $orders -or $orders.Count -eq 0) {
    $body = @{ idCliente=1; servicios=@(1); total=80.00; estado="CREADA" } | ConvertTo-Json
    $order = Invoke-RestMethod -Method Post -Uri "$Gateway/api/v1/ordenes" -ContentType "application/json" -Body $body
    $id = $order.id
} else {
    $id = @($orders)[0].id
}
1..12 | ForEach-Object {
    $r = Invoke-WebRequest -UseBasicParsing "$Gateway/api/v1/ordenes/$id"
    Write-Host ("{0,2}. X-Instance-ID = {1} | X-Trace-ID = {2}" -f $_,$r.Headers["X-Instance-ID"],$r.Headers["X-Trace-ID"])
}
