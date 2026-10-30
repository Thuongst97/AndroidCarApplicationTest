package com.pidash.companion

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pidash.companion.domain.model.SpotCategory
import com.pidash.companion.domain.model.SystemSettings
import com.pidash.companion.domain.model.TripData
import com.pidash.companion.robots.dashboard
import com.pidash.companion.ui.dashboard.DashboardActions
import com.pidash.companion.ui.dashboard.DashboardUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** A. Node finding & display assertions. */
@RunWith(AndroidJUnit4::class)
class DashboardScreenTest {

    @get:Rule
    val rule = piDashComposeRule()

    @Test
    fun speedometer_stateChanges_updatesDisplayedSpeed() = rule.dashboard {
        // Given
        var state by mutableStateOf(DashboardUiState(trip = TripData(speedKmh = 0), weather = TestData.rainyWeather))
        launch { state }
        assertSpeed(0)

        // When
        state = state.copy(trip = TripData(speedKmh = 88, speedLimitKmh = 120))

        // Then (the animated value has settled)
        assertSpeed(88)
        assertSpeedAlert(false)
    }

    @Test
    fun speedometer_overSpeedLimit_raisesAlert() = rule.dashboard {
        var state by mutableStateOf(DashboardUiState(trip = TripData(speedKmh = 60, speedLimitKmh = 80)))
        launch { state }
        assertSpeedAlert(false)

        state = state.copy(trip = TripData(speedKmh = 97, speedLimitKmh = 80))

        assertSpeedAlert(true)
        assertSpeed(97)
        assertSpeedLimit(80)
    }

    @Test
    fun controls_initialRender_allCriticalControlsDisplayed() = rule.dashboard {
        launch { TestData.dashboard }

        assertCriticalControlsDisplayed()
    }

    @Test
    fun weatherWidget_withWeather_showsTemperatureConditionAndLocation() = rule.dashboard {
        launch { DashboardUiState(weather = TestData.rainyWeather) }

        assertWeather(temperature = "27°", location = "Da Lat", condition = "Rain")
    }

    @Test
    fun controls_tapped_invokeMatchingCallbacksInOrder() {
        // Given
        val calls = mutableListOf<String>()
        val actions = DashboardActions(
            onHomeNavigation = { calls += "home" },
            onEmergencyContact = { calls += "sos" },
            onToggleMute = { calls += "mute" },
            onToggleTheme = { calls += "theme" },
            onQuickStop = { calls += "stop:${it.name}" },
        )

        rule.dashboard {
            launch(actions) { TestData.dashboard }

            // When
            tapHome()
            tapEmergency()
            tapMute()
            tapTheme()
            tapQuickStop(SpotCategory.COFFEE)
        }

        // Then
        assertEquals(listOf("home", "sos", "mute", "theme", "stop:COFFEE"), calls)
    }

    @Test
    fun muteAndTheme_toggled_reflectNewSettingsState() = rule.dashboard {
        var settings by mutableStateOf(SystemSettings(isDarkTheme = true, isMuted = false))
        launch(
            DashboardActions(
                onToggleMute = { settings = settings.copy(isMuted = !settings.isMuted) },
                onToggleTheme = { settings = settings.copy(isDarkTheme = !settings.isDarkTheme) },
            ),
        ) { TestData.dashboard.copy(settings = settings) }
        assertMuted(false)
        assertDarkTheme(true)

        tapMute()
        tapTheme()

        assertMuted(true)
        assertDarkTheme(false)
    }
}
