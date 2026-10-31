package com.pidash.companion.testing

import android.graphics.Bitmap
import android.util.Log
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onRoot
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.services.storage.TestStorage
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

private const val TAG = "ComposeTestScreenshots"

/** Folder inside the app's external files dir used when the test storage service is absent. */
const val DEVICE_SCREENSHOT_DIR = "test-screenshots"

private val runnerArgs get() = InstrumentationRegistry.getArguments()

/** True when the run passes `-e evidenceScreenshots true` (e.g. Marathon): capture passing tests too. */
val evidenceScreenshotsEnabled: Boolean get() = runnerArgs.getString("evidenceScreenshots") == "true"

/**
 * Where screenshots go:
 * - Gradle runs (`useTestStorageService=true`): [TestStorage]; Gradle pulls them to
 *   `build/outputs/connected_android_test_additional_output/<variant>/connected/<device>/<path>`.
 * - Other runners without the test-services APK (e.g. Marathon): the app's external files dir,
 *   `/sdcard/Android/data/<app>/files/test-screenshots/<path>`, which the runner can pull via adb.
 */
private fun openScreenshotOutput(path: String): OutputStream =
    if (runnerArgs.getString("useTestStorageService") == "true") {
        TestStorage().openOutputFile(path)
    } else {
        val root = InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(DEVICE_SCREENSHOT_DIR)
        val file = File(root, path).apply { parentFile?.mkdirs() }
        FileOutputStream(file)
    }

/** Saves the composed UI as a PNG at [path] (see [openScreenshotOutput] for the location). */
fun ComposeContentTestRule.saveScreenshot(path: String): Boolean = runCatching {
    val bitmap = onRoot().captureToImage().asAndroidBitmap()
    openScreenshotOutput(path).use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }
    Log.i(TAG, "Saved screenshot: $path")
}.onFailure { Log.w(TAG, "Could not save screenshot $path", it) }.isSuccess

/**
 * Saves the whole device screen (status bar, other apps, launcher) as a PNG at [path]. Used when the
 * app's UI no longer exists, e.g. a test failed because Back closed the app.
 */
fun saveDeviceScreenshot(path: String): Boolean = runCatching {
    val bitmap = checkNotNull(InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()) { "no device screenshot" }
    openScreenshotOutput(path).use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }
    Log.i(TAG, "Saved device screenshot: $path")
}.onFailure { Log.w(TAG, "Could not save device screenshot $path", it) }.isSuccess

/**
 * Captures `screenshots/<Class>_<method>.png` when a test fails, and — when
 * [evidenceScreenshotsEnabled] — `evidence/<Class>_<method>.png` (final screen) when it passes.
 * Must run *inside* the compose rule so the composition still exists; [AppComposeRule] chains it.
 */
class ScreenshotOnFailureRule(
    private val composeRule: ComposeContentTestRule,
    /** Runs before capturing (e.g. hold the final screen in observation mode). */
    private val beforeCapture: () -> Unit = {},
) : TestWatcher() {
    override fun failed(e: Throwable, description: Description) {
        runCatching(beforeCapture)
        val path = "screenshots/${description.fileName()}"
        // No composition left (app closed, activity gone): capture the device screen instead.
        if (!composeRule.saveScreenshot(path)) saveDeviceScreenshot(path)
    }

    override fun succeeded(description: Description) {
        runCatching(beforeCapture)
        if (evidenceScreenshotsEnabled) composeRule.saveScreenshot("evidence/${description.fileName()}")
    }

    private fun Description.fileName() = "${testClass.simpleName}_$methodName.png"
}
