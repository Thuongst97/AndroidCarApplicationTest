<#
.SYNOPSIS
  Installs ATR (Android Test Runner) for the current user.
.DESCRIPTION
  Copies this distribution to %LOCALAPPDATA%\atr and adds its bin folder to the user PATH.
  Run from the unzipped atr-<version> folder:  powershell -ExecutionPolicy Bypass -File .\install.ps1
.PARAMETER Dir
  Install folder (default: %LOCALAPPDATA%\atr).
.PARAMETER Uninstall
  Removes the install folder and the PATH entry.
#>
param(
    [string]$Dir = (Join-Path $env:LOCALAPPDATA "atr"),
    [switch]$Uninstall
)
$ErrorActionPreference = "Stop"
$bin = Join-Path $Dir "bin"

function Set-UserPath([string[]]$entries) {
    [Environment]::SetEnvironmentVariable("Path", ($entries -join ";"), "User")
}
$userPath = @(([Environment]::GetEnvironmentVariable("Path", "User") -split ";") | Where-Object { $_ })

if ($Uninstall) {
    if (Test-Path $Dir) { Remove-Item -Recurse -Force $Dir }
    Set-UserPath ($userPath | Where-Object { $_ -ne $bin })
    Write-Host "ATR removed from $Dir"
    exit 0
}

$source = $PSScriptRoot
if (-not (Test-Path (Join-Path $source "bin\atr.bat"))) { throw "Run this script from the unzipped ATR folder (bin\atr.bat not found)." }

if (-not (Get-Command java -ErrorAction SilentlyContinue) -and -not $env:JAVA_HOME) {
    Write-Warning "Java 17+ not found (java on PATH or JAVA_HOME). ATR needs it to run."
}
if (-not (Get-Command adb -ErrorAction SilentlyContinue) -and -not $env:ANDROID_HOME) {
    Write-Warning "adb not found (PATH or ANDROID_HOME). Install Android platform-tools."
}
if (-not (Get-Command scrcpy -ErrorAction SilentlyContinue)) {
    Write-Warning "scrcpy not found: ATR will run without videos (or set scrcpy.path in atr.yml)."
}

if ((Resolve-Path $source).Path -ne [IO.Path]::GetFullPath($Dir)) {
    if (Test-Path $Dir) { Remove-Item -Recurse -Force $Dir }
    New-Item -ItemType Directory -Force $Dir | Out-Null
    Copy-Item -Recurse -Force (Join-Path $source "*") $Dir
}
if ($userPath -notcontains $bin) { Set-UserPath ($userPath + $bin) }

Write-Host "ATR installed to $Dir"
Write-Host "Open a new terminal and run:  atr version"
