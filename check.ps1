param([string]$JdkHome = $env:JAVA_HOME, [switch]$RenderUI)
$ErrorActionPreference = 'Stop'
$projectRoot = $PSScriptRoot
if ($JdkHome) {
    $compiler = Join-Path $JdkHome 'bin/javac.exe'
    $runtime = Join-Path $JdkHome 'bin/java.exe'
} else {
    $compiler = (Get-Command javac -ErrorAction Stop).Source
    $runtime = (Get-Command java -ErrorAction Stop).Source
}
if (-not (Test-Path -LiteralPath $compiler) -or -not (Test-Path -LiteralPath $runtime)) {
    throw 'Pass -JdkHome with a JDK 17+ installation folder.'
}
Push-Location -LiteralPath $projectRoot
try {
    $classes = Join-Path $projectRoot 'out/classes'
    New-Item -ItemType Directory -Force -Path $classes | Out-Null
    $sources = Get-ChildItem -LiteralPath (Join-Path $projectRoot 'src') -Filter '*.java' -File
    & $compiler -encoding UTF-8 -d $classes @($sources.FullName)
    if ($LASTEXITCODE -ne 0) { throw 'Java compilation failed.' }
    & $runtime '-Djava.awt.headless=true' -cp $classes Main --self-test
    if ($LASTEXITCODE -ne 0) { throw 'Regression checks failed.' }
    if ($RenderUI) {
        & $runtime '-Djava.awt.headless=true' -cp $classes Main --render-ui (Join-Path $projectRoot 'out/ui-preview')
        if ($LASTEXITCODE -ne 0) { throw 'UI preview failed.' }
    }
} finally { Pop-Location }
