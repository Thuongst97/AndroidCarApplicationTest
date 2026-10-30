package com.pidash.companion.domain.usecase

import com.pidash.companion.domain.model.MediaState
import com.pidash.companion.domain.model.Spot
import com.pidash.companion.domain.model.SpotCategory
import com.pidash.companion.domain.model.TripData
import com.pidash.companion.domain.model.Weather
import com.pidash.companion.domain.repository.MediaRepository
import com.pidash.companion.domain.repository.SettingsRepository
import com.pidash.companion.domain.repository.SpotsRepository
import com.pidash.companion.domain.repository.TripRepository
import com.pidash.companion.domain.repository.WeatherRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class ObserveTripDataUseCase @Inject constructor(private val repository: TripRepository) {
    operator fun invoke(): Flow<TripData> = repository.tripData
}

class ObserveWeatherUseCase @Inject constructor(private val repository: WeatherRepository) {
    operator fun invoke(): Flow<Weather> = repository.weather
}

class ObserveMediaStateUseCase @Inject constructor(private val repository: MediaRepository) {
    operator fun invoke(): Flow<MediaState> = repository.mediaState
}

/** Single entry point for transport controls so the UI never talks to the player directly. */
class MediaControlUseCase @Inject constructor(private val repository: MediaRepository) {
    fun togglePlayPause() {
        if (repository.mediaState.value.isPlaying) repository.pause() else repository.play()
    }
    fun skipNext() = repository.skipNext()
    fun skipPrevious() = repository.skipPrevious()
    fun seekTo(fraction: Float) = repository.seekTo(fraction.coerceIn(0f, 1f))
}

/** Emits spots sorted by distance, filtered by [category] (null = all categories). */
class FilterSpotsUseCase @Inject constructor(private val repository: SpotsRepository) {
    operator fun invoke(category: Flow<SpotCategory?>): Flow<List<Spot>> =
        combine(repository.spots(), category) { spots, selected ->
            spots.filter { selected == null || it.category == selected }.sortedBy { it.distanceKm }
        }
}

class ToggleThemeUseCase @Inject constructor(private val repository: SettingsRepository) {
    operator fun invoke() = repository.setDarkTheme(!repository.settings.value.isDarkTheme)
}

class ToggleMuteUseCase @Inject constructor(private val repository: SettingsRepository) {
    operator fun invoke() = repository.setMuted(!repository.settings.value.isMuted)
}
