package com.pidash.companion.ui.clock

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.pidash.companion.ui.components.GlowButton
import com.pidash.companion.ui.components.SectionLabel
import com.pidash.companion.ui.components.TestTags
import com.pidash.companion.ui.components.rememberPressScale
import com.pidash.companion.ui.components.revealFraction
import com.pidash.companion.ui.components.rotaryFocused
import com.pidash.companion.ui.theme.DashColors
import com.pidash.companion.ui.theme.DashTheme
import com.pidash.companion.ui.theme.Dimens
import com.pidash.companion.ui.theme.ambientBackground
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.hypot

/**
 * Page 1 — cockpit-style clock: a circular luxury instrument (champagne bezel, 60 graduations with
 * numerals, a glowing comet ring sweeping once per minute that lights each graduation it passes,
 * an hour marker on the bezel) with the digital time at its centre.
 */
@Composable
internal fun CockpitClockPage(timeState: State<LocalDateTime>, modifier: Modifier = Modifier) {
    val colors = DashTheme.colors
    // Derived values so each part recomposes only when *its* value changes (digits once a minute).
    val hhmm by remember { derivedStateOf { timeState.value.hhmm() } }
    val second by remember { derivedStateOf { timeState.value.second } }
    val colonOn by remember { derivedStateOf { timeState.value.nano < 500_000_000 } }
    val date by remember { derivedStateOf { timeState.value.format(DateFormat).uppercase() } }

    val intro = remember { Animatable(0f) }
    LaunchedEffect(Unit) { intro.animateTo(1f, tween(1400, delayMillis = 150, easing = EmphasizedDecelerate)) }

    Box(modifier.fillMaxSize().testTag(TestTags.CLOCK_PAGE_COCKPIT), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .fillMaxHeight(0.92f)
                .aspectRatio(1f)
                .testTag(TestTags.LUXURY_CLOCK_DIAL)
                .semantics { progressBarRangeInfo = ProgressBarRangeInfo(second / 60f, 0f..1f) },
            contentAlignment = Alignment.Center,
        ) {
            CockpitDialStatic(intro = intro.value)
            CockpitDialLive(timeState = timeState, intro = { intro.value })

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer { alpha = ((intro.value - 0.3f) / 0.7f).coerceIn(0f, 1f) },
            ) {
                SectionLabel("Local time", color = colors.accent)
                Box(
                    Modifier
                        .testTag(TestTags.LUXURY_CLOCK_TIME)
                        .clearAndSetSemantics {
                            text = AnnotatedString(hhmm)
                            contentDescription = "Time $hhmm"
                        },
                ) {
                    Digits(hhmm = hhmm, colonOn = colonOn, intro = { intro.value })
                }
                SectionLabel(date, color = colors.textSecondary, modifier = Modifier.testTag(TestTags.LUXURY_CLOCK_DATE))
                Spacer(Modifier.height(10.dp))
                Text(
                    "%02d".format(Locale.US, second),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Light, fontFeatureSettings = "tnum"),
                    color = colors.accent,
                    modifier = Modifier.testTag(TestTags.LUXURY_CLOCK_SECONDS),
                )
            }
        }
    }
}

/**
 * HH:MM in thin tabular figures; each digit rolls up independently when it changes.
 *
 * @param metallic premium finish: digits in brushed champagne gold with a soft warm glow.
 *   Plain text colour otherwise.
 */
@Composable
internal fun Digits(hhmm: String, colonOn: Boolean, intro: () -> Float, fontSize: TextUnit = 104.sp, metallic: Boolean = false) {
    val colors = DashTheme.colors
    val style = MaterialTheme.typography.displayLarge.copy(
        fontSize = fontSize,
        fontWeight = FontWeight.Thin,
        letterSpacing = (-3).sp,
        fontFeatureSettings = "tnum", // equal-width digits: no jitter when they change
        color = colors.textPrimary,
    )
    val hourStyle = if (metallic) style.metallicGold(colors) else style
    val minuteStyle = hourStyle // same metallic finish for hours and minutes
    val colonAlpha by animateFloatAsState(if (colonOn) 1f else 0.25f, tween(350), label = "colon")

    Row(verticalAlignment = Alignment.CenterVertically) {
        hhmm.forEachIndexed { index, ch ->
            // Staggered intro: digits rise into place one after another
            val itemModifier = Modifier.graphicsLayer {
                val p = ((intro() - 0.3f - index * 0.06f) / 0.5f).coerceIn(0f, 1f)
                alpha = p
                translationY = (1f - p) * 24.dp.toPx()
            }
            if (ch == ':') {
                Text(":", style = style.copy(color = colors.accent), modifier = itemModifier.graphicsLayer { alpha *= colonAlpha })
            } else {
                AnimatedContent(
                    targetState = ch,
                    transitionSpec = {
                        (slideInVertically(tween(450, easing = EmphasizedDecelerate)) { it / 2 } + fadeIn(tween(450))) togetherWith
                            (slideOutVertically(tween(300)) { -it / 2 } + fadeOut(tween(250)))
                    },
                    label = "digit$index",
                    modifier = itemModifier,
                ) { digit ->
                    Text(digit.toString(), style = if (index < 2) hourStyle else minuteStyle)
                }
            }
        }
    }
}

