package com.pidash.companion.ui.clock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pidash.companion.domain.model.Weather
import com.pidash.companion.domain.usecase.ObserveWeatherUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Supplies the full-screen clock's weather page (location, temperature, condition). */
@HiltViewModel
class ClockViewModel @Inject constructor(observeWeather: ObserveWeatherUseCase) : ViewModel() {
    val weather: StateFlow<Weather?> = observeWeather()
        .map<Weather, Weather?> { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
