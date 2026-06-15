# Libera a porta 8080, compila e inicia o MemoryCard.
$ErrorActionPreference = "Stop"
$root = Split-Path $PSScriptRoot -Parent

& (Join-Path $PSScriptRoot "stop.ps1")
& (Join-Path $PSScriptRoot "build.ps1")

$javaHome = $env:JAVA_HOME
if (-not $javaHome) { $javaHome = "C:\Program Files\Java\jdk-21.0.11" }
$java = Join-Path $javaHome "bin\java.exe"

$argfile = Get-ChildItem "$env:TEMP\cp_*.argfile" | Sort-Object LastWriteTime -Descending | Select-Object -First 1
$cp = (Get-Content $argfile.FullName -Raw) -replace '(?s).*-cp "([^"]+)".*', '$1'
$classes = Join-Path $root "target\classes"

Write-Host "Iniciando MemoryCard em http://localhost:8080 ..."
Set-Location $root
& $java -cp "$cp;$classes" com.memorycard.MemorycardApplication
