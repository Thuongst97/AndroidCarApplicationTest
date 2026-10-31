<#
.SYNOPSIS
  Runs all Pi-Dash tests and builds ONE Robot Framework report.
.DESCRIPTION
  1. ATR runs the Compose UI tests on the device (video, screenshots, logcat per test)
     and writes them as a Robot result file: results/android.xml
  2. Robot Framework runs the project's .robot suites: results/robot-tests.xml
  3. rebot merges both into results/report.html + results/log.html (+ results/output.xml)
  Paths in the log point to atr-results/artifacts, so keep results/ and atr-results/ side by side
  when publishing (CI: archive both folders).
.EXAMPLE
  .\scripts\run-all-tests.ps1                     # Smoke UI tests + Robot suites
  .\scripts\run-all-tests.ps1 -AtrArgs @()        # all UI tests
  .\scripts\run-all-tests.ps1 -AtrArgs @("-t", "*mute*")
#>
param(
    [string[]]$AtrArgs = @("--include", "Smoke"),
    [string]$Atr = "report-tool\build\install\atr\bin\atr.bat",
    [string]$Python = "python",
    [string]$Name = "Pi-Dash"
)
$ErrorActionPreference = "Stop"
Set-Location (Split-Path $PSScriptRoot -Parent)
$env:PYTHONIOENCODING = "utf-8"
if (-not (Get-Command $Atr -ErrorAction SilentlyContinue)) { $Atr = "atr" }   # installed from the zip

Remove-Item -Recurse -Force results -ErrorAction SilentlyContinue
New-Item -ItemType Directory results | Out-Null

# The Robot suites target the same device as atr.yml
$serial = (Select-String -Path atr.yml -Pattern '^\s*serial:\s*"?([^"#\s]+)' | Select-Object -First 1).Matches.Groups[1].Value
if ($serial) { $env:ANDROID_SERIAL = $serial }

Write-Host "== 1/3 Compose UI tests (ATR) ==" -ForegroundColor Cyan
& $Atr run @AtrArgs --robot results/android.xml
$atrRc = $LASTEXITCODE

Write-Host "== 2/3 Robot Framework suites ==" -ForegroundColor Cyan
& $Python -m robot --outputdir results --output robot-tests.xml --report NONE --log NONE robot-tests
$robotRc = $LASTEXITCODE

Write-Host "== 3/3 Merge (rebot) ==" -ForegroundColor Cyan
$inputs = @("results/robot-tests.xml", "results/android.xml") | Where-Object { Test-Path $_ }
& $Python -m robot.rebot --name $Name --outputdir results --output output.xml `
    --tagstatexclude robot --tagstatexclude device $inputs
$rc = $LASTEXITCODE

Write-Host "ATR exit $atrRc, Robot exit $robotRc, combined failures $rc"
Write-Host "Report: $(Resolve-Path results/report.html)"
exit $rc
