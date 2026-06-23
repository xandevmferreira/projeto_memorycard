# Compila o MemoryCard Sync Agent (sem Maven no PATH).
$ErrorActionPreference = "Stop"
$root = Split-Path $PSScriptRoot -Parent
$agentRoot = Join-Path $root "sync-agent"
$classes = Join-Path $agentRoot "target\classes"
$jar = Join-Path $agentRoot "target\memorycard-sync-agent-0.0.11.jar"
$libDir = Join-Path $agentRoot "target\lib"
$mainJar = Join-Path $root "target\memorycard-0.1.0.jar"

$javaHome = $env:JAVA_HOME
if (-not $javaHome) { $javaHome = "C:\Program Files\Java\jdk-21.0.11" }
$javac = Join-Path $javaHome "bin\javac.exe"
$jarExe = Join-Path $javaHome "bin\jar.exe"
if (-not (Test-Path $javac)) { $javaHome = "C:\Program Files\Java\jdk-25.0.3"; $javac = Join-Path $javaHome "bin\javac.exe"; $jarExe = Join-Path $javaHome "bin\jar.exe" }
if (-not (Test-Path $javac)) { throw "javac não encontrado. Instale o JDK 21+ ou defina JAVA_HOME." }

function Ensure-JacksonLibs {
    if ((Test-Path (Join-Path $libDir "jackson-databind-2.17.2.jar")) -and
        (Test-Path (Join-Path $libDir "jackson-core-2.17.2.jar")) -and
        (Test-Path (Join-Path $libDir "jackson-annotations-2.17.2.jar"))) {
        return
    }
    if (-not (Test-Path $mainJar)) {
        throw "JAR principal não encontrado: $mainJar. Rode scripts\build.ps1 primeiro."
    }
    New-Item -ItemType Directory -Force -Path $libDir | Out-Null
    $tmpdir = Join-Path $agentRoot "target\_extract"
    Remove-Item -Recurse -Force $tmpdir -ErrorAction SilentlyContinue
    New-Item -ItemType Directory -Force -Path $tmpdir | Out-Null
    Push-Location $tmpdir
    & $jarExe xf $mainJar "BOOT-INF/lib/jackson-databind-2.17.2.jar" "BOOT-INF/lib/jackson-core-2.17.2.jar" "BOOT-INF/lib/jackson-annotations-2.17.2.jar"
    if ($LASTEXITCODE -ne 0) { Pop-Location; throw "Falha ao extrair Jackson do JAR principal" }
    Move-Item "BOOT-INF\lib\*.jar" $libDir -Force
    Pop-Location
    Remove-Item -Recurse -Force $tmpdir
}

Ensure-JacksonLibs

$cp = ((Get-ChildItem $libDir -Filter *.jar | ForEach-Object { $_.FullName }) -join ';')
$sources = Get-ChildItem (Join-Path $agentRoot "src\main\java") -Recurse -Filter "*.java" | ForEach-Object { $_.FullName }

Remove-Item -Recurse -Force $classes -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force -Path $classes | Out-Null

Write-Host "Compilando sync-agent ($($sources.Count) arquivos)..."
& $javac --release 21 -d $classes -cp $cp @sources
if ($LASTEXITCODE -ne 0) { throw "Falha na compilação do sync-agent" }

# Fat JAR: classes + dependências Jackson
$fatDir = Join-Path $agentRoot "target\_fat"
Remove-Item -Recurse -Force $fatDir -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force -Path $fatDir | Out-Null
Copy-Item -Recurse -Force "$classes\*" $fatDir
Push-Location $fatDir
foreach ($dep in Get-ChildItem $libDir -Filter *.jar) {
    & $jarExe xf $dep.FullName
}
Pop-Location

New-Item -ItemType Directory -Force -Path (Split-Path $jar -Parent) | Out-Null
$manifest = Join-Path $agentRoot "target\MANIFEST.MF"
@"
Manifest-Version: 1.0
Main-Class: com.memorycard.sync.SyncAgentApp

"@ | Set-Content -Path $manifest -Encoding ASCII

Push-Location $fatDir
& $jarExe cfm $jar $manifest .
if ($LASTEXITCODE -ne 0) { Pop-Location; throw "Falha ao criar JAR" }
Pop-Location
Remove-Item -Recurse -Force $fatDir
Remove-Item -Force $manifest -ErrorAction SilentlyContinue

$staticJar = Join-Path $root "src\main\resources\static\download\memorycard-sync.jar"
New-Item -ItemType Directory -Force -Path (Split-Path $staticJar -Parent) | Out-Null
Copy-Item -Force $jar $staticJar

Write-Host "Sync agent pronto: $jar"
Write-Host "Copiado para download no site: $staticJar"
