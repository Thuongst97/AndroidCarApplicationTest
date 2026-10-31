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
import androidx.test.platform.app.InstrumentationRegistry
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

    /**
     * Observation mode for recorded runs (e.g. Marathon + video). Off by default; enabled with the
     * runner arguments `observeStepMs` (real-time play after each step) and `observeHoldMs` (hold
     * the final screen). Normal runs keep the fast virtual clock.
     */
    val observeStepMs: Long = runnerArg("observeStepMs")
    val observeHoldMs: Long = runnerArg("observeHoldMs")

    // Hold the final screen before the evidence/failure screenshot and teardown.
    private val chain: RuleChain = RuleChain.outerRule(delegate)
        .around(ScreenshotOnFailureRule(delegate, beforeCapture = { observe(observeHoldMs) }))

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
        observe() // let the first screen (and its intro animations) be seen
    }

    /**
     * In observation mode, plays the UI in **real time** for [ms]: the virtual test clock is
     * advanced one frame (16 ms) at a time with a real 16 ms sleep, so animations started by the
     * previous action run at their true speed on the device (visible on screen recordings).
     * No-op when observation mode is off. Robots call this after every action; tests can call it
     * after direct state changes they want to be visible.
     */
    fun observe(ms: Long = observeStepMs) {
        if (ms <= 0) return
        repeat((ms / FRAME_MS).toInt()) {
            delegate.mainClock.advanceTimeByFrame()
            Thread.sleep(FRAME_MS)
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

private const val FRAME_MS = 16L

private fun runnerArg(name: String): Long =
    InstrumentationRegistry.getArguments().getString(name)?.toLongOrNull() ?: 0L

fun createAppComposeRule(config: ComposeTestConfig): AppComposeRule = AppComposeRule(config)
