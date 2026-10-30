package com.pidash.companion.ui.clock

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.pidash.companion.ui.components.TestTags
import com.pidash.companion.ui.theme.DashColors
import com.pidash.companion.ui.theme.DashTheme
import java.time.LocalDateTime
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Page 2 — full-screen rectangular luxury watch: polished champagne case filling the screen,
 * hobnail ("clous de Paris") dial, railway minute track following the rectangle, Roman numerals at
 * XII/III/VI/IX, applied baton indices, faceted leaf hands. Static case/dial (GPU-cached) and live
 * hands are separate layers.
 */
@Composable
internal fun RectangleClockPage(timeState: State<LocalDateTime>, modifier: Modifier = Modifier) {
    val hhmm by remember { derivedStateOf { timeState.value.hhmm() } }
    val intro = remember { Animatable(0f) }
    LaunchedEffect(Unit) { intro.animateTo(1f, tween(1300, delayMillis = 100, easing = EmphasizedDecelerate)) }

    Box(modifier.fillMaxSize().testTag(TestTags.CLOCK_PAGE_RECTANGLE)) {
        Box(
            Modifier
                .fillMaxSize()
                .padding(10.dp) // edge to edge; controls float above when shown
                .testTag(TestTags.CLOCK_RECTANGLE_FACE)
                .semantics { contentDescription = "Rectangular analog clock showing $hhmm" },
        ) {
            RectangleCaseAndDial(intro = intro.value)
            RectangleHands(timeState = timeState, intro = { intro.value })
        }
    }
}

/** Proportions of the rectangular case, scaled by the shorter side [m]. */
private class RectGeometry(size: Size) {
    val w = size.width
    val h = size.height
    val m = min(w, h)
    val c = Offset(w / 2f, h / 2f)
    val caseRadius = m * 0.14f
    val dialInset = m * 0.055f
    val dialRadius = m * 0.10f

    /** Half-extents of the railway track (kept inside the dial's rounded corners). */
    val trackHalfW = w / 2f - dialInset - m * 0.05f
    val trackHalfH = h / 2f - dialInset - m * 0.05f

    /**
     * Point where a ray from the centre at [angleDeg] (0° = 12 o'clock, clockwise) meets the track
     * rectangle, pulled in by [inset]. Ray-based so indices sit exactly where the hands point.
     */
    fun onTrack(angleDeg: Float, inset: Float): Offset {
        val a = Math.toRadians(angleDeg - 90.0)
        val cosA = cos(a).toFloat()
        val sinA = sin(a).toFloat()
        val dx = if (abs(cosA) < 1e-4f) Float.MAX_VALUE else (trackHalfW - inset) / abs(cosA)
        val dy = if (abs(sinA) < 1e-4f) Float.MAX_VALUE else (trackHalfH - inset) / abs(sinA)
        val d = min(dx, dy)
        return Offset(c.x + d * cosA, c.y + d * sinA)
    }
}

@Composable
private fun RectangleCaseAndDial(intro: Float) {
    val colors = DashTheme.colors
    val measurer = rememberTextMeasurer()
    Box(
        Modifier
            .fillMaxSize()
            // Offscreen = the GPU caches this layer as a texture; it is redrawn only while the intro runs.
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawBehind { drawCaseAndDial(RectGeometry(size), colors, measurer, intro.coerceIn(0f, 1f)) },
    )
}

