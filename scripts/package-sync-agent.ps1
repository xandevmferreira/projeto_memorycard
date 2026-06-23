# Gera instalador/app do MemoryCard Sync Agent via jpackage.
# Se WiX não estiver instalado, gera app-image + script de atalho (sem .exe instalador).
$ErrorActionPreference = "Stop"
$root = Split-Path $PSScriptRoot -Parent
$agentRoot = Join-Path $root "sync-agent"
$jar = Join-Path $agentRoot "target\memorycard-sync-agent-0.0.11.jar"
$icon = Join-Path $agentRoot "icon.ico"
$installerDir = Join-Path $agentRoot "target\installer"

$javaHome = $env:JAVA_HOME
if (-not $javaHome) { $javaHome = "C:\Program Files\Java\jdk-21.0.11" }
$jpackage = Join-Path $javaHome "bin\jpackage.exe"
$java = Join-Path $javaHome "bin\java.exe"
if (-not (Test-Path $jpackage)) { $javaHome = "C:\Program Files\Java\jdk-25.0.3"; $jpackage = Join-Path $javaHome "bin\jpackage.exe"; $java = Join-Path $javaHome "bin\java.exe" }
if (-not (Test-Path $jpackage)) { throw "jpackage não encontrado. Use JDK 21+ com jpackage." }

if (-not (Test-Path $jar)) {
    & (Join-Path $PSScriptRoot "build-sync-agent.ps1")
}

function Ensure-Icon {
    if (Test-Path $icon) { return }
    Write-Host "Gerando icon.ico..."
    $iconGenClasses = Join-Path $agentRoot "target\icon-gen"
    $iconGenSrc = Join-Path $agentRoot "src\main\java"
    New-Item -ItemType Directory -Force -Path $iconGenClasses | Out-Null
    & (Join-Path $javaHome "bin\javac.exe") --release 21 -d $iconGenClasses `
        (Join-Path $iconGenSrc "com\memorycard\sync\ui\AppIcon.java") `
        (Join-Path $iconGenSrc "com\memorycard\sync\ui\IconGenerator.java")
    if ($LASTEXITCODE -ne 0) { return }
    & $java -cp $iconGenClasses com.memorycard.sync.ui.IconGenerator $icon
}

Ensure-Icon

Remove-Item -Recurse -Force $installerDir -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force -Path $installerDir | Out-Null

$inputDir = Join-Path $agentRoot "target"
$commonArgs = @(
    "--input", $inputDir,
    "--main-jar", "memorycard-sync-agent-0.0.11.jar",
    "--main-class", "com.memorycard.sync.SyncAgentApp",
    "--name", "MemoryCard Sync",
    "--app-version", "0.0.11",
    "--vendor", "MemoryCard",
    "--description", "Envia saves do emulador para sua conta MemoryCard",
    "--dest", $installerDir
)
if (Test-Path $icon) {
    $commonArgs += @("--icon", $icon)
}

function Test-WiX {
    return (Get-Command candle.exe -ErrorAction SilentlyContinue) -or (Get-Command wix.exe -ErrorAction SilentlyContinue)
}

if (Test-WiX) {
    Write-Host "Criando instalador .exe (WiX detectado)..."
    & $jpackage @commonArgs --type exe --win-menu --win-shortcut --win-per-user-install
    if ($LASTEXITCODE -eq 0) {
        $exe = Get-ChildItem $installerDir -Filter "*.exe" | Select-Object -First 1
        Write-Host "Instalador pronto: $($exe.FullName)"
        exit 0
    }
    Write-Host "Falha no instalador WiX; tentando app-image..."
}

Write-Host "Criando app-image (não requer WiX)..."
& $jpackage @commonArgs --type app-image
if ($LASTEXITCODE -ne 0) { throw "jpackage falhou" }

$appDir = Join-Path $installerDir "MemoryCard Sync"
$appExe = Join-Path $appDir "MemoryCard Sync.exe"
if (-not (Test-Path $appExe)) { throw "Executável não encontrado em $appDir" }

# Script de "instalação" local (atalho na área de trabalho + menu iniciar)
$installScript = Join-Path $installerDir "instalar-memorycard-sync.ps1"
@'
# Instala MemoryCard Sync localmente (sem WiX).
$ErrorActionPreference = "Stop"
$here = Split-Path $MyInvocation.MyCommand.Path -Parent
$source = Join-Path $here "MemoryCard Sync"
$target = Join-Path $env:LOCALAPPDATA "MemoryCard Sync"
$exe = Join-Path $target "MemoryCard Sync.exe"

Write-Host "Copiando para $target ..."
Remove-Item -Recurse -Force $target -ErrorAction SilentlyContinue
Copy-Item -Recurse -Force $source $target

$wsh = New-Object -ComObject WScript.Shell
$desktop = [Environment]::GetFolderPath("Desktop")
$shortcut = $wsh.CreateShortcut((Join-Path $desktop "MemoryCard Sync.lnk"))
$shortcut.TargetPath = $exe
$shortcut.WorkingDirectory = $target
$shortcut.Save()

$startMenu = Join-Path $env:APPDATA "Microsoft\Windows\Start Menu\Programs"
$startShortcut = $wsh.CreateShortcut((Join-Path $startMenu "MemoryCard Sync.lnk"))
$startShortcut.TargetPath = $exe
$startShortcut.WorkingDirectory = $target
$startShortcut.Save()

Write-Host "Instalado! Atalho criado na área de trabalho."
Write-Host "Execute: $exe"
'@ | Set-Content -Path $installScript -Encoding UTF8

Write-Host ""
Write-Host "App pronto: $appExe"
Write-Host "Para instalar com atalhos, rode:"
Write-Host "  powershell -ExecutionPolicy Bypass -File `"$installScript`""
