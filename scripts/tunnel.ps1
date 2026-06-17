# Expõe o MemoryCard local (porta 8080) na internet via Cloudflare Quick Tunnel.
# Requisitos: MemoryCard rodando (scripts\run.ps1) e cloudflared instalado.
$ErrorActionPreference = "Stop"

$cloudflared = "C:\Program Files (x86)\cloudflared\cloudflared.exe"
if (-not (Test-Path $cloudflared)) {
    $cloudflared = "C:\Program Files\Cloudflare\cloudflared\cloudflared.exe"
}
if (-not (Test-Path $cloudflared)) {
    throw "cloudflared não encontrado. Instale com: winget install Cloudflare.cloudflared"
}

$portCheck = netstat -ano | Select-String ":8080"
if (-not $portCheck) {
    Write-Host "AVISO: nada está escutando na porta 8080. Rode o site antes:"
    Write-Host "  powershell -ExecutionPolicy Bypass -File scripts\run.ps1"
    exit 1
}

Write-Host ""
Write-Host "Iniciando tunnel... O link público aparecerá abaixo (https://....trycloudflare.com)"
Write-Host "Mantenha esta janela aberta. Ctrl+C para encerrar o tunnel."
Write-Host ""

& $cloudflared tunnel --url http://localhost:8080
