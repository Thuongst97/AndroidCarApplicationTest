package com.pidash.companion.testing

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import org.junit.rules.RuleChain
import org.junit.runner.Description
import org.junit.runners.model.Statement
import kotlin.math.abs

/**
 * The one rule every Compose UI test uses. It is a full [ComposeContentTestRule] (all finders,
 * `runOnIdle`, `mainClock`, … work as usual) plus:
 *
 * - **Themed content:** `setContent { }` wraps content in the app theme at a fixed font scale.
 * - **Screenshot on failure:** chained inside the compose rule automatically.
 * - **Rotary/D-pad helpers:** [enableRotaryInput], [rotate] — no `lateinit` plumbing in tests.
 *
 * ```
 * @get:Rule val rule = createAppComposeRule(MyAppTestConfig)
 * ```
 */
@OptIn(ExperimentalTestApi::class)
class AppComposeRule(
    val config: ComposeTestConfig,
    // Still the v1 rule (UnconfinedTestDispatcher). Migrating to junit4.v2.createComposeRule
    // (StandardTestDispatcher) is a one-line change here once the suite is verified against it.
    @Suppress("DEPRECATION") private val delegate: ComposeContentTestRule = createComposeRule(),
) : ComposeContentTestRule by delegate {

    private val chain: RuleChain = RuleChain.outerRule(delegate).around(ScreenshotOnFailureRule(delegate))

    private var focusManager: FocusManager? = null
    private var inputModeManager: InputModeManager? = null

    override fun apply(base: Statement, description: Description): Statement = chain.apply(base, description)

    /** Themed (dark), default font scale. */
    override fun setContent(composable: @Composable () -> Unit) = setContent(darkTheme = true, content = composable)

    fun setContent(
        darkTheme: Boolean,
        fontScale: Float = config.defaultFontScale,
        content: @Composable () -> Unit,
    ) {
        delegate.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                focusManager = LocalFocusManager.current
                inputModeManager = LocalInputModeManager.current
                config.themeWrapper(darkTheme, content)
            }
        }
    }

    /** Raw content without theme/font-scale wrapping (rarely needed). */
    fun setRawContent(content: @Composable () -> Unit) = delegate.setContent(content)

    /**
     * Switch to keyboard input mode, as real rotary/D-pad hardware does. In touch mode ordinary
     * clickables are not focusable, so call this before any focus assertion.
     */
    @OptIn(ExperimentalComposeUiApi::class)
    fun enableRotaryInput() {
        runOnIdle { requireNotNull(inputModeManager) { "Call setContent first" }.requestInputMode(InputMode.Keyboard) }
    }

    /** Simulate rotary detents: positive = clockwise = next focusable, negative = previous. */
    fun rotate(detents: Int = 1) {
        val manager = requireNotNull(focusManager) { "Call setContent first" }
        repeat(abs(detents)) {
            runOnIdle { manager.moveFocus(if (detents > 0) FocusDirection.Next else FocusDirection.Previous) }
        }
    }
}

fun createAppComposeRule(config: ComposeTestConfig): AppComposeRule = AppComposeRule(config)
