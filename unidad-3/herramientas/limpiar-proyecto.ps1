<#
.SYNOPSIS
Previsualiza y retira copias antiguas y resultados de ensayos ya identificados.
.DESCRIPTION
Sin -Aplicar solo muestra el listado. Las rutas son cerradas y deben estar dentro
del proyecto. No incluye volúmenes Docker, bases de datos, documentos subidos,
credenciales activas, comprobantes de pago ni dependencias de desarrollo.
#>
[CmdletBinding(SupportsShouldProcess)]
param([switch]$Aplicar)

$ErrorActionPreference = 'Stop'
$projectRoot = [IO.Path]::GetFullPath((Split-Path -Parent $PSScriptRoot))
$rootPrefix = $projectRoot + [IO.Path]::DirectorySeparatorChar
$plan = [Collections.Generic.List[object]]::new()

function Add-CleanupTarget([string]$RelativePath, [string]$Reason, [switch]$MustBeEmpty) {
    $absolute = [IO.Path]::GetFullPath((Join-Path $projectRoot $RelativePath))
    if (-not $absolute.StartsWith($rootPrefix, [StringComparison]::OrdinalIgnoreCase)) {
        throw "Ruta fuera del proyecto: $RelativePath"
    }
    if (-not (Test-Path -LiteralPath $absolute)) { return }
    $item = Get-Item -LiteralPath $absolute -Force
    if ($item.LinkType) { throw "No se eliminará un enlace: $RelativePath" }
    if ($MustBeEmpty -and @(Get-ChildItem -LiteralPath $absolute -Recurse -File -Force).Count -ne 0) {
        Write-Warning "Se conserva $RelativePath porque contiene archivos nuevos."
        return
    }
    if ($item.PSIsContainer) {
        $links = @(Get-ChildItem -LiteralPath $absolute -Recurse -Force | Where-Object { $_.LinkType })
        if ($links.Count) { throw "No se recorrerán enlaces dentro de $RelativePath" }
    }
    $plan.Add([pscustomobject]@{ Ruta = $RelativePath; Motivo = $Reason; Absoluta = $absolute })
}

foreach ($folder in @('backend-legacy-casos', 'infra', 'services')) {
    Add-CleanupTarget $folder 'Carpeta anterior vacía; el código activo usa los nombres actuales' -MustBeEmpty
}
foreach ($folder in @(
    'respaldos/documentacion-retirada',
    'respaldos/retirado-b5a42a53',
    'respaldos/verification-generado-273dce99',
    '.github/modernize',
    'frontend/portal-web/.angular'
)) { Add-CleanupTarget $folder 'Copia retirada, herramienta anterior o caché regenerable' }

Add-CleanupTarget 'infraestructura/puerta-enlace/target/client-ui-review/overflow-ADMIN.png' 'Captura temporal de la revisión de interfaz'

foreach ($file in @(
    'credenciales/ACCESOS.md',
    'credenciales/demo.json',
    'credenciales/mercadopago-demo.json',
    'credenciales/mercadopago-prueba.env',
    'credenciales/public-demo-client-before.json',
    'credenciales/public-demo-containers-before.json',
    'credenciales/services.json',
    'credenciales/verificacion-integral.json',
    'credenciales/diagnostico-yape-sandbox.json',
    'credenciales/diagnostico-yape-sandbox-ultimo.json',
    'output/pdf/guia-bienvenida-illari-legal.pdf'
)) { Add-CleanupTarget $file 'Resultado de ensayo o copia; sin uso en el funcionamiento actual' }

foreach ($pair in @(
    @('illari-legal-welcome-v2.png', 'illari-legal-welcome-v2-1122.jpg'),
    @('portada-asesoria-original.png', 'portada-asesoria-1200.jpg'),
    @('portada-expedientes-original.png', 'portada-expedientes-1200.jpg')
)) {
    if (Test-Path -LiteralPath (Join-Path $projectRoot ('frontend/portal-web/src/assets/images/' + $pair[1]))) {
        Add-CleanupTarget ('frontend/portal-web/src/assets/images/' + $pair[0]) 'Original intermedio; el portal utiliza el JPEG optimizado'
    }
}

$plan | Select-Object Ruta, Motivo | Format-Table -AutoSize
if (-not $Aplicar) {
    Write-Output 'Previsualización: no se borró ningún archivo. Usa -Aplicar para ejecutar este listado.'
    return
}

foreach ($entry in $plan) {
    if ($PSCmdlet.ShouldProcess($entry.Ruta, 'Eliminar el elemento revisado')) {
        Remove-Item -LiteralPath $entry.Absoluta -Recurse -Force
    }
}

foreach ($emptyParent in @('respaldos', '.github', 'output/pdf', 'output', 'frontend/portal-web/public')) {
    $absolute = [IO.Path]::GetFullPath((Join-Path $projectRoot $emptyParent))
    if (-not $absolute.StartsWith($rootPrefix, [StringComparison]::OrdinalIgnoreCase)) { throw 'Ruta inválida.' }
    if ((Test-Path -LiteralPath $absolute) -and @(Get-ChildItem -LiteralPath $absolute -Force).Count -eq 0) {
        if ($PSCmdlet.ShouldProcess($emptyParent, 'Retirar la carpeta vacía')) {
            Remove-Item -LiteralPath $absolute -Force
        }
    }
}
Write-Output 'Limpieza completada. Las bases de datos y los comprobantes reales se conservaron.'
