# ATR — Android Test Runner

ATR runs the instrumented (Compose / Espresso) tests of **any Android app** one test at a time and
produces a professional report: per-test video, screenshots, logcat, error and stack trace, retries
and flaky detection. Test selection works like Robot Framework (`--test`, `--suite`, `--include`,
`--exclude`, `--rerunfailed`).

```text
atr run --include Smoke
==============================================================================
My app UI tests
Pixel 7 | Android 15 | emulator-5554 | 5 tests
==============================================================================
com.example.app.LoginTest
------------------------------------------------------------------------------
validCredentials_opensHome                                   | PASS  |    6.8s
wrongPassword_showsError                                     | FAIL  |    7.0s
    Failed to assert the following: (Text = [Wrong password])
...
==============================================================================
5 tests, 4 passed, 1 failed, 0 errors, 0 skipped   (41.2s)
==============================================================================
Report:  atr-results/report.html
```

## 1. Requirements

| Tool | Why | Check |
|------|-----|-------|
| Java 17+ | runs ATR | `java -version` |
| Android platform-tools (`adb`) | talks to the device | `adb version` (or set `ANDROID_HOME`) |
| scrcpy *(optional)* | per-test video | `scrcpy --version`; without it ATR runs and skips videos |

## 2. Install

Unzip `atr-<version>.zip` anywhere, then:

**Windows (PowerShell)**

```powershell
cd atr-1.0.0
powershell -ExecutionPolicy Bypass -File .\install.ps1     # copies to %LOCALAPPDATA%\atr and adds bin to PATH
# open a new terminal
atr version
```

**macOS / Linux**

```bash
cd atr-1.0.0
./install.sh            # copies to ~/.local/share/atr and links ~/.local/bin/atr
atr version
```

No install is needed to try it: run `bin/atr` (or `bin\atr.bat`) directly.
Uninstall: `install.ps1 -Uninstall` / `./install.sh --uninstall`.

## 3. Set up an app (5 minutes)

1. In the app project root: `atr init` creates a commented `atr.yml`.
2. Fill in at least `appPackage` and the APK paths (defaults match a standard Gradle project).
3. Build the APKs: `./gradlew assembleDebug assembleDebugAndroidTest`.
4. Connect the device (`adb devices`) and run: `atr run`.
5. Open `atr-results/report.html`.

See `atr.example.yml` for a complete, real configuration.

### Tags (`--include` / `--exclude`)

Tags are JUnit annotations. Declare one in the androidTest sources:

```kotlin
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
annotation class Smoke
```

Put `@Smoke` on tests and map the short name in `atr.yml`:

```yaml
tags:
  Smoke: com.example.app.Smoke
```

Built-in filters such as `androidx.test.filters.LargeTest` work the same way.

### Screenshots in the report

ATR copies the folders listed under `artifacts.pullPaths` from the device after every test, and
images in them appear in the report. Let the tests save screenshots into the app's external files
dir, e.g. `/sdcard/Android/data/<appPackage>/files/test-screenshots`. ATR empties those folders
before each test, so every test gets only its own images.

### Readable videos

Compose tests run faster than a human can follow. If the tests support it, pass a pause through
instrumentation arguments (the test code reads them with `InstrumentationRegistry.getArguments()`):

```yaml
instrumentationArgs:
  observeStepMs: "1500"
```

## 4. Command reference

```text
atr run    [options]                 run tests
atr list   [options]                 list the tests that would run (dry run)
atr report -I output.json [-d DIR]   regenerate report.html and junit.xml
atr init   [atr.yml]                 write a commented configuration template
atr version | atr help
```

| Option | Meaning |
|--------|---------|
| `-t, --test PATTERN` | method, `Class#method` or `pkg.Class#method`; `*` and `?` allowed; repeatable |
| `-s, --suite PATTERN` | test class (simple or fully-qualified name) |
| `-i, --include TAG` / `-e, --exclude TAG` | tag alias from `atr.yml` or annotation FQN |
| `-R, --rerunfailed FILE` | only the tests that failed in a previous `output.json` |
| `-c, --config FILE` | config file (default `./atr.yml`) |
| `--serial SERIAL` | device (default: config, or the only connected device) |
| `--retries N` | extra attempts; a test passing on retry is marked **FLAKY** |
| `--timeout SECONDS` | per-test timeout |
| `--video always\|on-failure\|never` | video policy |
| `-a, --arg KEY=VALUE` | extra instrumentation argument |
| `--no-install` | do not install the APKs |
| `-d, --outputdir DIR` | results folder |
| `-N, --name NAME` | run name in the report |

