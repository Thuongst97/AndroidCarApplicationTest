package com.pidash.companion

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pidash.companion.domain.model.MediaState
import com.pidash.companion.domain.repository.MediaRepository
import com.pidash.companion.domain.usecase.MediaControlUseCase
import com.pidash.companion.domain.usecase.ObserveMediaStateUseCase
import com.pidash.companion.robots.media
import com.pidash.companion.ui.media.MediaActions
import com.pidash.companion.ui.media.MediaViewModel
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

/** B. User actions & gestures. */
@RunWith(AndroidJUnit4::class)
class MediaCompanionTest {

    @get:Rule
    val rule = piDashComposeRule()

    private val track = TestData.track

    @Test
    fun playPause_tapped_togglesStateAndCallsViewModel() {
        // Given: real ViewModel + use cases over a MockK repository
        val player = MutableStateFlow(MediaState(track = track, queueSize = 1))
        val repository = mockk<MediaRepository>(relaxed = true) {
            every { mediaState } returns player
            every { play() } answers { player.update { it.copy(isPlaying = true) } }
            every { pause() } answers { player.update { it.copy(isPlaying = false) } }
        }
        val viewModel = MediaViewModel(ObserveMediaStateUseCase(repository), MediaControlUseCase(repository))

        rule.media {
            launch(MediaActions(viewModel::onTogglePlayPause, viewModel::onSkipNext, viewModel::onSkipPrevious, viewModel::onSeek)) {
                viewModel.uiState.collectAsState().value
            }
            assertPlaying(false)

            // When
            tapPlayPause()

            // Then
            verify(exactly = 1) { repository.play() }
            assertPlaying(true)

            // When / Then
            tapPlayPause()
            verify(exactly = 1) { repository.pause() }
            assertPlaying(false)
        }
    }

    @Test
    fun skipButtons_tapped_invokeCallbacks() {
        var next = 0
        var previous = 0

        rule.media {
            launch(MediaActions(onSkipNext = { next++ }, onSkipPrevious = { previous++ })) { MediaState(track = track) }
            tapNext()
            tapNext()
            tapPrevious()
        }

        assertEquals(2, next)
        assertEquals(1, previous)
    }

    @Test
    fun trackInfo_withTrack_showsTitleArtistAndTimes() = rule.media {
        launch { MediaState(track = track, positionMs = 65_000) }

        assertTrackInfo(title = track.title, artistLine = "${track.artist} · ${track.album}")
        assertTimes(position = "1:05", duration = "3:34")
    }

    @Test
    fun progressSlider_dragged_seeksToReleasedPosition() {
        // Given
        var seekedTo: Float? = null
        var state by mutableStateOf(MediaState(track = track))

        rule.media {
            launch(MediaActions(onSeek = { f -> seekedTo = f; state = state.copy(positionMs = (track.durationMs * f).toLong()) })) { state }

            // When
            dragProgressTo(0.5f)

            // Then — drag end is reported from a coroutine, so read once idle
            val fraction = rule.runOnIdle { seekedTo }
            assertNotNull("Slider drag did not seek", fraction)
            assertTrue("Expected ≈0.5, was $fraction", fraction!! in 0.35f..0.65f)
            val seconds = (track.durationMs * fraction).toLong() / 1000
            assertTimes(position = String.format(Locale.US, "%d:%02d", seconds / 60, seconds % 60))
        }
    }

    @Test
    fun progressSlider_tapped_seeksToTappedPosition() {
        var seekedTo: Float? = null

        rule.media {
            launch(MediaActions(onSeek = { seekedTo = it })) { MediaState(track = track) }
            tapProgressAt(0.8f)
        }

        val fraction = rule.runOnIdle { seekedTo }
        assertNotNull(fraction)
        assertTrue("Expected ≈0.8, was $fraction", fraction!! in 0.7f..0.9f)
    }
}
