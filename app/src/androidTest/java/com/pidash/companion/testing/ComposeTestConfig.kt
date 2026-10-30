package com.pidash.companion.testing

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Per-app settings for the generic Compose test framework (`testing` package).
 *
 * Everything in `testing/` is app-agnostic; to reuse it in another app, copy the package and
 * create one config describing that app's theme and UI standards.
 */
data class ComposeTestConfig(
    /** Minimum width/height for every clickable node (Material: 48dp, automotive: 64dp). */
    val minTouchTarget: Dp = 48.dp,
    /** Font scale used by default so assertions don't depend on the device setting. */
    val defaultFontScale: Float = 1f,
    /** Largest font scale the app supports; contract tests render at this scale. */
    val maxFontScale: Float = 1.3f,
    /** Wraps all test content in the app theme (and any background/providers it needs). */
    val themeWrapper: @Composable (darkTheme: Boolean, content: @Composable () -> Unit) -> Unit = { _, content -> content() },
)