private fun DrawScope.drawCaseAndDial(g: RectGeometry, colors: DashColors, measurer: TextMeasurer, p: Float) {
    val m = g.m
    val metalDark = lerp(colors.accent, Color.Black, 0.45f)
    val caseRect = Rect(Offset.Zero, Size(g.w, g.h))

    // Drop shadow
    drawRoundRect(Color.Black.copy(alpha = if (colors.isDark) 0.5f else 0.2f), Offset(0f, m * 0.012f), caseRect.size, CornerRadius(g.caseRadius))

    // Polished case: diagonal metal gradient with a light/dark bevel
    drawRoundRect(
        Brush.linearGradient(listOf(colors.accentLight, colors.accent, metalDark, colors.accent, colors.accentLight), start = caseRect.topLeft, end = caseRect.bottomRight),
        caseRect.topLeft,
        caseRect.size,
        CornerRadius(g.caseRadius),
        alpha = 0.35f + 0.65f * p,
    )
    val bevel = m * 0.035f
    drawRoundRect(metalDark.copy(alpha = 0.9f), Offset(bevel, bevel), Size(g.w - bevel * 2, g.h - bevel * 2), CornerRadius(g.caseRadius * 0.8f), style = Stroke(m * 0.006f))

    // Dial
    val dialRect = Rect(Offset(g.dialInset, g.dialInset), Size(g.w - g.dialInset * 2, g.h - g.dialInset * 2))
    drawRoundRect(
        Brush.radialGradient(
            if (colors.isDark) listOf(Color(0xFF1C222C), Color(0xFF06080B)) else listOf(Color.White, Color(0xFFE6DFD1)),
            center = g.c - Offset(0f, g.h * 0.12f),
            radius = g.w * 0.6f,
        ),
        dialRect.topLeft,
        dialRect.size,
        CornerRadius(g.dialRadius),
    )

    // Clous de Paris: two sets of fine diagonal lines clipped to the dial = hobnail texture
    val dialPath = Path().apply { addRoundRect(RoundRect(dialRect, CornerRadius(g.dialRadius))) }
    val texture = if (colors.isDark) Color.White.copy(alpha = 0.045f * p) else Color.Black.copy(alpha = 0.04f * p)
    clipPath(dialPath) {
        val step = m * 0.024f
        var x = -g.h
        while (x < g.w) {
            drawLine(texture, Offset(x, dialRect.top), Offset(x + g.h, dialRect.bottom), strokeWidth = 1.2f)
            drawLine(texture, Offset(x, dialRect.bottom), Offset(x + g.h, dialRect.top), strokeWidth = 1.2f)
            x += step
        }
        // Light catching the dial from the top-left
        drawCircle(
            Brush.radialGradient(listOf(Color.White.copy(alpha = 0.07f), Color.Transparent), center = dialRect.topLeft + Offset(g.w * 0.15f, g.h * 0.15f), radius = g.w * 0.6f),
            radius = g.w,
            center = g.c,
        )
    }

    // Railway minute track: two hairline rectangles + 60 ticks on the hands' rays
    val trackColor = colors.textSecondary.copy(alpha = 0.6f * p)
    listOf(0f, m * 0.018f).forEach { inset ->
        drawRoundRect(
            trackColor,
            Offset(g.c.x - g.trackHalfW + inset, g.c.y - g.trackHalfH + inset),
            Size((g.trackHalfW - inset) * 2, (g.trackHalfH - inset) * 2),
            CornerRadius(m * 0.04f),
            style = Stroke(1.dp.toPx()),
        )
    }
    for (i in 0 until 60) {
        val major = i % 5 == 0
        drawLine(
            trackColor,
            g.onTrack(i * 6f, 0f),
            g.onTrack(i * 6f, if (major) m * 0.03f else m * 0.018f),
            strokeWidth = if (major) 2.dp.toPx() else 1.dp.toPx(),
            cap = StrokeCap.Round,
        )
    }

    // Indices: Roman numerals at the cardinal hours, applied batons elsewhere (staggered intro)
    val numeralStyle = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Medium, fontSize = (m * 0.1f).toSp(), color = colors.accentLight)
    val romans = mapOf(0 to "XII", 3 to "III", 6 to "VI", 9 to "IX")
    for (hour in 0 until 12) {
        val alpha = ((p * 1.6f) - hour / 12f * 0.6f).coerceIn(0f, 1f)
        if (alpha <= 0f) continue
        val angle = hour * 30f
        val roman = romans[hour]
        if (roman != null) {
            val layout = measurer.measure(roman, numeralStyle)
            val inset = if (hour % 6 == 0) m * 0.1f else m * 0.13f
            val pos = g.onTrack(angle, inset)
            drawText(layout, topLeft = pos - Offset(layout.size.width / 2f, layout.size.height / 2f), alpha = alpha)
        } else {
            drawBaton(g, colors, metalDark, angle, alpha)
        }
    }

    // Printing
    val textAlpha = ((p - 0.5f) * 2f).coerceIn(0f, 1f)
    val brandStyle = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Medium, fontSize = (m * 0.065f).toSp(), letterSpacing = (m * 0.016f).toSp(), color = colors.accentLight)
    val brand = measurer.measure("XUAN THUONG", brandStyle)
    val secondName = measurer.measure("HA MY", brandStyle)
    val small = TextStyle(fontWeight = FontWeight.Medium, fontSize = (m * 0.03f).toSp(), letterSpacing = (m * 0.01f).toSp(), color = colors.textSecondary)
    val sub = measurer.measure("AUTOMATIC", small)
    val bottom = measurer.measure("DA NANG", small)
    drawText(brand, topLeft = Offset(g.c.x - brand.size.width / 2f, g.c.y - g.trackHalfH * 0.55f), alpha = textAlpha)
    drawText(sub, topLeft = Offset(g.c.x - sub.size.width / 2f, g.c.y - g.trackHalfH * 0.55f + brand.size.height), alpha = textAlpha)
    // "HA MY": same style as the brand, in the lower half just above "DA NANG"
    drawText(secondName, topLeft = Offset(g.c.x - secondName.size.width / 2f, g.c.y + g.trackHalfH * 0.42f - secondName.size.height - m * 0.005f), alpha = textAlpha)
    drawText(bottom, topLeft = Offset(g.c.x - bottom.size.width / 2f, g.c.y + g.trackHalfH * 0.42f), alpha = textAlpha)
}

