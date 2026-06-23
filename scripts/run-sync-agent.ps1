# Inicia o MemoryCard Sync Agent (programa desktop).
$ErrorActionPreference = "Stop"
$root = Split-Path $PSScriptRoot -Parent
$jar = Join-Path $root "sync-agent\target\memorycard-sync-agent-0.0.11.jar"

if (-not (Test-Path $jar)) {
    Write-Host "JAR não encontrado. Compilando sync-agent..."
    & (Join-Path $PSScriptRoot "build-sync-agent.ps1")
    if ($LASTEXITCODE -ne 0) { throw "Falha ao compilar sync-agent" }
}

$javaHome = $env:JAVA_HOME
if (-not $javaHome) { $javaHome = "C:\Program Files\Java\jdk-25.0.3" }
$java = Join-Path $javaHome "bin\java.exe"
if (-not (Test-Path $java)) { $java = "java" }

Write-Host "Iniciando MemoryCard Sync Agent..."
& $java -jar $jar
