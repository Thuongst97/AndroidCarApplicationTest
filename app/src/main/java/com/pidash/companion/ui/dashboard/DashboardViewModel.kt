package com.pidash.companion.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pidash.companion.domain.model.MediaState
import com.pidash.companion.domain.model.SystemSettings
import com.pidash.companion.domain.model.TripData
import com.pidash.companion.domain.model.Weather
import com.pidash.companion.domain.repository.SettingsRepository
import com.pidash.companion.domain.usecase.MediaControlUseCase
import com.pidash.companion.domain.usecase.ObserveMediaStateUseCase
import com.pidash.companion.domain.usecase.ObserveTripDataUseCase
import com.pidash.companion.domain.usecase.ObserveWeatherUseCase
import com.pidash.companion.domain.usecase.ToggleMuteUseCase
import com.pidash.companion.domain.usecase.ToggleThemeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class DashboardUiState(
    val trip: TripData = TripData(),
    val weather: Weather? = null,
    val settings: SystemSettings = SystemSettings(),
    val media: MediaState = MediaState(),
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    observeTrip: ObserveTripDataUseCase,
    observeWeather: ObserveWeatherUseCase,
    observeMedia: ObserveMediaStateUseCase,
    settingsRepository: SettingsRepository,
    private val toggleMute: ToggleMuteUseCase,
    private val toggleTheme: ToggleThemeUseCase,
    private val mediaControl: MediaControlUseCase,
) : ViewModel() {

    val uiState: StateFlow<DashboardUiState> = combine(
        observeTrip().onStart { emit(TripData()) },
        observeWeather().map<Weather, Weather?> { it }.onStart { emit(null) },
        settingsRepository.settings,
        observeMedia(),
    ) { trip, weather, settings, media ->
        DashboardUiState(trip = trip, weather = weather, settings = settings, media = media)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())

    fun onToggleMute() = toggleMute()
    fun onToggleTheme() = toggleTheme()
    fun onTogglePlayPause() = mediaControl.togglePlayPause()
}
