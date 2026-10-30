package com.pidash.companion.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pidash.companion.ui.theme.DashTheme
import kotlin.math.cos
import kotlin.math.sin

private const val START_ANGLE = 135f
private const val SWEEP_ANGLE = 270f

/**
 * Instrument-cluster gauge drawn on a Canvas: hairline bezel, graduated ticks with numerals and a
 * thin value arc with a luminous "comet tail". Colors shift from platinum/ice to champagne as
 * [value] nears [warnValue], and to amber/red beyond it.
 */
@Composable
fun NeonArcGauge(
    value: Float,
    maxValue: Float,
    warnValue: Float,
    modifier: Modifier = Modifier,
    majorStep: Int = 20,
    minorStep: Int = 10,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val colors = DashTheme.colors
    val fraction by animateFloatAsState(
        targetValue = (value / maxValue).coerceIn(0f, 1f),
        animationSpec = tween(500),
        label = "gaugeFraction",
    )
    val (tailTarget, headTarget) = when {
        value > warnValue -> colors.warning to colors.danger
        value > warnValue * 0.85f -> colors.accent to colors.warning
        else -> colors.accentSecondary to colors.textPrimary
    }
    val tail by animateColorAsState(tailTarget, tween(500), label = "tail")
    val head by animateColorAsState(headTarget, tween(500), label = "head")
    val track = colors.hairline
    val tickMajor = colors.textSecondary
    val tickMinor = colors.textTertiary.copy(alpha = 0.6f)

    // Numerals are laid out once and reused every frame (the gauge redraws while animating).
    val measurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, letterSpacing = 0.5.sp, color = colors.textTertiary)
    val labels: List<Pair<Int, TextLayoutResult>> = remember(measurer, labelStyle, maxValue, majorStep) {
        (0..maxValue.toInt() step majorStep).map { it to measurer.measure(it.toString(), labelStyle) }
    }

    Box(modifier = modifier.aspectRatio(1f), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 5.dp.toPx()
            val radius = size.minDimension / 2f - stroke * 3f
            val arcSize = Size(radius * 2, radius * 2)
            val topLeft = Offset(center.x - radius, center.y - radius)

            // Outer bezel + track
            drawCircle(track, radius = radius + stroke * 2.4f, style = Stroke(1.dp.toPx()))
            drawArc(track, START_ANGLE, SWEEP_ANGLE, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))

            drawGraduations(maxValue, minorStep, majorStep, radius - stroke * 2.6f, tickMajor, tickMinor)
            labels.forEach { (v, layout) ->
                val angle = Math.toRadians((START_ANGLE + SWEEP_ANGLE * v / maxValue).toDouble())
                val r = radius - stroke * 2.6f - 28.dp.toPx()
                val c = Offset(center.x + r * cos(angle).toFloat(), center.y + r * sin(angle).toFloat())
                drawText(layout, topLeft = Offset(c.x - layout.size.width / 2f, c.y - layout.size.height / 2f))
            }

            // Inner hairline ring for depth
            drawCircle(track, radius = radius * 0.62f, style = Stroke(1.dp.toPx()))

            val sweep = SWEEP_ANGLE * fraction
            if (sweep > 0.5f) {
                val headStop = sweep / 360f
                val brush = Brush.sweepGradient(
                    0f to tail.copy(alpha = 0f),
                    headStop * 0.55f to tail.copy(alpha = 0.55f),
                    headStop to head,
                    1f to head,
                    center = center,
                )
                rotate(START_ANGLE) {
                    // Soft bloom under the crisp arc (cheaper than a blur on a Pi).
                    listOf(4.5f to 0.06f, 2.6f to 0.12f).forEach { (w, a) ->
                        drawArc(brush, 0f, sweep, false, topLeft, arcSize, alpha = a, style = Stroke(stroke * w, cap = StrokeCap.Round))
                    }
                    drawArc(brush, 0f, sweep, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
                }

                val angle = Math.toRadians((START_ANGLE + sweep).toDouble())
                val headPos = Offset(center.x + radius * cos(angle).toFloat(), center.y + radius * sin(angle).toFloat())
                drawCircle(
                    Brush.radialGradient(listOf(head.copy(alpha = 0.55f), Color.Transparent), center = headPos, radius = stroke * 4f),
                    radius = stroke * 4f,
                    center = headPos,
                )
                drawCircle(Color.White, radius = stroke * 0.55f, center = headPos)
            }
        }
        content()
    }
}

private fun DrawScope.drawGraduations(maxValue: Float, minorStep: Int, majorStep: Int, outer: Float, major: Color, minor: Color) {
    val steps = (maxValue / minorStep).toInt()
    for (i in 0..steps) {
        val v = i * minorStep
        val isMajor = v % majorStep == 0
        val length = if (isMajor) 12.dp.toPx() else 6.dp.toPx()
        val angle = Math.toRadians((START_ANGLE + SWEEP_ANGLE * v / maxValue).toDouble())
        val c = cos(angle).toFloat()
        val s = sin(angle).toFloat()
        drawLine(
            color = if (isMajor) major else minor,
            start = Offset(center.x + (outer - length) * c, center.y + (outer - length) * s),
            end = Offset(center.x + outer * c, center.y + outer * s),
            strokeWidth = if (isMajor) 2.dp.toPx() else 1.dp.toPx(),
            cap = StrokeCap.Round,
        )
    }
}