/**
 * Static part of the instrument, in its own layer (re-recorded only during the intro): metal
 * bezel drawn clockwise, dark dial face, 60 graduations and 12 numerals lighting up in sequence.
 */
@Composable
private fun CockpitDialStatic(intro: Float) {
    val colors = DashTheme.colors
    val measurer = rememberTextMeasurer()
    val numeralStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Light, color = colors.textSecondary)
    val numerals = remember(measurer, numeralStyle) {
        (0 until 12).map { measurer.measure(if (it == 0) "12" else it.toString(), numeralStyle) }
    }
    val metalDark = lerp(colors.accent, Color.Black, 0.45f)

    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen } // GPU-cached texture
            .drawBehind {
                val r = size.minDimension / 2f
                val c = center
                val p = intro.coerceIn(0f, 1f)

                // Soft halo behind the instrument
                drawCircle(Brush.radialGradient(listOf(colors.accent.copy(alpha = 0.08f), Color.Transparent), center = c, radius = r * 1.1f), radius = r * 1.1f, center = c)

                // Dial face
                drawCircle(
                    Brush.radialGradient(
                        if (colors.isDark) listOf(Color(0xFF1A1F28), Color(0xFF07090D)) else listOf(Color.White, Color(0xFFE8E2D6)),
                        center = c,
                        radius = r * 0.94f,
                    ),
                    radius = r * 0.94f,
                    center = c,
                )

                // Champagne bezel, drawn clockwise from 12 during the intro
                val bezelW = r * 0.035f
                val bezelR = r * 0.955f
                drawArc(
                    brush = Brush.sweepGradient(
                        listOf(colors.accentLight, colors.accent, metalDark, colors.accent, colors.accentLight, metalDark, colors.accentLight),
                        center = c,
                    ),
                    startAngle = -90f,
                    sweepAngle = 360f * p,
                    useCenter = false,
                    topLeft = c - Offset(bezelR, bezelR),
                    size = Size(bezelR * 2, bezelR * 2),
                    style = Stroke(bezelW),
                )
                drawCircle(colors.hairline, radius = r * 0.92f, center = c, style = Stroke(1.dp.toPx()))
                drawCircle(colors.hairline, radius = r * 0.56f, center = c, style = Stroke(1.dp.toPx()))

                // Graduations (dim; the live layer lights the ones the second ring has passed)
                for (i in 0 until 60) {
                    val alpha = ((p * 1.5f) - i / 60f * 0.5f).coerceIn(0f, 1f)
                    if (alpha <= 0f) continue
                    drawGraduation(c, r, i, colors.textTertiary.copy(alpha = colors.textTertiary.alpha * alpha))
                }

                // Numerals 12, 1 … 11
                numerals.forEachIndexed { hour, layout ->
                    val alpha = ((p * 1.5f) - hour / 12f * 0.5f).coerceIn(0f, 1f)
                    val a = Math.toRadians(hour * 30.0 - 90)
                    val nr = r * 0.66f
                    val pos = Offset(c.x + nr * cos(a).toFloat(), c.y + nr * sin(a).toFloat())
                    drawText(layout, topLeft = pos - Offset(layout.size.width / 2f, layout.size.height / 2f), alpha = alpha)
                }
            },
    )
}

/**
 * Live part, in a separate layer that reads the time in the draw phase only: graduations lit up to
 * the current second, a comet-tail seconds ring with a glowing head, and an hour marker on the bezel.
 */
