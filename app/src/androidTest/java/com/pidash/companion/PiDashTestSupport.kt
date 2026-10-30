package com.pidash.companion

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pidash.companion.data.repository.MockSpotsRepository
import com.pidash.companion.data.source.MockMediaCatalog
import com.pidash.companion.domain.model.MediaState
import com.pidash.companion.domain.model.TripData
import com.pidash.companion.domain.model.Weather
import com.pidash.companion.domain.model.WeatherCondition
import com.pidash.companion.testing.ComposeTestConfig
import com.pidash.companion.testing.ScreenContractTest
import com.pidash.companion.testing.createAppComposeRule
import com.pidash.companion.testing.hasSemantics
import com.pidash.companion.ui.components.DashSemantics
import com.pidash.companion.ui.dashboard.DashboardUiState
import com.pidash.companion.ui.spots.SpotsUiState
import com.pidash.companion.ui.theme.DashTheme
import com.pidash.companion.ui.theme.PiDashTheme
import com.pidash.companion.ui.theme.ambientBackground

/** Pi-Dash UI standards: automotive 64dp targets, in-app font scale capped at 1.3×. */
val PiDashTestConfig = ComposeTestConfig(
    minTouchTarget = 64.dp,
    defaultFontScale = 1f,
    maxFontScale = 1.3f,
    themeWrapper = { darkTheme, content ->
        PiDashTheme(darkTheme = darkTheme) {
            Box(Modifier.fillMaxSize().ambientBackground(DashTheme.colors)) { content() }
        }
    },
)

/** `@get:Rule val rule = piDashComposeRule()` */
fun piDashComposeRule() = createAppComposeRule(PiDashTestConfig)

/** Base class for per-screen contract tests in this app. */
abstract class PiDashScreenContract : ScreenContractTest(PiDashTestConfig)

/** Matchers for the app's custom semantics keys. */
fun isRotaryFocused(focused: Boolean = true) = hasSemantics(DashSemantics.RotaryFocused, focused)
fun isGlowActive(active: Boolean = true) = hasSemantics(DashSemantics.GlowActive, active)

/** Shared, representative fixtures. */
object TestData {
    val track = MockMediaCatalog.tracks.first()
    val spots = MockSpotsRepository.SAMPLE_SPOTS
    val rainyWeather = Weather(27, WeatherCondition.RAIN, "Da Lat", 90)
    val clearWeather = Weather(30, WeatherCondition.CLEAR, "Nha Trang", 55)

    val dashboard = DashboardUiState(
        trip = TripData(speedKmh = 72, speedLimitKmh = 80, tripDistanceKm = 12.4, averageSpeedKmh = 46, elapsedSeconds = 980),
        weather = clearWeather,
        media = MediaState(track = track),
    )
    val media = MediaState(track = MockMediaCatalog.tracks[1], positionMs = 42_000)
    val spotsState = SpotsUiState(spots = spots, isLoading = false)
}
