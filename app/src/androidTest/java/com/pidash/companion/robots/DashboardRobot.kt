package com.pidash.companion.robots

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assert
import com.pidash.companion.isGlowActive
import com.pidash.companion.isRotaryFocused
import com.pidash.companion.domain.model.SpotCategory
import com.pidash.companion.testing.AppComposeRule
import com.pidash.companion.testing.BaseRobot
import com.pidash.companion.ui.components.DashSemantics
import com.pidash.companion.ui.components.TestTags
import com.pidash.companion.ui.dashboard.DashboardActions
import com.pidash.companion.ui.dashboard.DashboardScreen
import com.pidash.companion.ui.dashboard.DashboardUiState

class DashboardRobot(rule: AppComposeRule) : BaseRobot(rule) {

    /** Render the screen. [state] is read in composition: back it with mutableStateOf or collectAsState(). */
    fun launch(actions: DashboardActions = DashboardActions(), state: @Composable () -> DashboardUiState) {
        rule.setContent { DashboardScreen(state(), actions) }
    }

    // ---- actions ----
    fun tapHome() = tap(TestTags.ACTION_HOME)
    fun tapEmergency() = tap(TestTags.ACTION_EMERGENCY)
    fun tapMute() = tap(TestTags.ACTION_MUTE)
    fun tapTheme() = tap(TestTags.ACTION_THEME)
    fun tapQuickStop(category: SpotCategory) = tap(TestTags.quickStop(category))

    // ---- checks ----
    fun assertSpeed(kmh: Int) {
        assertText(TestTags.SPEED_VALUE, kmh.toString())
        assertSemantics(TestTags.SPEED_GAUGE, DashSemantics.SpeedKmh, kmh)
        assertDescription(TestTags.SPEED_GAUGE, "Speed $kmh kilometers per hour")
    }

    fun assertSpeedAlert(alert: Boolean) = assertSemantics(TestTags.SPEED_GAUGE, DashSemantics.SpeedAlert, alert)

    fun assertSpeedLimit(limit: Int) = assertDescription(TestTags.SPEED_LIMIT, "Speed limit $limit")

    fun assertCriticalControlsDisplayed() {
        assertDisplayed(TestTags.ACTION_HOME, TestTags.ACTION_EMERGENCY, TestTags.ACTION_MUTE, TestTags.ACTION_THEME)
        assertDisplayed(*SpotCategory.entries.map(TestTags::quickStop).toTypedArray())
        assertDisplayed(TestTags.SPEED_GAUGE, TestTags.MINI_PLAYER)
    }

    fun assertWeather(temperature: String, location: String, condition: String) {
        assertText(TestTags.WEATHER_TEMPERATURE, temperature)
        assertText(TestTags.WEATHER_LOCATION, location)
        assertDescription(TestTags.WEATHER_ICON, condition, unmerged = true)
    }

    fun assertMuted(muted: Boolean) {
        assertDescription(TestTags.ACTION_MUTE, if (muted) "Unmute audio" else "Mute audio")
        assertStateDescription(TestTags.ACTION_MUTE, if (muted) "Muted" else "Sound on")
        node(TestTags.ACTION_MUTE).assert(isGlowActive(muted))
    }

    fun assertDarkTheme(dark: Boolean) {
        assertStateDescription(TestTags.ACTION_THEME, if (dark) "Night theme" else "Day theme")
        assertDescription(TestTags.ACTION_THEME, if (dark) "Switch to day theme" else "Switch to night theme")
    }

    fun assertRotaryFocus(tag: String, focused: Boolean) {
        if (focused) assertFocused(tag)
        node(tag).assert(isRotaryFocused(focused))
    }
}

fun AppComposeRule.dashboard(block: DashboardRobot.() -> Unit) {
    DashboardRobot(this).block()
}
