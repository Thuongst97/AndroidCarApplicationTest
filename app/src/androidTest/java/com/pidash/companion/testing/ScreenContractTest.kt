package com.pidash.companion.testing

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onRoot
import org.junit.Rule
import org.junit.Test

/**
 * Template-method base class: every screen gets the app's UI standards checked for free.
 *
 * ```
 * class SettingsScreenContractTest : MyAppScreenContract() {
 *     @Composable override fun Content() = SettingsScreen(sampleState, {})
 * }
 * ```
 *
 * Each contract test also saves a screenshot to `screens/<Class>_<variant>.png`, giving a visual
 * gallery of every screen in both themes after each run.
 */
abstract class ScreenContractTest(config: ComposeTestConfig) {

    @get:Rule
    val rule = createAppComposeRule(config)

    /** The screen under test, rendered with representative sample data. */
    @Composable
    abstract fun Content()

    private val screenName: String get() = this::class.java.simpleName.removeSuffix("ContractTest")

    @Test
    fun contract_rendersInDarkTheme() {
        rule.setContent(darkTheme = true) { Content() }
        rule.onRoot().assertIsDisplayed()
        rule.saveScreenshot("screens/${screenName}_dark.png")
    }

    @Test
    fun contract_rendersInLightTheme() {
        rule.setContent(darkTheme = false) { Content() }
        rule.onRoot().assertIsDisplayed()
        rule.saveScreenshot("screens/${screenName}_light.png")
    }

    @Test
    fun contract_rendersAtMaxFontScale() {
        rule.setContent(darkTheme = true, fontScale = rule.config.maxFontScale) { Content() }
        UiStandards.assertClickablesHaveLabels(rule, screenName)
        rule.saveScreenshot("screens/${screenName}_maxFont.png")
    }

    @Test
    fun contract_clickablesHaveAccessibleLabels() {
        rule.setContent { Content() }
        UiStandards.assertClickablesHaveLabels(rule, screenName)
    }

    @Test
    fun contract_clickablesMeetMinimumTouchTarget() {
        rule.setContent { Content() }
        UiStandards.assertClickablesMeetTouchTarget(rule, screen = screenName)
    }

    @Test
    fun contract_clickablesReachableByRotary() {
        rule.setContent { Content() }
        rule.enableRotaryInput()
        UiStandards.assertClickablesReachableByRotary(rule, screenName)
    }
}
