@echo off
rem ============================================================================================
rem  Runs the Pi-Dash Compose UI tests directly with Gradle (connectedDebugAndroidTest)
rem  and copies the results to test-run-report\.
rem
rem  Usage:
rem    run_composet_test.bat                         run all tests
rem    run_composet_test.bat all                     run all tests
rem    run_composet_test.bat test NAME [NAME ...]    run specific tests; NAME is
rem                                                    Class#method   one test case
rem                                                    Class          all tests of a class
rem                                                  (package com.pidash.companion is added when omitted)
rem    run_composet_test.bat tag Smoke               run tests annotated with @Smoke
rem
rem  Options:
rem    --serial SERIAL   device to use (default: %%ANDROID_SERIAL%% or DEFAULT_SERIAL below)
rem    --open            open the HTML report when done
rem
rem  Examples:
rem    run_composet_test.bat test NavigationTest#railReturnsToDashboard_afterQuickStopOpenedSpots
rem    run_composet_test.bat test DashboardScreenTest MediaCompanionTest#playPause_tapped_togglesStateAndCallsViewModel
rem    run_composet_test.bat tag Smoke --open
rem
rem  Exit code: 0 = all tests passed, non-zero = failures or build error.
rem ============================================================================================
setlocal EnableExtensions

rem Capture before parsing: SHIFT also shifts %0.
set "SELF=%~nx0"
set "ROOT=%~dp0.."

set "PKG=com.pidash.companion"
set "DEFAULT_SERIAL=192.168.1.140:35381"
set "MODE=all"
set "CLASSES="
set "TAG="
set "OPEN="
set "SERIAL=%ANDROID_SERIAL%"
if not defined SERIAL set "SERIAL=%DEFAULT_SERIAL%"

:parse
if "%~1"=="" goto run
if /i "%~1"=="all"      (set "MODE=all"  & shift & goto parse)
if /i "%~1"=="test"     (set "MODE=test" & shift & goto parse)
if /i "%~1"=="tag" if "%~2"=="" goto err_tag
if /i "%~1"=="tag"      (set "MODE=tag" & set "TAG=%~2" & shift & shift & goto parse)
if /i "%~1"=="--serial" if "%~2"=="" goto err_serial
if /i "%~1"=="--serial" (set "SERIAL=%~2" & shift & shift & goto parse)
if /i "%~1"=="--open"   (set "OPEN=1" & shift & goto parse)
if /i "%~1"=="help"     goto usage
if /i "%~1"=="-h"       goto usage
if /i "%~1"=="--help"   goto usage
if /i "%~1"=="/?"       goto usage
if /i "%MODE%"=="test"  (call :addtest "%~1" & shift & goto parse)
echo [ERROR] Unknown argument "%~1". Run: %SELF% help
exit /b 2

:err_tag
echo [ERROR] tag needs a name, e.g. tag Smoke
exit /b 2

:err_serial
echo [ERROR] --serial needs a value
exit /b 2

:err_notest
echo [ERROR] test needs at least one Class or Class#method
exit /b 2

:addtest
rem Adds one Class or Class#method to CLASSES; prefixes the package for simple class names.
set "ITEM=%~1"
set "CLS=%ITEM%"
for /f "tokens=1 delims=#" %%c in ("%ITEM%") do set "CLS=%%c"
rem No dot in the class part = simple name (string test; does not depend on find.exe on PATH)
if "%CLS:.=%"=="%CLS%" set "ITEM=%PKG%.%ITEM%"
if defined CLASSES (set "CLASSES=%CLASSES%,%ITEM%") else (set "CLASSES=%ITEM%")
exit /b 0

:settag
rem Simple tag name (Smoke) -> annotation class in the app package.
if "%TAG:.=%"=="%TAG%" set "TAG=%PKG%.%TAG%"
set "FILTER=-Pandroid.testInstrumentationRunnerArguments.annotation=%TAG%"
exit /b 0

:run
set "FILTER="
if /i "%MODE%"=="test" if not defined CLASSES goto err_notest
if /i "%MODE%"=="test" (
    set "FILTER=-Pandroid.testInstrumentationRunnerArguments.class=%CLASSES%"
)
if /i "%MODE%"=="tag" call :settag

pushd "%ROOT%"
set "ROOT=%CD%"
set "REPORT=%ROOT%\test-run-report"

