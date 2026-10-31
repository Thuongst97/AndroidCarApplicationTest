<#
.SYNOPSIS
  Runs the Compose UI tests with Marathon while recording the device screen, then embeds the
  video (one clip per test) and the tests' screenshots into Marathon's HTML report.

.DESCRIPTION
  Marathon's own video recording uses `screenrecord`, which fails on the Raspberry Pi (no hardware
  video encoder). This script records from the PC side with scrcpy using the Pi's *software* H.264
  encoder, runs Marathon, stops the recording cleanly, and calls the Gradle task
  `attachMarathonEvidence`, which gives each test page a clip of the run video
  (`run.mp4#t=<start>,<end>`) plus its evidence screenshot.

.EXAMPLE
  # 5 review tests
  .\scripts\marathon-with-video.ps1 -Marathonfile Marathonfile.review -Report app/build/reports/marathon-review
  # all tests
  .\scripts\marathon-with-video.ps1
#>
param(
    [string]$Marathonfile = "Marathonfile",
    [string]$Report = "app/build/reports/marathon",
    [string]$Serial = "192.168.1.140:35381",
    [string]$Marathon = "marathon",          # path to marathon(.bat) if it's not on PATH
    [switch]$SkipBuild
)
$ErrorActionPreference = "Stop"
Set-Location (Split-Path $PSScriptRoot -Parent)

if (-not $SkipBuild) {
    Write-Host "==> Building APKs"
    & .\gradlew :app:assembleDebug :app:assembleDebugAndroidTest -q
    if ($LASTEXITCODE -ne 0) { throw "Gradle build failed" }
}

# Work folder outside the report (Marathon rewrites the report folder).
$work = "app/build/marathon-video"
New-Item -ItemType Directory -Force $work | Out-Null
$video = Join-Path (Resolve-Path $work) "run.mp4"
if (Test-Path $video) { Remove-Item $video -Force }

# scrcpy must talk to the same adb server as Marathon.
$env:ADB = (Get-Command adb).Source

Write-Host "==> Starting screen recording (scrcpy, software H.264 encoder)"
$scrcpyArgs = @(
    "-s", $Serial,
    "--no-window", "--no-audio", "--no-control",
    "--video-codec=h264", "--video-encoder=c2.android.avc.encoder",   # Pi has no usable hw encoder
    "--max-fps=15", "--video-bit-rate=2M",
    "--record=`"$video`""
)
$scrcpy = Start-Process scrcpy -ArgumentList $scrcpyArgs -PassThru -WindowStyle Hidden `
    -RedirectStandardOutput "$work/scrcpy.out.log" -RedirectStandardError "$work/scrcpy.err.log"

# Wait until scrcpy reports the recording has started; that moment is t = 0 of the video.
$deadline = (Get-Date).AddSeconds(30)
while (-not (Select-String -Path "$work/scrcpy.out.log", "$work/scrcpy.err.log" -Pattern "Recording started" -Quiet -ErrorAction SilentlyContinue)) {
    if ((Get-Date) -gt $deadline -or $scrcpy.HasExited) { throw "scrcpy did not start recording; see $work/scrcpy.*.log" }
    Start-Sleep -Milliseconds 200
}
$videoStart = [DateTimeOffset]::Now.ToUnixTimeMilliseconds()

try {
    Write-Host "==> Running Marathon ($Marathonfile)"
    & $Marathon run -m $Marathonfile
    $marathonExit = $LASTEXITCODE
}
finally {
    Write-Host "==> Stopping recording"
    # Ending scrcpy's server on the device makes the client finish the MP4 file properly.
    adb -s $Serial shell "pkill -f com.genymobile.scrcpy.Server" | Out-Null
    if (-not $scrcpy.WaitForExit(20000)) { Stop-Process -Id $scrcpy.Id -Force }
}

# Store the video + its start time inside the report, then attach clips and screenshots.
$videoDir = Join-Path $Report "video"
New-Item -ItemType Directory -Force $videoDir | Out-Null
Copy-Item $video (Join-Path $videoDir "run.mp4") -Force
Set-Content (Join-Path $videoDir "run.start") $videoStart

Write-Host "==> Attaching video clips and screenshots to the report"
& .\gradlew :app:attachMarathonEvidence "-PmarathonReport=$Report" -q
Write-Host "Report: $(Resolve-Path (Join-Path $Report 'html/index.html'))"
exit $marathonExit
