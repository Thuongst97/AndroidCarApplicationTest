package com.pidash.companion.testing

import android.graphics.Bitmap
import android.util.Log
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onRoot
import androidx.test.services.storage.TestStorage
import org.junit.rules.TestWatcher
import org.junit.runner.Description

private const val TAG = "ComposeTestScreenshots"

/**
 * Saves the composed UI as a PNG through [TestStorage]. With the `useTestStorageService` runner
 * argument, Gradle pulls the file to
 * `build/outputs/connected_android_test_additional_output/<variant>/connected/<device>/<path>`.
 */
fun ComposeContentTestRule.saveScreenshot(path: String): Boolean = runCatching {
    val bitmap = onRoot().captureToImage().asAndroidBitmap()
    TestStorage().openOutputFile(path).use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }
    Log.i(TAG, "Saved screenshot: $path")
}.onFailure { Log.w(TAG, "Could not save screenshot $path", it) }.isSuccess

/**
 * Captures `screenshots/<Class>_<method>.png` when a test fails. Must run *inside* the compose rule
 * so the composition still exists; [AppComposeRule] chains it correctly.
 */
class ScreenshotOnFailureRule(private val composeRule: ComposeContentTestRule) : TestWatcher() {
    override fun failed(e: Throwable, description: Description) {
        composeRule.saveScreenshot("screenshots/${description.testClass.simpleName}_${description.methodName}.png")
    }
}
