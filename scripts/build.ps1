# Compila o projeto, copia resources e atualiza o JAR (sem precisar do Maven no PATH).
$ErrorActionPreference = "Stop"
$root = Split-Path $PSScriptRoot -Parent
$classes = Join-Path $root "target\classes"
$jar = Join-Path $root "target\memorycard-0.1.0.jar"
$libDir = Join-Path $root "target\boot-libs"

$javaHome = $env:JAVA_HOME
if (-not $javaHome) { $javaHome = "C:\Program Files\Java\jdk-21.0.11" }
$javac = Join-Path $javaHome "bin\javac.exe"
if (-not (Test-Path $javac)) { $javaHome = "C:\Program Files\Java\jdk-25.0.3"; $javac = Join-Path $javaHome "bin\javac.exe" }
if (-not (Test-Path $javac)) { throw "javac não encontrado. Instale o JDK 21+ ou defina JAVA_HOME." }

$argfile = Get-ChildItem "$env:TEMP\cp_*.argfile" -ErrorAction SilentlyContinue |
    Sort-Object LastWriteTime -Descending |
    Select-Object -First 1

if ($argfile) {
    $cp = (Get-Content $argfile.FullName -Raw) -replace '(?s).*-cp "([^"]+)".*', '$1'
} else {
    if (-not (Test-Path $jar)) { throw "JAR não encontrado em $jar. Compile pelo menos uma vez com Maven ou Cursor." }
    if (-not (Test-Path (Join-Path $libDir "spring-boot-3.3.5.jar"))) {
        New-Item -ItemType Directory -Force -Path $libDir | Out-Null
        Push-Location $root
        jar xf $jar "BOOT-INF/lib" 2>$null
        if (Test-Path "BOOT-INF\lib") {
            Move-Item "BOOT-INF\lib\*" $libDir -Force
            Remove-Item -Recurse -Force "BOOT-INF"
        }
        Pop-Location
    }
    $cp = ((Get-ChildItem $libDir -Filter *.jar | ForEach-Object { $_.FullName }) -join ';')
}

New-Item -ItemType Directory -Force -Path $classes | Out-Null
$sources = Get-ChildItem (Join-Path $root "src\main\java") -Recurse -Filter "*.java" | ForEach-Object { $_.FullName }

Write-Host "Compilando $($sources.Count) arquivos Java..."
& $javac -parameters --release 21 -d $classes -cp $cp @sources
if ($LASTEXITCODE -ne 0) { throw "Falha na compilação (exit $LASTEXITCODE)" }

Write-Host "Copiando resources..."
Copy-Item -Recurse -Force (Join-Path $root "src\main\resources\*") $classes

if (-not (Test-Path $jar)) { throw "JAR não encontrado: $jar" }
$tmpdir = Join-Path $root "target\_jarupdate"
Remove-Item -Recurse -Force $tmpdir -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force -Path (Join-Path $tmpdir "BOOT-INF\classes") | Out-Null
Copy-Item -Recurse -Force "$classes\*" (Join-Path $tmpdir "BOOT-INF\classes")
Push-Location $tmpdir
& jar uf $jar "BOOT-INF/classes"
if ($LASTEXITCODE -ne 0) { Pop-Location; throw "Falha ao atualizar o JAR" }
Pop-Location
Remove-Item -Recurse -Force $tmpdir

Write-Host "Build concluído. JAR atualizado: $jar"