rem Gradle runs on every connected device; ANDROID_SERIAL limits it to one.
adb -s %SERIAL% get-state >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Device %SERIAL% is not connected. Check "adb devices" or use --serial.
    popd & exit /b 3
)
set "ANDROID_SERIAL=%SERIAL%"

echo ==============================================================================
echo  Pi-Dash Compose UI tests (direct Gradle run)
echo  Device : %SERIAL%
if /i "%MODE%"=="all"  echo  Tests  : all
if /i "%MODE%"=="test" echo  Tests  : %CLASSES%
if /i "%MODE%"=="tag"  echo  Tests  : tag %TAG%
echo  Report : %REPORT%
echo ==============================================================================

rem Remove results of earlier runs so a failed build never shows a stale report.
if exist "app\build\reports\androidTests\connected" rd /s /q "app\build\reports\androidTests\connected"
if exist "app\build\outputs\androidTest-results\connected" rd /s /q "app\build\outputs\androidTest-results\connected"
if exist "app\build\outputs\code_coverage\debugAndroidTest\connected" rd /s /q "app\build\outputs\code_coverage\debugAndroidTest\connected"
if exist "app\build\outputs\connected_android_test_additional_output" rd /s /q "app\build\outputs\connected_android_test_additional_output"

rem Full path: cmd may not search the current folder (NoDefaultCurrentDirectoryInExePath).
if defined FILTER (
    call "%ROOT%\gradlew.bat" :app:connectedDebugAndroidTest --console=plain "%FILTER%"
) else (
    call "%ROOT%\gradlew.bat" :app:connectedDebugAndroidTest --console=plain
)
set "RC=%ERRORLEVEL%"

rem Copy results (also after failures: the report shows what failed)
if exist "%REPORT%" rd /s /q "%REPORT%"
mkdir "%REPORT%\html" "%REPORT%\results" "%REPORT%\coverage" "%REPORT%\device-files" >nul
rem html\ includes html\screenshots\ (failure screenshots, shown under each failed test)
xcopy /e /i /q /y "app\build\reports\androidTests\connected\debug" "%REPORT%\html" >nul 2>&1
rem All files the tests saved on the device (failure and theme screenshots)
xcopy /e /i /q /y "app\build\outputs\connected_android_test_additional_output\debugAndroidTest\connected" "%REPORT%\device-files" >nul 2>&1
xcopy /e /i /q /y "app\build\outputs\androidTest-results\connected\debug" "%REPORT%\results" >nul 2>&1
xcopy /e /i /q /y "app\build\outputs\code_coverage\debugAndroidTest\connected" "%REPORT%\coverage" >nul 2>&1

rem A name that matches nothing runs 0 tests and Gradle still succeeds: treat that as an error.
if "%RC%"=="0" (
    findstr /c:"<testcase" "%REPORT%\results\*.xml" >nul 2>&1 || set "RC=4"
)

echo.
if "%RC%"=="4" (echo RESULT : NO TESTS RAN - check the class/method names ^(exit code 4^)) else if "%RC%"=="0" (echo RESULT : ALL TESTS PASSED) else (echo RESULT : FAILED ^(exit code %RC%^))
if exist "%REPORT%\html\index.html" (
    echo Report : %REPORT%\html\index.html
    if defined OPEN start "" "%REPORT%\html\index.html"
) else (
    echo Report : not generated - see the Gradle output above
)
popd
exit /b %RC%

:usage
echo Runs the Pi-Dash Compose UI tests directly with Gradle; results go to the test-run-report folder.
echo.
echo Usage:
echo   %SELF%                          run all tests
echo   %SELF% all                      run all tests
echo   %SELF% test NAME [NAME ...]     run specific tests; NAME is Class#method or Class
echo                                                  (package %PKG% is added when omitted)
echo   %SELF% tag Smoke                run tests annotated with @Smoke
echo.
echo Options:
echo   --serial SERIAL   device to use (default: ANDROID_SERIAL or %DEFAULT_SERIAL%)
echo   --open            open the HTML report when done
echo.
echo Examples:
echo   %SELF% test NavigationTest#railReturnsToDashboard_afterQuickStopOpenedSpots
echo   %SELF% test DashboardScreenTest MediaCompanionTest#playPause_tapped_togglesStateAndCallsViewModel
echo   %SELF% tag Smoke --open
exit /b 0
