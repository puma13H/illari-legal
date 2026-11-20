param(
    [Parameter(Position=0)]
    [ValidateSet('preparar','iniciar','detener','estado','compilar','verificar','actualizar-web','url','observabilidad')]
    [string]$Accion = 'estado',
    [switch]$Aislado
)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$previousDirectory = Get-Location
try {
    Set-Location -LiteralPath $projectRoot
    if ($Accion -eq 'preparar') {
        $prepareArgs = @()
        if ($Aislado) { $prepareArgs += '--aislado' }
        & node (Join-Path $PSScriptRoot 'preparar-entorno.mjs') @prepareArgs
        if ($LASTEXITCODE -ne 0) { throw 'No se pudo preparar el entorno local.' }
        return
    }
    if ($Aislado) { throw 'Usa -Aislado con la acción preparar.' }
    if (-not (Test-Path -LiteralPath 'credenciales/runtime.env')) { throw 'Falta credenciales/runtime.env. Consulta documentacion/OPERACION.md.' }
    $composeArgs = @('--env-file','credenciales/runtime.env','-f','docker-compose.yml','-f','infraestructura/observabilidad/compose.observabilidad.yml','--profile','obs')
    if (Test-Path -LiteralPath 'credenciales/public-demo.env') {
        $composeArgs = @('--env-file','credenciales/runtime.env','--env-file','credenciales/public-demo.env','-f','docker-compose.yml','-f','infraestructura/observabilidad/compose.observabilidad.yml','-f','infraestructura/acceso-publico/compose.yml','--profile','obs')
    }
    function Invoke-Compose([string[]]$Arguments) {
        & docker compose @composeArgs @Arguments
        if ($LASTEXITCODE -ne 0) { throw 'La operación de Docker no se completó.' }
    }
    function Initialize-GrafanaPassword {
        & node (Join-Path $PSScriptRoot 'preparar-entorno.mjs') --solo-grafana
        if ($LASTEXITCODE -ne 0) { throw 'No se pudo preparar la contraseña de Grafana.' }
    }
    function Sync-PublicUrl {
        & node (Join-Path $PSScriptRoot 'actualizar-url.mjs')
        if ($LASTEXITCODE -ne 0) { throw 'No se pudo sincronizar la URL pública.' }
        Invoke-Compose -Arguments @('up','-d','--no-deps','keycloak','cliente-ms','servicio-legal-ms','orden-legal-ms','pago-ms','backend-legacy','legal-gateway','checkout-web','publico-entrada')
    }
    switch ($Accion) {
        'estado' { Invoke-Compose -Arguments @('ps','--all') }
        'detener' { Invoke-Compose -Arguments @('stop') }
        'actualizar-web' { Invoke-Compose -Arguments @('up','-d','--no-deps','--build','checkout-web') }
        'observabilidad' {
            Initialize-GrafanaPassword
            Invoke-Compose -Arguments @('up','-d','--wait','--wait-timeout','180','prometheus','loki','blackbox','alloy','grafana')
        }
        'url' {
            if (-not (Test-Path -LiteralPath 'credenciales/public-demo.env')) { throw 'El modo público necesita credenciales/public-demo.env.' }
            Sync-PublicUrl
        }
        'iniciar' {
            Initialize-GrafanaPassword
            Invoke-Compose -Arguments @('config','--quiet')
            Invoke-Compose -Arguments @('up','-d','--wait','--wait-timeout','180','postgres','keycloak')
            & docker compose @composeArgs exec -T postgres psql -U postgres -d illari_juridico -v ON_ERROR_STOP=1 -f /migrations/06-registros-y-presentacion.sql
            if ($LASTEXITCODE -ne 0) { throw 'No se pudo actualizar el esquema de expedientes.' }
            & docker compose @composeArgs exec -T postgres psql -U postgres -d illari_pagos -v ON_ERROR_STOP=1 -f /migrations/07-diagnostico-pagos-yape.sql
            if ($LASTEXITCODE -ne 0) { throw 'No se pudo actualizar el diagnóstico de pagos.' }
            $migrations = @(
                @('illari_servicios','08-entrega-catalogo.sql'),
                @('illari_ordenes','09-entrega-pedidos.sql'),
                @('illari_juridico','10-expedientes-de-pedidos.sql'),
                @('illari_pagos','11-conciliacion-yape.sql')
            )
            foreach ($migration in $migrations) {
                & docker compose @composeArgs exec -T postgres psql -U postgres -d $migration[0] -v ON_ERROR_STOP=1 -f ('/migrations/' + $migration[1])
                if ($LASTEXITCODE -ne 0) { throw ('No se pudo aplicar ' + $migration[1]) }
            }
            & node (Join-Path $PSScriptRoot 'configurar-registros.mjs')
            if ($LASTEXITCODE -ne 0) { throw 'No se pudo preparar la gestión de usuarios.' }
            Invoke-Compose -Arguments @('up','-d','--wait','--wait-timeout','240')
            if (Test-Path -LiteralPath 'credenciales/public-demo.env') { Sync-PublicUrl }
            $config = @(Get-Content -LiteralPath 'credenciales/runtime.env' | Where-Object { $_ -match '^FRONTEND_PUBLIC_URL=' })
            $portalUrl = if ($config.Count) { $config[-1].Substring('FRONTEND_PUBLIC_URL='.Length).Trim() } else { 'http://localhost:4200' }
            Write-Output ('Portal local: ' + $portalUrl)
        }
        'compilar' {
            & mvn -f (Join-Path $projectRoot 'pom.xml') package -DskipTests -q
            if ($LASTEXITCODE -ne 0) { throw 'No se pudieron compilar los módulos Java.' }
            Push-Location -LiteralPath 'frontend/portal-web'
            try {
                if (-not (Test-Path -LiteralPath 'node_modules')) { & npm ci; if ($LASTEXITCODE -ne 0) { throw 'No se pudieron instalar las dependencias del portal.' } }
                & npm run build
                if ($LASTEXITCODE -ne 0) { throw 'No se pudo compilar el portal.' }
            } finally { Pop-Location }
            Invoke-Compose -Arguments @('build')
        }
        'verificar' {
            Invoke-Compose -Arguments @('config','--quiet')
            & mvn -f (Join-Path $projectRoot 'pom.xml') verify -q
            if ($LASTEXITCODE -ne 0) { throw 'Falló la verificación de los módulos Java.' }
            Push-Location -LiteralPath 'frontend/portal-web'
            try {
                if (-not (Test-Path -LiteralPath 'node_modules')) { & npm ci; if ($LASTEXITCODE -ne 0) { throw 'No se pudieron instalar las dependencias del portal.' } }
                & npm run build
                if ($LASTEXITCODE -ne 0) { throw 'No se pudo compilar el portal.' }
            } finally { Pop-Location }
            Write-Output 'Verificación completada: Docker, Java y Angular.'
        }
    }
} finally { Set-Location -LiteralPath $previousDirectory.Path }
