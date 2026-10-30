package com.pidash.companion.robots

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.bottomLeft
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.espresso.Espresso
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import com.pidash.companion.domain.model.Weather
import com.pidash.companion.testing.AppComposeRule
import com.pidash.companion.testing.BaseRobot
import com.pidash.companion.ui.clock.LuxuryClockOverlay
import com.pidash.companion.ui.clock.RailClock
import com.pidash.companion.ui.clock.TimeSource
import com.pidash.companion.ui.clock.rememberLuxuryClockState
import com.pidash.companion.ui.components.DashSemantics
import com.pidash.companion.ui.components.TestTags
import org.junit.Assert.assertEquals

class ClockRobot(rule: AppComposeRule) : BaseRobot(rule) {

    /** Same wiring as PiDashApp: rail clock + full-screen overlay sharing one LuxuryClockState. */
    fun launch(timeSource: TimeSource, weather: Weather? = null) {
        rule.setContent {
            val state = rememberLuxuryClockState()
            Box(Modifier.fillMaxSize()) {
                RailClock(
                    onClick = state::open,
                    onPositioned = { state.origin = it },
                    timeSource = timeSource,
                    modifier = Modifier.padding(start = 12.dp, top = 20.dp),
                )
                LuxuryClockOverlay(state, weather = weather, timeSource = timeSource)
            }
        }
    }

    // ---- actions ----
    fun tapRailClock() = tap(TestTags.NAV_CLOCK)
    fun tapClose() = tap(TestTags.LUXURY_CLOCK_CLOSE)
    fun tapBackground() = node(TestTags.LUXURY_CLOCK).performTouchInput { click(bottomLeft + androidx.compose.ui.geometry.Offset(40f, -40f)) }
    fun pressBack() = Espresso.pressBack()
    fun swipeUp() = node(TestTags.LUXURY_CLOCK_PAGER).performTouchInput { swipeUp() }
    fun swipeDown() = node(TestTags.LUXURY_CLOCK_PAGER).performTouchInput { swipeDown() }

    // ---- checks ----
    fun assertRailClock(time: String, date: String) {
        node(TestTags.NAV_CLOCK).assertTextEquals(time, date)
    }

    fun assertClosed() = assertNotPresent(TestTags.LUXURY_CLOCK)

    /** Open and covering the whole screen. */
    fun assertOpenFullScreen() {
        assertDisplayed(TestTags.LUXURY_CLOCK, TestTags.LUXURY_CLOCK_TIME)
        val root = rule.onRoot().fetchSemanticsNode().size
        val overlay = node(TestTags.LUXURY_CLOCK).fetchSemanticsNode().size
        assertEquals("Overlay should fill the screen", root, overlay)
        assertEquals(1f, revealFraction(), 0.001f)
    }

    fun revealFraction(): Float =
        node(TestTags.LUXURY_CLOCK).fetchSemanticsNode().config.getOrNull(DashSemantics.RevealFraction) ?: 0f

    fun assertTime(hhmm: String, seconds: String, date: String) {
        assertText(TestTags.LUXURY_CLOCK_TIME, hhmm)
        assertDescription(TestTags.LUXURY_CLOCK_TIME, "Time $hhmm")
        assertText(TestTags.LUXURY_CLOCK_SECONDS, seconds)
        assertText(TestTags.LUXURY_CLOCK_DATE, date)
    }

    fun assertControlsVisible(visible: Boolean) {
        if (visible) assertDisplayed(TestTags.LUXURY_CLOCK_CLOSE, TestTags.CLOCK_PAGE_INDICATOR) else assertNotPresent(TestTags.LUXURY_CLOCK_CLOSE)
    }

    /** Page [pageTag] is on screen and the indicator says "Clock n of 3". */
    fun assertPage(pageTag: String, number: Int) {
        assertDisplayed(pageTag)
        assertDescription(TestTags.CLOCK_PAGE_INDICATOR, "Clock $number of 3")
    }

    fun assertWeatherPage(time: String, location: String, temperature: String, condition: String) {
        assertText(TestTags.CLOCK_WEATHER_TIME, time)
        assertText(TestTags.CLOCK_WEATHER_LOCATION, location)
        assertText(TestTags.CLOCK_WEATHER_TEMPERATURE, temperature)
        assertText(TestTags.CLOCK_WEATHER_CONDITION, condition)
    }

    fun assertSecondsProgress(fraction: Float) {
        node(TestTags.LUXURY_CLOCK_DIAL).assert(hasProgressBarRangeInfo(ProgressBarRangeInfo(fraction, 0f..1f)))
    }
}

fun AppComposeRule.clock(block: ClockRobot.() -> Unit) {
    ClockRobot(this).block()
}
