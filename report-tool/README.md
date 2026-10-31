# ATR — Android Test Runner (source)

A standalone command-line runner for Android instrumented tests with Robot Framework–style test
selection and a self-contained HTML report (per-test video, screenshots, logcat, retries/flaky).
It works with any app: it only needs `adb`, an app APK and its androidTest APK.

- **User guide** (what ships in the zip): [src/dist/README.md](src/dist/README.md)
- **Design and options considered**: [docs/DESIGN.md](docs/DESIGN.md)
- **Release package**: [release/atr-1.0.0.zip](release/atr-1.0.0.zip)

## Build, test, package

Requires JDK 17+. Run from this folder:

```bash
./gradlew test          # unit tests (parser, selection, CLI, config, reports)
./gradlew installDist   # runnable copy in build/install/atr/bin/atr
./gradlew distZip       # build/distributions/atr-<version>.zip  (the deliverable)
```

To release: bump `version` in `build.gradle.kts`, run `./gradlew clean test distZip`, and copy the zip
to `release/`.

## Source layout

| File | Responsibility |
|------|----------------|
| `Main.kt` | commands `run`, `list`, `report`, `init`, `version`, `help`; exit codes |
| `Cli.kt` | argument parsing, CLI → config overrides, help text, version |
| `Config.kt` | `atr.yml` loading (SnakeYAML), defaults, tag aliases |
| `Selection.kt` | `--test` / `--suite` glob matching, `--rerunfailed` |
| `Runner.kt` | device + install, discovery (`-e log true`), one `am instrument` per test, retries, artifact collection |
| `Adb.kt`, `Process.kt` | adb wrapper (install, shell, pull, logcat, instrumentations) and process execution with timeouts |
| `InstrumentationParser.kt` | parses `am instrument -r` status blocks into results |
| `ScrcpyRecorder.kt` | per-attempt video through scrcpy |
| `Console.kt` | Robot-style console output |
| `Reports.kt` | `junit.xml` and `report.html` (template + CSS + JS + data inlined) |
| `RobotXmlReport.kt` | Robot Framework `output.xml` (schema 5 or legacy 4) for `rebot` |
| `Model.kt` | serializable result model (`output.json`) |
| `resources/report/` | HTML template, CSS and JS of the report |
| `src/dist/` | files added to the zip: README, install scripts, example config |

## How a run works

1. Resolve the device and `adb`, install the APKs, find the instrumentation for `appPackage`.
2. Discover the tests with `am instrument -e log true` (plus `annotation`/`notAnnotation` for tags).
3. Filter by `--test` / `--suite` / `--rerunfailed`.
4. For each test and attempt: clear `pullPaths` and logcat → start scrcpy → `am instrument -e class C#m`
   → parse the status → stop the video → save logcat and the instrumentation output → pull device files.
5. Write `output.json`, `junit.xml` and `report.html`; the exit code is the number of failed tests.

## Verified

- Unit tests: 27 passing.
- Robot Framework 7.5: `rebot` reads both schemas; a merged run of ATR and Robot tests produced one report.html and log.html with embedded video and screenshots.
- Pi-Dash on a Raspberry Pi 4 (Android 16): Smoke suite 5/5 with video, screenshot and logcat per test;
  a demo suite covering FAIL, flaky (pass on retry), SKIP (assumption) and `--rerunfailed`.
- The zip installs and uninstalls with `install.ps1`.
