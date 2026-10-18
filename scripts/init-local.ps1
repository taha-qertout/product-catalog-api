$ErrorActionPreference = 'Stop'
Set-Location (Split-Path $PSScriptRoot -Parent)
if (Test-Path .env) {
    Write-Host '.env already exists; left unchanged.'
    exit 0
}
$password = [Convert]::ToHexString([System.Security.Cryptography.RandomNumberGenerator]::GetBytes(32))
@("POSTGRES_PASSWORD=$password", 'DB_PORT=55432', 'SERVER_PORT=8080') | Set-Content .env
Write-Host 'Created ignored .env with a random local password. Keep this file private.'
