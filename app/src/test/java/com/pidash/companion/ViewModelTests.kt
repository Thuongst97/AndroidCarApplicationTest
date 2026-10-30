package com.pidash.companion

import app.cash.turbine.test
import com.pidash.companion.data.repository.MockSpotsRepository
import com.pidash.companion.domain.model.MediaState
import com.pidash.companion.domain.model.SpotCategory
import com.pidash.companion.domain.model.SystemSettings
import com.pidash.companion.domain.model.TripData
import com.pidash.companion.domain.model.Weather
import com.pidash.companion.domain.model.WeatherCondition
import com.pidash.companion.domain.repository.MediaRepository
import com.pidash.companion.domain.repository.SettingsRepository
import com.pidash.companion.domain.repository.SpotsRepository
import com.pidash.companion.domain.repository.TripRepository
import com.pidash.companion.domain.repository.WeatherRepository
import com.pidash.companion.domain.usecase.FilterSpotsUseCase
import com.pidash.companion.domain.usecase.MediaControlUseCase
import com.pidash.companion.domain.usecase.ObserveMediaStateUseCase
import com.pidash.companion.domain.usecase.ObserveTripDataUseCase
import com.pidash.companion.domain.usecase.ObserveWeatherUseCase
import com.pidash.companion.domain.usecase.ToggleMuteUseCase
import com.pidash.companion.domain.usecase.ToggleThemeUseCase
import com.pidash.companion.ui.dashboard.DashboardViewModel
import com.pidash.companion.ui.media.MediaViewModel
import com.pidash.companion.ui.spots.SpotsViewModel
import androidx.lifecycle.SavedStateHandle
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class DashboardViewModelTest {
    @get:Rule val mainRule = MainDispatcherRule()

    private val trip = MutableSharedFlow<TripData>()
    private val settingsFlow = MutableStateFlow(SystemSettings())
    private val media = MutableStateFlow(MediaState())
    private val settingsRepo = mockk<SettingsRepository>(relaxed = true) {
        every { settings } returns settingsFlow
        every { setMuted(any()) } answers { settingsFlow.value = settingsFlow.value.copy(isMuted = firstArg()) }
        every { setDarkTheme(any()) } answers { settingsFlow.value = settingsFlow.value.copy(isDarkTheme = firstArg()) }
    }
    private val mediaRepo = mockk<MediaRepository>(relaxed = true) { every { mediaState } returns media }

    private fun viewModel() = DashboardViewModel(
        ObserveTripDataUseCase(object : TripRepository { override val tripData = trip }),
        ObserveWeatherUseCase(object : WeatherRepository { override val weather = flowOf(Weather(25, WeatherCondition.CLOUDY, "Hue", 80)) }),
        ObserveMediaStateUseCase(mediaRepo),
        settingsRepo,
        ToggleMuteUseCase(settingsRepo),
        ToggleThemeUseCase(settingsRepo),
        MediaControlUseCase(mediaRepo),
    )

    @Test
    fun `speed emissions flow into ui state`() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            assertEquals(0, expectMostRecentItem().trip.speedKmh)
            trip.emit(TripData(speedKmh = 64))
            assertEquals(64, awaitItem().trip.speedKmh)
            trip.emit(TripData(speedKmh = 101, speedLimitKmh = 80))
            val state = awaitItem()
            assertEquals(101, state.trip.speedKmh)
            assertTrue(state.trip.isSpeeding)
            assertEquals("Hue", state.weather?.locationName)
        }
    }

    @Test
    fun `toggles update settings through repository`() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            skipItems(1)
            vm.onToggleMute()
            assertTrue(expectMostRecentItem().settings.isMuted)
            vm.onToggleTheme()
            assertEquals(false, expectMostRecentItem().settings.isDarkTheme)
        }
        verify { settingsRepo.setMuted(true) }
        verify { settingsRepo.setDarkTheme(false) }
    }

    @Test
    fun `mini player toggle plays when paused`() {
        viewModel().onTogglePlayPause()
        verify { mediaRepo.play() }
    }
}

class MediaViewModelTest {
    @get:Rule val mainRule = MainDispatcherRule()

    private val state = MutableStateFlow(MediaState(isPlaying = true))
    private val repo = mockk<MediaRepository>(relaxed = true) { every { mediaState } returns state }
    private val vm = MediaViewModel(ObserveMediaStateUseCase(repo), MediaControlUseCase(repo))

    @Test
    fun `toggle pauses when playing`() {
        vm.onTogglePlayPause()
        verify { repo.pause() }
    }

    @Test
    fun `seek is clamped to valid range`() {
        vm.onSeek(1.7f)
        verify { repo.seekTo(1f) }
    }

    @Test
    fun `skip delegates to repository`() {
        vm.onSkipNext()
        vm.onSkipPrevious()
        verify { repo.skipNext() }
        verify { repo.skipPrevious() }
    }
}

class SpotsViewModelTest {
    @get:Rule val mainRule = MainDispatcherRule()

    private val repo = object : SpotsRepository { override fun spots() = flowOf(MockSpotsRepository.SAMPLE_SPOTS) }

    @Test
    fun `category filter narrows and sorts spots`() = runTest {
        val vm = SpotsViewModel(FilterSpotsUseCase(repo), SavedStateHandle())
        vm.uiState.test {
            assertEquals(MockSpotsRepository.SAMPLE_SPOTS.size, expectMostRecentItem().spots.size)

            vm.onCategorySelected(SpotCategory.GAS_STATION)
            val filtered = expectMostRecentItem()
            assertEquals(SpotCategory.GAS_STATION, filtered.selectedCategory)
            assertTrue(filtered.spots.all { it.category == SpotCategory.GAS_STATION })
            assertEquals(filtered.spots.sortedBy { it.distanceKm }, filtered.spots)

            vm.onCategorySelected(SpotCategory.GAS_STATION)
            assertNull(expectMostRecentItem().selectedCategory)
        }
    }

    @Test
    fun `initial category comes from navigation argument`() = runTest {
        val vm = SpotsViewModel(FilterSpotsUseCase(repo), SavedStateHandle(mapOf(SpotsViewModel.ARG_CATEGORY to "FOOD")))
        vm.uiState.test {
            val state = expectMostRecentItem()
            assertEquals(SpotCategory.FOOD, state.selectedCategory)
            assertTrue(state.spots.all { it.category == SpotCategory.FOOD })
        }
    }
}
