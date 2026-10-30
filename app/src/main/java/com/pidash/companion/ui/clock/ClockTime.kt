package com.pidash.companion.ui.clock

import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.delay
import java.time.LocalDateTime

/** Where the clock reads time from. Injectable so tests can freeze time. */
typealias TimeSource = () -> LocalDateTime

val SystemTimeSource: TimeSource = { LocalDateTime.now() }

/**
 * Time that advances in discrete "beats", like a mechanical movement (8 beats/s = 28,800 vph).
 *
 * Each tick waits for one frame through [withInfiniteAnimationFrameMillis] and then sleeps until the
 * next beat, so the UI only recomposes [beatsPerSecond] times per second (cheap on a Raspberry Pi).
 * Compose UI tests disable infinite animations, so under test the time stays at its initial value:
 * deterministic, and never blocks idling.
 */
@Composable
fun rememberTickingTime(source: TimeSource, beatsPerSecond: Int): State<LocalDateTime> {
    val beatNanos = 1_000_000_000L / beatsPerSecond
    fun quantize(t: LocalDateTime) = t.withNano((t.nano - t.nano % beatNanos).toInt())

    val time = remember(source) { mutableStateOf(quantize(source())) }
    LaunchedEffect(source, beatsPerSecond) {
        while (true) {
            withInfiniteAnimationFrameMillis { }
            val now = source()
            val quantized = quantize(now)
            if (quantized != time.value) time.value = quantized
            delay(((beatNanos - now.nano % beatNanos) / 1_000_000L).coerceAtLeast(1L))
        }
    }
    return time
}

/** Pages of the full-screen clock, in swipe-up order. */
enum class ClockPage { COCKPIT, RECTANGLE, WEATHER }

/** Open/closed state of the full-screen clock, the page shown and the point it expands from. */
@Stable
class LuxuryClockState {
    var isOpen by mutableStateOf(false)
        private set

    /** Current page index; changed by swipes, the rotary knob or [showPage]. */
    var page by mutableIntStateOf(0)

    /** Centre of the rail clock in root coordinates — the origin of the circular reveal. */
    var origin by mutableStateOf(Offset.Unspecified)

    /** Opens on the first clock (cockpit). */
    fun open() {
        page = 0
        isOpen = true
    }

    fun close() {
        isOpen = false
    }

    fun showPage(target: ClockPage) {
        page = target.ordinal
    }

    fun nextPage() {
        page = (page + 1).coerceAtMost(ClockPage.entries.lastIndex)
    }

    fun previousPage() {
        page = (page - 1).coerceAtLeast(0)
    }
}

@Composable
fun rememberLuxuryClockState(): LuxuryClockState = remember { LuxuryClockState() }

/** "HH:mm" in 24-hour format. */
internal fun LocalDateTime.hhmm(): String = String.format(java.util.Locale.US, "%02d:%02d", hour, minute)
