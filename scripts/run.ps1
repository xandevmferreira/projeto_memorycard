# Libera a porta 8080, compila, atualiza o JAR e inicia o MemoryCard.
$ErrorActionPreference = "Stop"
$root = Split-Path $PSScriptRoot -Parent

& (Join-Path $PSScriptRoot "stop.ps1")
& (Join-Path $PSScriptRoot "build.ps1")

$javaHome = $env:JAVA_HOME
if (-not $javaHome) { $javaHome = "C:\Program Files\Java\jdk-21.0.11" }
$java = Join-Path $javaHome "bin\java.exe"
if (-not (Test-Path $java)) { $javaHome = "C:\Program Files\Java\jdk-25.0.3"; $java = Join-Path $javaHome "bin\java.exe" }

Write-Host "Iniciando MemoryCard em http://localhost:8080 ..."
Set-Location $root
& $java -jar "target\memorycard-0.1.0.jar"
