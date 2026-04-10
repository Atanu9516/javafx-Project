param(
    [ValidateSet("exe", "app-image")]
    [string]$Type = "exe",
    [string]$AppName = "OnlineMCQExam",
    [string]$AppVersion = "1.0.0",
    [string]$MainJar = "onlinemcqexam-1.0-SNAPSHOT.jar",
    [string]$MainClass = "com.example.onlinemcqexam.Launcher",
    [string]$AddModules = "javafx.controls,javafx.fxml,org.kordamp.ikonli.javafx,org.kordamp.ikonli.fontawesome5",
    [switch]$WinConsole,
    [switch]$EnableDebugLogs,
    [switch]$VerboseJPackage,
    [string[]]$JavaOptions = @("-Djava.net.preferIPv4Stack=true")
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $projectRoot

Write-Host "[1/4] Building Maven artifact..."
& .\mvnw.cmd package -DskipTests
if ($LASTEXITCODE -ne 0) { throw "Maven build failed with exit code $LASTEXITCODE" }

$targetDir = Join-Path $projectRoot "target"
$jarPath = Join-Path $targetDir $MainJar
$libDir = Join-Path $targetDir "lib"
$inputDir = Join-Path $projectRoot ".jpackage-input"
$destDir = Join-Path $projectRoot "dist"

if (-not (Test-Path $jarPath)) { throw "Main JAR not found: $jarPath" }
if (-not (Test-Path $libDir)) { throw "Dependency directory not found: $libDir" }
if (-not $env:JAVA_HOME) { throw "JAVA_HOME is not set. Set JAVA_HOME to your JDK 21+ installation." }
if (-not (Test-Path (Join-Path $env:JAVA_HOME "jmods"))) { throw "JAVA_HOME does not point to a full JDK (missing jmods): $env:JAVA_HOME" }
if (-not (Get-Command jpackage -ErrorAction SilentlyContinue)) { throw "jpackage is not available in PATH. Ensure %JAVA_HOME%\\bin is in PATH." }

$modulePath = "$($env:JAVA_HOME)\jmods;$libDir"

if (Test-Path $inputDir) { Remove-Item -Recurse -Force $inputDir }
New-Item -ItemType Directory -Path $inputDir | Out-Null
Copy-Item -Path $jarPath -Destination $inputDir
Get-ChildItem -Path $libDir -Filter "*.jar" -File | ForEach-Object {
    Copy-Item -Path $_.FullName -Destination $inputDir
}

if (-not (Test-Path $destDir)) {
    New-Item -ItemType Directory -Path $destDir | Out-Null
}

$outputArtifact = if ($Type -eq "exe") {
    Join-Path $destDir "$AppName-$AppVersion.exe"
} else {
    Join-Path $destDir $AppName
}

if (Test-Path $outputArtifact) {
    try {
        Remove-Item -Recurse -Force $outputArtifact -ErrorAction Stop
    } catch {
        throw "Cannot overwrite existing package output: $outputArtifact. Close any running installer/app from that path and retry."
    }
}

if ($Type -eq "exe") {
    $candle = Get-Command candle.exe -ErrorAction SilentlyContinue
    $light = Get-Command light.exe -ErrorAction SilentlyContinue
    if (-not ($candle -and $light)) {
        throw "WiX Toolset is required for --type exe. Install WiX v3+ and add candle.exe/light.exe to PATH."
    }
}

Write-Host "[2/4] Running jpackage..."
$jpackageArgs = @(
    "--type", $Type,
    "--name", $AppName,
    "--app-version", $AppVersion,
    "--dest", $destDir,
    "--input", $inputDir,
    "--module-path", $modulePath,
    "--main-jar", $MainJar,
    "--main-class", $MainClass,
    "--add-modules", $AddModules
)

if ($WinConsole) {
    $jpackageArgs += "--win-console"
}

if ($VerboseJPackage) {
    $jpackageArgs += "--verbose"
}

if ($EnableDebugLogs) {
    if (-not $WinConsole) {
        $jpackageArgs += "--win-console"
    }
    $JavaOptions += "-Dprism.verbose=true"
    $JavaOptions += "-Djavafx.verbose=true"
}

foreach ($javaOption in $JavaOptions) {
    if ($null -ne $javaOption -and -not [string]::IsNullOrWhiteSpace($javaOption)) {
        $jpackageArgs += "--java-options"
        $jpackageArgs += $javaOption.Trim()
    }
}

& jpackage @jpackageArgs
if ($LASTEXITCODE -ne 0) { throw "jpackage failed with exit code $LASTEXITCODE" }

Write-Host "[3/4] Package created successfully."
if ($Type -eq "exe") {
    Write-Host "[4/4] EXE installer path: $destDir\$AppName-$AppVersion.exe"
} else {
    Write-Host "[4/4] App image path: $destDir\$AppName"
}
