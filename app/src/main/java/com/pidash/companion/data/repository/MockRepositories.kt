package com.pidash.companion.data.repository

import com.pidash.companion.data.source.MockMediaCatalog
import com.pidash.companion.data.source.MockSpeedGenerator
import com.pidash.companion.di.ApplicationScope
import com.pidash.companion.domain.model.MediaState
import com.pidash.companion.domain.model.Spot
import com.pidash.companion.domain.model.SpotCategory
import com.pidash.companion.domain.model.Track
import com.pidash.companion.domain.model.TripData
import com.pidash.companion.domain.model.Weather
import com.pidash.companion.domain.model.WeatherCondition
import com.pidash.companion.domain.repository.MediaRepository
import com.pidash.companion.domain.repository.SpotsRepository
import com.pidash.companion.domain.repository.TripRepository
import com.pidash.companion.domain.repository.WeatherRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MockTripRepository @Inject constructor() : TripRepository {
    override val tripData: Flow<TripData> = MockSpeedGenerator().stream()
}

/**
 * Cycles through locations along a simulated route, refreshing like a real weather API would.
 * Shared (one upstream, replay = 1) so every screen shows the same location at the same time.
 */
@Singleton
class MockWeatherRepository @Inject constructor(
    @ApplicationScope scope: CoroutineScope,
) : WeatherRepository {
    private val samples = listOf(
        Weather(29, WeatherCondition.CLEAR, "Da Nang City", 70),
        Weather(28, WeatherCondition.PARTLY_CLOUDY, "Son Tra Peninsula", 74),
        Weather(27, WeatherCondition.CLOUDY, "Ngu Hanh Son", 78),
        Weather(19, WeatherCondition.RAIN, "Ba Na Hills", 92),
        Weather(25, WeatherCondition.NIGHT_CLEAR, "Hoi An", 80),
    )

    override val weather: Flow<Weather> = flow {
        var index = 0
        while (true) {
            emit(samples[index % samples.size])
            index++
            delay(30_000)
        }
    }.shareIn(scope, SharingStarted.WhileSubscribed(5_000), replay = 1)
}

/** In-memory player that advances the playback position while playing and auto-advances tracks. */
@Singleton
class MockMediaRepository @Inject constructor(
    @ApplicationScope private val scope: CoroutineScope,
) : MediaRepository {
    private val queue: List<Track> = MockMediaCatalog.tracks
    private val _state = MutableStateFlow(MediaState(track = queue.first(), queueIndex = 0, queueSize = queue.size))
    override val mediaState: StateFlow<MediaState> = _state.asStateFlow()

    private var ticker: Job? = null

    override fun play() {
        _state.update { it.copy(isPlaying = true) }
        if (ticker?.isActive == true) return
        ticker = scope.launch {
            while (isActive) {
                delay(TICK_MS)
                val current = _state.value
                val track = current.track ?: break
                val next = current.positionMs + TICK_MS
                if (next >= track.durationMs) skipNext() else _state.value = current.copy(positionMs = next)
            }
        }
    }

    override fun pause() {
        ticker?.cancel()
        ticker = null
        _state.update { it.copy(isPlaying = false) }
    }

    override fun skipNext() = moveTo(_state.value.queueIndex + 1)

    override fun skipPrevious() {
        // Like real players: restart the current track when more than 3s in, otherwise go back.
        if (_state.value.positionMs > 3_000) _state.update { it.copy(positionMs = 0) }
        else moveTo(_state.value.queueIndex - 1)
    }

    override fun seekTo(fraction: Float) {
        _state.update { s -> s.copy(positionMs = ((s.track?.durationMs ?: 0) * fraction).toLong()) }
    }

    private fun moveTo(index: Int) {
        val wrapped = Math.floorMod(index, queue.size)
        _state.update { it.copy(track = queue[wrapped], queueIndex = wrapped, positionMs = 0) }
    }

    private companion object {
        const val TICK_MS = 1_000L
    }
}

@Singleton
class MockSpotsRepository @Inject constructor() : SpotsRepository {
    override fun spots(): Flow<List<Spot>> = flowOf(SAMPLE_SPOTS)

    companion object {
        val SAMPLE_SPOTS = listOf(
            Spot("c1", "Highlands Coffee", SpotCategory.COFFEE, "74 Bach Dang, Hai Chau", 0.8, 16.0716, 108.2244, isFavorite = true),
            Spot("c2", "Cong Caphe", SpotCategory.COFFEE, "96 Bach Dang, Hai Chau", 2.1, 16.0702, 108.2241),
            Spot("c3", "Phuc Long Tea & Coffee", SpotCategory.COFFEE, "910A Ngo Quyen, Son Tra", 3.4, 16.0716, 108.2302),
            Spot("c4", "The Coffee House", SpotCategory.COFFEE, "177 Nguyen Van Linh, Thanh Khe", 5.0, 16.0606, 108.2105),
            Spot("g1", "Petrolimex Station 12", SpotCategory.GAS_STATION, "201 Dien Bien Phu, Thanh Khe", 1.2, 16.0660, 108.1960, isFavorite = true),
            Spot("g2", "PV Oil Ngu Hanh Son", SpotCategory.GAS_STATION, "Le Van Hien, Ngu Hanh Son", 4.6, 16.0300, 108.2480),
            Spot("g3", "Petrolimex Hoa Khanh", SpotCategory.GAS_STATION, "Ton Duc Thang, Lien Chieu", 9.3, 16.0750, 108.1490),
            Spot("r1", "Hai Van Pass Summit Stop", SpotCategory.REST_STOP, "QL1A, Hai Van Pass", 23.0, 16.1960, 108.1300),
            Spot("r2", "Lang Co Rest Area", SpotCategory.REST_STOP, "QL1A, Lang Co, Phu Loc", 58.5, 16.2590, 108.0700),
            Spot("r3", "Tam Ky Service Station", SpotCategory.REST_STOP, "QL1A, Tam Ky, Quang Nam", 88.0, 15.5730, 108.4740),
            Spot("f1", "Mi Quang Ba Mua", SpotCategory.FOOD, "19 Tran Binh Trong, Hai Chau", 1.6, 16.0630, 108.2170, isFavorite = true),
            Spot("f2", "Banh Mi Ba Lan", SpotCategory.FOOD, "62 Trung Nu Vuong, Hai Chau", 2.7, 16.0560, 108.2140),
            Spot("f3", "Bun Cha Ca Ba Lu", SpotCategory.FOOD, "319 Hung Vuong, Thanh Khe", 4.1, 16.0680, 108.2110),
            Spot("f4", "Be Man Seafood", SpotCategory.FOOD, "Vo Nguyen Giap, Son Tra", 6.2, 16.0690, 108.2460),
        )
    }
}