/** Applied baton: polished metal bar with a lume strip, oriented towards the centre. */
private fun DrawScope.drawBaton(g: RectGeometry, colors: DashColors, metalDark: Color, angle: Float, alpha: Float) {
    val m = g.m
    val outer = g.onTrack(angle, m * 0.05f)
    val length = m * 0.09f
    val width = m * 0.026f
    val distance = (outer - g.c).getDistance()
    rotate(angle, pivot = g.c) {
        val topLeft = Offset(g.c.x - width / 2f, g.c.y - distance)
        drawRoundRect(Color.Black.copy(alpha = 0.35f * alpha), topLeft + Offset(m * 0.004f, m * 0.008f), Size(width, length), CornerRadius(width / 3f))
        drawRoundRect(
            Brush.horizontalGradient(listOf(colors.accentLight, colors.accent, metalDark), startX = topLeft.x, endX = topLeft.x + width),
            topLeft,
            Size(width, length),
            CornerRadius(width / 3f),
            alpha = alpha,
        )
        drawRoundRect(Color(0xFFEAF6EE), topLeft + Offset(width * 0.32f, length * 0.14f), Size(width * 0.36f, length * 0.72f), CornerRadius(width / 5f), alpha = alpha * 0.85f)
    }
}

@Composable
private fun RectangleHands(timeState: State<LocalDateTime>, intro: () -> Float) {
    val colors = DashTheme.colors
    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer() // only this layer redraws on each beat
            .drawBehind {
                val t = timeState.value
                val p = intro().coerceIn(0f, 1f)
                val g = RectGeometry(size)
                val reach = g.trackHalfH // hands scale with the short side
                val seconds = t.second + t.nano / 1_000_000_000f
                val minutes = t.minute + seconds / 60f
                val hours = (t.hour % 12) + minutes / 60f
                val metalDark = lerp(colors.accent, Color.Black, 0.45f)
                val shadow = Offset(g.m * 0.008f, g.m * 0.014f)

                drawLeafHand(g.c, hours * 30f * p, length = reach * 0.6f, width = g.m * 0.06f, colors, metalDark, shadow)
                drawLeafHand(g.c, minutes * 6f * p, length = reach * 0.9f, width = g.m * 0.048f, colors, metalDark, shadow)

                rotate(seconds * 6f * p, pivot = g.c) {
                    val tail = Offset(g.c.x, g.c.y + reach * 0.22f)
                    val tip = Offset(g.c.x, g.c.y - reach * 0.97f)
                    translate(shadow.x, shadow.y) {
                        drawLine(Color.Black.copy(alpha = 0.35f), tail, tip, strokeWidth = g.m * 0.007f, cap = StrokeCap.Round)
                    }
                    drawLine(colors.accentLight, tail, tip, strokeWidth = g.m * 0.006f, cap = StrokeCap.Round)
                    drawCircle(colors.accent, radius = g.m * 0.02f, center = Offset(g.c.x, g.c.y + reach * 0.17f))
                }
                drawCircle(Brush.radialGradient(listOf(colors.accentLight, metalDark), center = g.c - Offset(g.m * 0.005f, g.m * 0.005f), radius = g.m * 0.03f), radius = g.m * 0.026f, center = g.c)
                drawCircle(metalDark, radius = g.m * 0.007f, center = g.c)
            },
    )
}

/** Faceted leaf hand: lens-shaped blade, lit on one side, with a lume inlay and drop shadow. */
private fun DrawScope.drawLeafHand(c: Offset, angle: Float, length: Float, width: Float, colors: DashColors, metalDark: Color, shadow: Offset) {
    val tail = length * 0.18f
    val blade = Path().apply {
        moveTo(c.x, c.y + tail)
        quadraticTo(c.x - width, c.y - length * 0.35f, c.x, c.y - length)
        quadraticTo(c.x + width, c.y - length * 0.35f, c.x, c.y + tail)
        close()
    }
    val lume = Path().apply {
        moveTo(c.x, c.y - length * 0.2f)
        quadraticTo(c.x - width * 0.32f, c.y - length * 0.5f, c.x, c.y - length * 0.85f)
        quadraticTo(c.x + width * 0.32f, c.y - length * 0.5f, c.x, c.y - length * 0.2f)
        close()
    }
    rotate(angle, pivot = c) {
        translate(shadow.x, shadow.y) { drawPath(blade, Color.Black.copy(alpha = 0.35f)) }
        drawPath(blade, Brush.horizontalGradient(listOf(colors.accentLight, colors.accentLight, metalDark, metalDark), startX = c.x - width, endX = c.x + width))
        drawPath(lume, Color(0xFFEAF6EE).copy(alpha = 0.85f))
    }
}
