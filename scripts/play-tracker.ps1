# Contador opcional de horas — detecta quando um processo está rodando e reporta ao MemoryCard.
# Uso:
#   .\scripts\play-tracker.ps1 -GameId 5 -ProcessName "javaw" -Email "voce@email.com" -Password "senha"
#   .\scripts\play-tracker.ps1 -GameId 5 -ProcessName "Minecraft" -BaseUrl "http://localhost:8080" -Token "JWT..."
#
param(
    [Parameter(Mandatory = $true)]
    [long]$GameId,

    [Parameter(Mandatory = $true)]
    [string]$ProcessName,

    [string]$BaseUrl = "http://localhost:8080",
    [string]$Email = "",
    [string]$Password = "",
    [string]$Token = "",
    [int]$PollSeconds = 30
)

$ErrorActionPreference = "Stop"

function Get-AuthToken {
    if ($Token) { return $Token }
    if (-not $Email -or -not $Password) {
        throw "Informe -Token ou -Email e -Password para autenticar."
    }
    $body = @{ email = $Email; password = $Password } | ConvertTo-Json
    $resp = Invoke-RestMethod -Uri "$BaseUrl/api/auth/login" -Method Post -Body $body -ContentType "application/json"
    return $resp.token
}

function Invoke-SessionApi {
    param([string]$Method, [string]$Path, [hashtable]$Body, [string]$AuthToken)
    $headers = @{ Authorization = "Bearer $AuthToken" }
    $uri = "$BaseUrl$Path"
    if ($Body) {
        $json = $Body | ConvertTo-Json
        return Invoke-RestMethod -Uri $uri -Method $Method -Headers $headers -Body $json -ContentType "application/json"
    }
    return Invoke-RestMethod -Uri $uri -Method $Method -Headers $headers
}

function Test-ProcessRunning([string]$Name) {
    return [bool](Get-Process -Name $Name -ErrorAction SilentlyContinue)
}

$authToken = Get-AuthToken
$sessionActive = $false

Write-Host ""
Write-Host "MemoryCard Play Tracker"
Write-Host "  Jogo ID:     $GameId"
Write-Host "  Processo:    $ProcessName"
Write-Host "  Servidor:    $BaseUrl"
Write-Host "  Poll:        ${PollSeconds}s"
Write-Host ""

Write-Host "Mantenha esta janela aberta. Ctrl+C para encerrar."

while ($true) {
    $running = Test-ProcessRunning $ProcessName
    if ($running -and -not $sessionActive) {
        try {
            Invoke-SessionApi -Method Post -Path "/api/play-sessions/start" -Body @{
                gameId = $GameId
                source = "TRACKER"
                processName = $ProcessName
            } -AuthToken $authToken | Out-Null
            $sessionActive = $true
            Write-Host "[$(Get-Date -Format 'HH:mm:ss')] Sessão iniciada ($ProcessName detectado)"
        } catch {
            Write-Host "[$(Get-Date -Format 'HH:mm:ss')] Erro ao iniciar: $($_.Exception.Message)"
        }
    }
    elseif (-not $running -and $sessionActive) {
        try {
            Invoke-SessionApi -Method Post -Path "/api/play-sessions/stop" -Body @{ gameId = $GameId } -AuthToken $authToken | Out-Null
            $sessionActive = $false
            Write-Host "[$(Get-Date -Format 'HH:mm:ss')] Sessão encerrada (processo fechou)"
        } catch {
            Write-Host "[$(Get-Date -Format 'HH:mm:ss')] Erro ao parar: $($_.Exception.Message)"
        }
    }
    Start-Sleep -Seconds $PollSeconds
}