Examples:

```bash
atr run -t "LoginTest#wrongPassword*"           # one test, like robot --test
atr run -s LoginTest -s CartTest                # two classes
atr run -i Smoke -e Slow --retries 1
atr run -R atr-results/output.json -d atr-results/rerun
atr list -i Smoke
```

**Exit code:** `0` all passed, `1..250` number of failed tests, `252` invalid usage, `255` error —
ready for CI. Publish `junit.xml` to the CI's test view and archive the whole results folder.

## 5. Robot Framework integration (one report for everything)

If the project's other tests run with Robot Framework, let Robot build the report. ATR writes its
results as a Robot `output.xml`, and `rebot` merges it with the Robot runs:

```bash
atr run -i Smoke --robot results/android.xml                        # 1. Android UI tests
robot --outputdir results --output robot-tests.xml --report NONE --log NONE robot-tests/   # 2. Robot tests
rebot --name "My project" --outputdir results results/robot-tests.xml results/android.xml   # 3. merge
```

`results/report.html` and `results/log.html` then contain every test. Each Android test maps as follows:

| ATR | Robot Framework |
|-----|-----------------|
| run (`name` in atr.yml) | top-level suite with metadata: device, Android, serial, app package, instrumentation, command, link to ATR's own report |
| test class | suite (documentation = fully-qualified class name) |
| test method | test (documentation = `Class#method`) |
| tag aliases (`tags:` in atr.yml) | Robot tags, so `rebot --include Smoke` and tag statistics work |
| flaky (passed on retry) | PASS with the tag `flaky` and the message "Passed on attempt n of n (flaky)" |
| ERROR (crash, timeout) | FAIL with the message prefix `ERROR:` (Robot has no ERROR status) |
| SKIP (assumption) | SKIP |
| each attempt | keyword `ATR.Run Instrumentation Test` with embedded video and screenshots, links to logcat and the instrumentation output, the stack trace, and the FAIL message |

Things to know:

- **Keep `log.html` in the same folder as the `--robot` file.** Video and screenshot paths are relative
  to that folder (for example `../atr-results/artifacts/...`). When publishing, keep the results folder and
  the ATR output folder side by side.
- `--robot-legacy` writes the Robot Framework 6 format (schema 4); the default is RF 7 (schema 5).
- `atr report -I atr-results/output.json --robot results/android.xml` converts an earlier run.
- Instead of the CLI flag you can set it in the config: `output.robot: results/android.xml`.
- The `rebot` exit code is the number of failed tests across both runs, so CI can use it directly.

## 6. Results folder

```text
atr-results/
├── report.html     open in a browser (keep it next to artifacts/)
├── output.json     machine-readable results; input for --rerunfailed and atr report
├── junit.xml       for CI (Jenkins, GitHub Actions, GitLab, Azure DevOps)
└── artifacts/t001-<Class>-<method>/a1/
    ├── video.mp4  logcat.txt  instrumentation.txt
    └── files/…    pulled from the device
```

The report links straight to a test: `report.html#t=com.example.app.LoginTest%23wrongPassword_showsError`.

## 7. Troubleshooting

| Symptom | Fix |
|---------|-----|
| `adb not found` | install platform-tools; add to PATH or set `ANDROID_HOME` |
| `Several devices connected` | `--serial` or `device.serial` |
| `No test instrumentation found` | install the test APK, or set `appPackage` / `instrumentation` |
| `videos disabled` | install scrcpy or set `scrcpy.path`; on devices without a HW encoder add `--video-encoder=c2.android.avc.encoder` |
| Video not recorded on a test | the device encoder refused; try another `--video-encoder` (`scrcpy --list-encoders`) |
| `Could not pull …` | the folder does not exist yet (no screenshots) or the path is too long on Windows; use a shorter `-d` |
| Test killed after N s | raise `execution.timeoutSeconds` / `--timeout` |
