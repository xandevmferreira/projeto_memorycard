# Agente opcional de sync — envia saves/ROMs da pasta do emulador para o MemoryCard na nuvem.
# Rode no SEU PC enquanto joga; o site hospedado recebe os arquivos via API.
#
# Uso (recomendado — token do Perfil → MemoryCard Sync):
#   powershell -ExecutionPolicy Bypass -File scripts\archive-sync.ps1 `
#     -GameId 5 `
#     -WatchFolder "C:\Emuladores\Snes9x\Saves" `
#     -BaseUrl "https://seu-memorycard.com" `
#     -SyncToken "mc_sync_..."
#
param(
    [Parameter(Mandatory = $true)]
    [long]$GameId,

    [Parameter(Mandatory = $true)]
    [string]$WatchFolder,

    [string]$BaseUrl = "http://localhost:8080",
    [string]$SyncToken = "",
    [string]$Token = "",
    [string]$Email = "",
    [string]$Password = "",
    [int]$PollSeconds = 15,
    [string]$NameFilter = "*"
)

$ErrorActionPreference = "Stop"

$extensions = @(
    ".sav", ".srm", ".sra", ".sta", ".dsv", ".mcr", ".mem", ".eep",
    ".state", ".st0", ".st1", ".st2", ".savestate",
    ".sfc", ".smc", ".nes", ".gb", ".gbc", ".gba", ".n64", ".z64"
)

function Get-AuthToken {
    if ($SyncToken) { return $SyncToken }
    if ($Token) { return $Token }
    if (-not $Email -or -not $Password) {
        throw "Informe -SyncToken (recomendado) ou -Token, ou -Email e -Password."
    }
    $body = @{ email = $Email; password = $Password } | ConvertTo-Json
    $resp = Invoke-RestMethod -Uri "$BaseUrl/api/auth/login" -Method Post -Body $body -ContentType "application/json"
    return $resp.token
}

function Send-ArchiveFile {
    param([string]$FilePath, [string]$AuthToken)
    $uri = "$BaseUrl/api/games/$GameId/archives/sync"
    curl.exe -s -S -X POST $uri `
        -H "Authorization: Bearer $AuthToken" `
        -F "file=@$FilePath" | Out-Null
}

if (-not (Test-Path $WatchFolder)) {
    throw "Pasta não encontrada: $WatchFolder"
}

$authToken = Get-AuthToken
$known = @{}

Write-Host ""
Write-Host "MemoryCard Archive Sync"
Write-Host "  Jogo ID:   $GameId"
Write-Host "  Pasta:     $WatchFolder"
Write-Host "  Servidor:  $BaseUrl"
Write-Host "  Poll:      ${PollSeconds}s"
Write-Host ""
Write-Host "Mantenha aberto enquanto joga. Ctrl+C para encerrar."
Write-Host ""

while ($true) {
    Get-ChildItem -Path $WatchFolder -File -Filter $NameFilter -ErrorAction SilentlyContinue | ForEach-Object {
        $ext = [System.IO.Path]::GetExtension($_.Name).ToLower()
        if ($extensions -notcontains $ext) { return }

        $key = $_.FullName
        $stamp = "$($_.LastWriteTimeUtc.Ticks)-$($_.Length)"
        if ($known[$key] -eq $stamp) { return }
        $known[$key] = $stamp

        try {
            Send-ArchiveFile -FilePath $_.FullName -AuthToken $authToken
            Write-Host "[$(Get-Date -Format 'HH:mm:ss')] Enviado: $($_.Name)"
        } catch {
            Write-Host "[$(Get-Date -Format 'HH:mm:ss')] Erro em $($_.Name): $($_.Exception.Message)"
        }
    }
    Start-Sleep -Seconds $PollSeconds
}
