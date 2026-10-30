package com.pidash.companion.domain.model

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    /** ARGB colors used to render generated album artwork (no network images on a head unit). */
    val artworkColors: List<Long>,
)

data class MediaState(
    val track: Track? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0,
    val queueIndex: Int = 0,
    val queueSize: Int = 0,
) {
    val progress: Float
        get() = track?.takeIf { it.durationMs > 0 }?.let { positionMs.toFloat() / it.durationMs }?.coerceIn(0f, 1f) ?: 0f
}
