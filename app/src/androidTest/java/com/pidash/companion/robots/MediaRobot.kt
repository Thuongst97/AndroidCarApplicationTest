package com.pidash.companion.robots

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.click
import androidx.compose.ui.test.percentOffset
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeRight
import com.pidash.companion.isGlowActive
import com.pidash.companion.domain.model.MediaState
import com.pidash.companion.testing.AppComposeRule
import com.pidash.companion.testing.BaseRobot
import com.pidash.companion.ui.components.TestTags
import com.pidash.companion.ui.media.MediaActions
import com.pidash.companion.ui.media.MediaScreen

class MediaRobot(rule: AppComposeRule) : BaseRobot(rule) {

    fun launch(actions: MediaActions = MediaActions(), state: @Composable () -> MediaState) {
        rule.setContent { MediaScreen(state(), actions) }
    }

    // ---- actions ----
    fun tapPlayPause() = tap(TestTags.MEDIA_PLAY_PAUSE)
    fun tapNext() = tap(TestTags.MEDIA_NEXT)
    fun tapPrevious() = tap(TestTags.MEDIA_PREVIOUS)

    /** Drag the progress thumb from the start of the track to [fraction] of its width. */
    fun dragProgressTo(fraction: Float) {
        node(TestTags.MEDIA_SLIDER).performTouchInput {
            swipeRight(startX = left + width * 0.05f, endX = left + width * fraction, durationMillis = 400)
        }
        rule.observe()
    }

    fun tapProgressAt(fraction: Float) {
        node(TestTags.MEDIA_SLIDER).performTouchInput { click(percentOffset(fraction, 0.5f)) }
        rule.observe()
    }

    // ---- checks ----
    fun assertPlaying(playing: Boolean) {
        assertDescription(TestTags.MEDIA_PLAY_PAUSE, if (playing) "Pause" else "Play")
        assertStateDescription(TestTags.MEDIA_PLAY_PAUSE, if (playing) "Playing" else "Paused")
        node(TestTags.MEDIA_PLAY_PAUSE).assert(isGlowActive(playing))
    }

    fun assertTrackInfo(title: String, artistLine: String) {
        assertDisplayed(TestTags.MEDIA_ARTWORK, TestTags.MEDIA_TITLE)
        assertText(TestTags.MEDIA_TITLE, title)
        assertText(TestTags.MEDIA_ARTIST, artistLine)
    }

    fun assertTimes(position: String, duration: String? = null) {
        assertText(TestTags.MEDIA_POSITION, position)
        if (duration != null) assertText(TestTags.MEDIA_DURATION, duration)
    }
}

fun AppComposeRule.media(block: MediaRobot.() -> Unit) {
    MediaRobot(this).block()
}
