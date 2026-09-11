$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$envFile = Join-Path $root ".env"

if (Test-Path $envFile) {
    $content = Get-Content $envFile -Raw
    if ($content -notmatch "CAMBIAR_EN_ENTORNO_LOCAL") {
        Write-Host "El archivo .env ya existe. No se modificó." -ForegroundColor Green
        exit 0
    }
}

$password = "Illari-" + ([guid]::NewGuid().ToString("N"))
$envContent = @"
DB_USERNAME=illari
DB_PASSWORD=$password
POSTGRES_PORT=15432
"@
[System.IO.File]::WriteAllText($envFile, $envContent.TrimStart(), (New-Object System.Text.UTF8Encoding($false)))

Write-Host "Entorno local configurado en .env." -ForegroundColor Green
Write-Host "La contraseña fue generada localmente y .gitignore evita versionarla."
