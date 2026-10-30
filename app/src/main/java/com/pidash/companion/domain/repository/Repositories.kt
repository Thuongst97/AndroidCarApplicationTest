package com.pidash.companion.domain.repository

import com.pidash.companion.domain.model.MediaState
import com.pidash.companion.domain.model.Spot
import com.pidash.companion.domain.model.SystemSettings
import com.pidash.companion.domain.model.TripData
import com.pidash.companion.domain.model.Weather
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface TripRepository {
    val tripData: Flow<TripData>
}

interface WeatherRepository {
    val weather: Flow<Weather>
}

interface MediaRepository {
    val mediaState: StateFlow<MediaState>
    fun play()
    fun pause()
    fun skipNext()
    fun skipPrevious()
    fun seekTo(fraction: Float)
}

interface SpotsRepository {
    fun spots(): Flow<List<Spot>>
}

interface SettingsRepository {
    val settings: StateFlow<SystemSettings>
    fun setDarkTheme(enabled: Boolean)
    fun setMuted(muted: Boolean)
}
