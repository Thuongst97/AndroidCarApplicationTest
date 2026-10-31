*** Settings ***
Documentation     Head-unit checks written in Robot Framework. They stand for the project's other
...               Robot suites: scripts/run-all-tests.ps1 runs them next to the Compose UI tests
...               (executed by ATR) and rebot merges both into one report.
Library           Process
Library           String
Force Tags        robot    device

*** Variables ***
${ADB}            adb
${SERIAL}         %{ANDROID_SERIAL=}
${APP}            com.pidash.companion

*** Test Cases ***
Head Unit Is Online
    [Documentation]    The device answers adb.
    ${state}=    Adb    get-state
    Should Be Equal    ${state}    device

App Is Installed
    [Documentation]    The Pi-Dash package is on the device.
    ${packages}=    Adb    shell    pm    list    packages    ${APP}
    Should Contain    ${packages}    package:${APP}

Display Is 800x480 Landscape
    [Documentation]    The UI is designed for an 800x480 head-unit panel.
    ${size}=    Adb    shell    wm    size
    Should Match Regexp    ${size}    (Physical|Override) size: 800x480

Runs Android 16
    [Documentation]    Target platform of the build.
    ${release}=    Adb    shell    getprop    ro.build.version.release
    Should Be Equal    ${release}    16

*** Keywords ***
Adb
    [Documentation]    Runs adb against ${SERIAL} (or the only device) and returns stdout.
    [Arguments]    @{args}
    ${target}=    IF    $SERIAL    Create List    -s    ${SERIAL}    ELSE    Create List
    ${result}=    Run Process    ${ADB}    @{target}    @{args}
    Should Be Equal As Integers    ${result.rc}    0    adb failed: ${result.stderr}
    ${out}=    Strip String    ${result.stdout}
    RETURN    ${out}
