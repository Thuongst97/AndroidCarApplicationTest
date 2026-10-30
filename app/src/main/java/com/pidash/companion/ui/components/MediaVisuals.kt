package com.pidash.companion.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.pidash.companion.domain.model.Track
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

fun Track.artworkBrush(): Brush = Brush.linearGradient(artworkColors.map { Color(it) })

/** Generated album art: the track's gradient with a soft highlight and a waveform glyph. */
@Composable
fun AlbumArtwork(track: Track?, shape: Shape, modifier: Modifier = Modifier) {
    val brush = track?.artworkBrush() ?: Brush.linearGradient(listOf(Color.DarkGray, Color.Black))
    Box(modifier.clip(shape).background(brush), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        listOf(Color.White.copy(alpha = 0.18f), Color.Transparent),
                        center = Offset(80f, 60f),
                        radius = 500f,
                    ),
                ),
        )
        Icon(
            Icons.Rounded.GraphicEq,
            contentDescription = null, // decorative; the title text describes the track
            tint = Color.White.copy(alpha = 0.55f),
            modifier = Modifier.fillMaxSize(0.3f),
        )
    }
}

/** Animated bar visualizer; settles to a flat line when paused. */
@Composable
fun WaveVisualizer(isPlaying: Boolean, color: Color, modifier: Modifier = Modifier, bars: Int = 32) {
    val phase = if (isPlaying) {
        rememberInfiniteTransition(label = "wave").animateFloat(
            initialValue = 0f,
            targetValue = (2 * PI).toFloat(),
            animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Restart),
            label = "wavePhase",
        ).value
    } else 0f

    Canvas(modifier.fillMaxWidth()) {
        val gap = 3.dp.toPx()
        val barWidth = (size.width - gap * (bars - 1)) / bars
        for (i in 0 until bars) {
            val level = if (isPlaying) {
                val a = sin(phase * 2 + i * 0.55f)
                val b = sin(phase * 3 + i * 0.23f + 1.3f)
                0.15f + 0.85f * abs(a * 0.6f + b * 0.4f)
            } else 0.08f
            val h = size.height * level
            drawRoundRect(
                brush = Brush.verticalGradient(listOf(color, color.copy(alpha = 0.35f)), startY = size.height - h, endY = size.height),
                topLeft = Offset(i * (barWidth + gap), size.height - h),
                size = Size(barWidth, h),
                cornerRadius = CornerRadius(barWidth / 2),
            )
        }
    }
}
