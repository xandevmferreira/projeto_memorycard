# Gera POSTGRES_PASSWORD e JWT_SECRET para o arquivo .env
$ErrorActionPreference = "Stop"
$root = Split-Path $PSScriptRoot -Parent
$example = Join-Path $root "deploy\.env.example"
$envFile = Join-Path $root ".env"

function New-Secret([int]$bytes = 32) {
    $buffer = New-Object byte[] $bytes
    [System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($buffer)
    return [Convert]::ToBase64String($buffer)
}

$postgres = New-Secret 24
$jwt = New-Secret 48

if (Test-Path $envFile) {
    Write-Host "Arquivo .env já existe — não sobrescrevi."
    Write-Host "Use estes valores se precisar:"
} else {
    $domain = Read-Host "Domínio (ex: memorycard.seudominio.com.br)"
    if (-not $domain) { $domain = "seudominio.com.br" }
    $email = Read-Host "E-mail para HTTPS (Let's Encrypt)"
    if (-not $email) { $email = "admin@$domain" }

    @"
DOMAIN=$domain
POSTGRES_PASSWORD=$postgres
JWT_SECRET=$jwt
ACME_EMAIL=$email
"@ | Set-Content -Path $envFile -Encoding UTF8
    Write-Host "Criado: $envFile"
}

Write-Host ""
Write-Host "POSTGRES_PASSWORD=$postgres"
Write-Host "JWT_SECRET=$jwt"
Write-Host ""
Write-Host "Próximo passo: veja docs/DEPLOY.md"
