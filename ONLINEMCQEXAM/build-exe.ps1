param(
    [string]$JdkPath = "C:\Program Files\Java\jdk-21.0.10",
    [ValidateSet("auto", "installer", "portable")]
    [string]$Mode = "auto"
)

$ErrorActionPreference = "Stop"

if (-not (Test-Path -Path ".\mvnw.cmd")) {
    throw "Run this script from the ONLINEMCQEXAM directory."
}

if (-not (Test-Path -Path $JdkPath)) {
    throw "JDK path not found: $JdkPath"
}

$env:JAVA_HOME = $JdkPath
$env:Path = "$env:JAVA_HOME\bin;$env:Path"

Write-Host "Building shaded JAR..."
.\mvnw.cmd clean package

if (Test-Path ".\dist") {
    Remove-Item -Recurse -Force ".\dist"
}

function New-Installer {
    Write-Host "Creating Windows EXE installer..."
    jpackage `
      --type exe `
      --name "OnlineMCQExam" `
      --input "target" `
      --main-jar "javafx-Project-1.0.jar" `
      --main-class "com.example.onlinemcqexam.Launcher" `
      --win-shortcut `
      --win-menu `
      --dest "dist"
    Write-Host "Installer created in .\dist"
}

function New-PortableApp {
    Write-Host "Creating portable EXE app image..."
    jpackage `
      --type app-image `
      --name "OnlineMCQExam" `
      --input "target" `
      --main-jar "javafx-Project-1.0.jar" `
      --main-class "com.example.onlinemcqexam.Launcher" `
      --dest "dist"
    Write-Host "Portable app created: .\dist\OnlineMCQExam\OnlineMCQExam.exe"
}

$hasWix = (Get-Command light.exe -ErrorAction SilentlyContinue) -and (Get-Command candle.exe -ErrorAction SilentlyContinue)

switch ($Mode) {
    "installer" {
        if (-not $hasWix) {
            throw "WiX tools not found (light.exe/candle.exe). Install WiX or run with -Mode portable."
        }
        New-Installer
    }
    "portable" {
        New-PortableApp
    }
    default {
        if ($hasWix) {
            New-Installer
        } else {
            Write-Host "WiX tools not found. Falling back to portable EXE app image."
            New-PortableApp
        }
    }
}

Write-Host "Done. Output folder: .\dist"