@Composable
private fun CockpitDialLive(timeState: State<LocalDateTime>, intro: () -> Float) {
    val colors = DashTheme.colors
    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer()
            .drawBehind {
                val p = intro().coerceIn(0f, 1f)
                if (p <= 0f) return@drawBehind
                val t = timeState.value
                val r = size.minDimension / 2f
                val c = center
                val secondsFraction = (t.second + t.nano / 1_000_000_000f) / 60f
                val sweep = 360f * secondsFraction * p

                // Graduations the ring has passed glow in the accent colour
                val lit = (secondsFraction * 60f * p).toInt()
                for (i in 0..lit.coerceAtMost(59)) drawGraduation(c, r, i, colors.accent.copy(alpha = 0.9f))

                // Seconds ring: hairline track + comet tail + glowing head
                val ringR = r * 0.875f
                val ringSize = Size(ringR * 2, ringR * 2)
                val topLeft = c - Offset(ringR, ringR)
                val stroke = 4.dp.toPx()
                drawCircle(colors.hairline, radius = ringR, center = c, style = Stroke(stroke))
                if (sweep > 0.5f) {
                    val headStop = sweep / 360f
                    val comet = Brush.sweepGradient(
                        0f to colors.accent.copy(alpha = 0f),
                        headStop * 0.6f to colors.accent.copy(alpha = 0.45f),
                        headStop to colors.accentLight,
                        1f to colors.accentLight,
                        center = c,
                    )
                    rotate(-90f, pivot = c) {
                        drawArc(comet, 0f, sweep, false, topLeft, ringSize, alpha = 0.18f, style = Stroke(stroke * 3.5f, cap = StrokeCap.Round))
                        drawArc(comet, 0f, sweep, false, topLeft, ringSize, style = Stroke(stroke, cap = StrokeCap.Round))
                    }
                }
                val headA = Math.toRadians(sweep - 90.0)
                val head = Offset(c.x + ringR * cos(headA).toFloat(), c.y + ringR * sin(headA).toFloat())
                drawCircle(Brush.radialGradient(listOf(colors.accent.copy(alpha = 0.6f), Color.Transparent), center = head, radius = stroke * 4.5f), radius = stroke * 4.5f, center = head)
                drawCircle(colors.accentLight, radius = stroke * 0.9f, center = head)

                // Hour marker: luminous triangle on the bezel pointing inwards
                val hourAngle = ((t.hour % 12) + t.minute / 60f) * 30f
                rotate(hourAngle * p, pivot = c) {
                    val tip = Offset(c.x, c.y - r * 0.905f)
                    val marker = Path().apply {
                        moveTo(tip.x, tip.y)
                        lineTo(tip.x - r * 0.025f, tip.y - r * 0.05f)
                        lineTo(tip.x + r * 0.025f, tip.y - r * 0.05f)
                        close()
                    }
                    drawCircle(Brush.radialGradient(listOf(colors.accentLight.copy(alpha = 0.5f), Color.Transparent), center = tip, radius = r * 0.06f), radius = r * 0.06f, center = tip)
                    drawPath(marker, colors.accentLight, alpha = p)
                }
            },
    )
}

private fun DrawScope.drawGraduation(c: Offset, r: Float, index: Int, color: Color) {
    val major = index % 5 == 0
    val outer = r * 0.83f
    val inner = outer - if (major) r * 0.065f else r * 0.03f
    val a = Math.toRadians(index * 6.0 - 90)
    val cosA = cos(a).toFloat()
    val sinA = sin(a).toFloat()
    drawLine(
        color,
        start = Offset(c.x + inner * cosA, c.y + inner * sinA),
        end = Offset(c.x + outer * cosA, c.y + outer * sinA),
        strokeWidth = if (major) 2.5.dp.toPx() else 1.2.dp.toPx(),
        cap = StrokeCap.Round,
    )
}

/** Brushed champagne-gold text with a soft warm glow — shared by the digital clock's digits and date. */
internal fun TextStyle.metallicGold(colors: DashColors): TextStyle = copy(
    brush = Brush.verticalGradient(listOf(colors.accentLight, colors.accent, lerp(colors.accent, Color.Black, 0.3f), colors.accentLight)),
    shadow = Shadow(colors.accent.copy(alpha = 0.45f), blurRadius = 28f),
)
