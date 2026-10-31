package com.pidash.companion

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pidash.companion.robots.clock
import com.pidash.companion.ui.components.TestTags
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDateTime

/** Rail clock → full-screen digital clock: open, content, animation, and all ways to close. */
@RunWith(AndroidJUnit4::class)
class LuxuryClockTest {

    @get:Rule
    val rule = piDashComposeRule()

    /** Frozen time (tests disable the ticking loop, so it stays exactly here). */
    private val tenNine = LocalDateTime.of(2026, 10, 31, 10, 9, 30)
    private val fixedTime = { tenNine }

    @Test
    fun railClock_rendered_showsTimeAndDate() = rule.clock {
        launch(fixedTime)

        assertRailClock(time = "10:09", date = "SAT 31 OCT")
        assertClosed()
    }

    @Test
    fun railClock_tapped_expandsToFullScreenClock() = rule.clock {
        launch(fixedTime)

        tapRailClock()

        assertOpenFullScreen()
    }

    @Test
    fun fullScreenClock_fixedTime_showsDigitsSecondsAndDate() = rule.clock {
        launch(fixedTime)
        tapRailClock()

        assertTime(hhmm = "10:09", seconds = "30", date = "SATURDAY  ·  31 OCTOBER")
        assertSecondsProgress(0.5f)
    }

    @Test
    fun expandAnimation_midway_isPartiallyRevealed() = rule.clock {
        // Given: drive the frame clock by hand to observe the animation
        launch(fixedTime)
        rule.mainClock.autoAdvance = false

        // When
        tapRailClock()
        rule.mainClock.advanceTimeBy(250)

        // Then: mid-reveal…
        val midway = revealFraction()
        assertTrue("Expected a partial reveal, was $midway", midway > 0.05f && midway < 0.99f)

        // …and fully open once the 750 ms reveal has finished
        rule.mainClock.advanceTimeBy(1_000)
        assertEquals(1f, revealFraction(), 0.001f)
    }

    @Test
    fun fullScreenClock_closeTapped_collapses() = rule.clock {
        launch(fixedTime)
        tapRailClock()

        tapClose()

        assertClosed()
    }

    @Test
    fun fullScreenClock_tappedAnywhere_togglesControlsWithoutClosing() = rule.clock {
        launch(fixedTime)
        tapRailClock()
        assertControlsVisible(true) // shown briefly on open (auto-hide is disabled under test)

        tapBackground()
        assertControlsVisible(false)
        assertOpenFullScreen()

        tapBackground()
        assertControlsVisible(true)
    }

    @Test
    @Smoke
    fun fullScreenClock_swipedUp_showsRectangleThenWeatherClock() = rule.clock {
        launch(fixedTime, weather = TestData.clearWeather)
        tapRailClock()
        assertPage(TestTags.CLOCK_PAGE_COCKPIT, 1)

        swipeUp()
        assertPage(TestTags.CLOCK_PAGE_RECTANGLE, 2)
        assertDescription(TestTags.CLOCK_RECTANGLE_FACE, "Rectangular analog clock showing 10:09")

        swipeUp()
        assertPage(TestTags.CLOCK_PAGE_WEATHER, 3)
        assertWeatherPage(time = "10:09", location = "Nha Trang", temperature = "30°", condition = "Clear")

        swipeDown()
        assertPage(TestTags.CLOCK_PAGE_RECTANGLE, 2)
    }

    @Test
    fun fullScreenClock_backPressed_collapses() = rule.clock {
        launch(fixedTime)
        tapRailClock()

        pressBack()

        assertClosed()
    }
}
