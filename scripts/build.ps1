# Compila o projeto e copia resources para target/classes (sem precisar do Maven no PATH).
$ErrorActionPreference = "Stop"
$root = Split-Path $PSScriptRoot -Parent
$classes = Join-Path $root "target\classes"
$javaHome = $env:JAVA_HOME
if (-not $javaHome) {
    $javaHome = "C:\Program Files\Java\jdk-21.0.11"
}
$javac = Join-Path $javaHome "bin\javac.exe"
if (-not (Test-Path $javac)) {
    throw "javac não encontrado em $javac. Instale o JDK 21 ou defina JAVA_HOME."
}

$argfile = Get-ChildItem "$env:TEMP\cp_*.argfile" -ErrorAction SilentlyContinue |
    Sort-Object LastWriteTime -Descending |
    Select-Object -First 1
if (-not $argfile) {
    throw "Classpath não encontrado. Rode MemorycardApplication.java uma vez no Cursor (Run) para o Java gerar o cp_*.argfile, depois execute este script."
}
$cp = (Get-Content $argfile.FullName -Raw) -replace '(?s).*-cp "([^"]+)".*', '$1'

New-Item -ItemType Directory -Force -Path $classes | Out-Null
$sources = Get-ChildItem (Join-Path $root "src\main\java") -Recurse -Filter "*.java" | ForEach-Object { $_.FullName }

Write-Host "Compilando $($sources.Count) arquivos Java..."
& $javac -parameters -d $classes -cp $cp @sources
if ($LASTEXITCODE -ne 0) { throw "Falha na compilação (exit $LASTEXITCODE)" }

Write-Host "Copiando resources..."
Copy-Item -Recurse -Force (Join-Path $root "src\main\resources\*") $classes
Write-Host "Build concluído em $classes"
