package com.pidash.companion.ui.clock

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.runtime.snapshotFlow
import com.pidash.companion.domain.model.Weather
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pidash.companion.ui.components.GlowButton
import com.pidash.companion.ui.components.SectionLabel
import com.pidash.companion.ui.components.TestTags
import com.pidash.companion.ui.components.rememberPressScale
import com.pidash.companion.ui.components.revealFraction
import com.pidash.companion.ui.components.rotaryFocused
import com.pidash.companion.ui.theme.DashTheme
import com.pidash.companion.ui.theme.Dimens
import com.pidash.companion.ui.theme.ambientBackground
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.hypot

internal val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
internal val EmphasizedAccelerate = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

internal val DateFormat = DateTimeFormatter.ofPattern("EEEE  ·  d MMMM", Locale.US)
private val RailDateFormat = DateTimeFormatter.ofPattern("EEE d MMM", Locale.US)

/**
 * Compact clock in the navigation rail. Tapping it opens the full-screen [LuxuryClockOverlay];
 * [onPositioned] reports its centre so the overlay can expand from exactly here.
 */
@Composable
fun RailClock(
    onClick: () -> Unit,
    onPositioned: (Offset) -> Unit,
    modifier: Modifier = Modifier,
    timeSource: TimeSource = SystemTimeSource,
) {
    val colors = DashTheme.colors
    val time by rememberTickingTime(timeSource, beatsPerSecond = 1)
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val scale = rememberPressScale(interaction, pressedScale = 0.92f)
    val shape = RoundedCornerShape(Dimens.CardRadiusSmall)

    Column(
        modifier
            .sizeIn(minWidth = 88.dp, minHeight = Dimens.MinTouchTarget)
            .onGloballyPositioned { onPositioned(it.boundsInRoot().center) }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .border(if (focused) 2.dp else 0.dp, if (focused) colors.accentLight else Color.Transparent, shape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClickLabel = "Open full-screen clock",
                onClick = onClick,
            )
            .semantics { rotaryFocused = focused }
            .testTag(TestTags.NAV_CLOCK)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            time.hhmm(),
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraLight, fontSize = 32.sp),
            color = colors.textPrimary,
        )
        Text(
            time.format(RailDateFormat).uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
            color = colors.accent,
            maxLines = 1,
        )
    }
}

/**
 * Full-screen clock. Expands from [LuxuryClockState.origin] with a circular reveal (plus a subtle
 * zoom), and collapses back into it. Swipe up/down (or turn the rotary knob) to change clocks.
 * Tap to reveal the close button; close with it or with Back.
 */
@Composable
fun LuxuryClockOverlay(
    state: LuxuryClockState,
    modifier: Modifier = Modifier,
    weather: Weather? = null,
    timeSource: TimeSource = SystemTimeSource,
) {
    BackHandler(enabled = state.isOpen) { state.close() }

    AnimatedVisibility(
        visible = state.isOpen,
        enter = EnterTransition.None,
        exit = ExitTransition.None,
        modifier = modifier,
    ) {
        val reveal by transition.animateFloat(
            transitionSpec = {
                if (targetState == EnterExitState.Visible) tween(750, easing = EmphasizedDecelerate)
                else tween(450, easing = EmphasizedAccelerate)
            },
            label = "clockReveal",
        ) { if (it == EnterExitState.Visible) 1f else 0f }

        val colors = DashTheme.colors
        Box(
            Modifier
                .fillMaxSize()
                .testTag(TestTags.LUXURY_CLOCK)
                .semantics { revealFraction = reveal }
                .drawWithContent {
                    if (reveal >= 1f) {
                        drawContent() // fully open: no clip, so every frame stays cheap
                    } else {
                        val origin = state.origin.takeIf { it.isSpecified } ?: center
                        val maxRadius = listOf(Offset.Zero, Offset(size.width, 0f), Offset(0f, size.height), Offset(size.width, size.height))
                            .maxOf { hypot(it.x - origin.x, it.y - origin.y) }
                        val circle = Path().apply { addOval(Rect(origin, maxRadius * reveal)) }
                        clipPath(circle) { this@drawWithContent.drawContent() }
                    }
                }
                .ambientBackground(colors)
                .graphicsLayer {
                    val s = 0.92f + 0.08f * reveal
                    scaleX = s
                    scaleY = s
                },
        ) {
            val pagerState = rememberPagerState(initialPage = state.page) { ClockPage.entries.size }
            // Keep LuxuryClockState.page and the pager in sync in both directions:
            // swipes update the state; rotary/showPage() changes scroll the pager.
            LaunchedEffect(pagerState) { snapshotFlow { pagerState.settledPage }.collect { state.page = it } }
            LaunchedEffect(state.page) {
                if (pagerState.currentPage != state.page) pagerState.animateScrollToPage(state.page)
            }
            LuxuryClockScreen(onClose = state::close, weather = weather, timeSource = timeSource, pagerState = pagerState)
        }
    }
}

/** How long the controls stay visible after they appear or the user interacts. */
private const val CONTROLS_AUTO_HIDE_MS = 4_000L

/**
 * The three full-screen clocks in a vertical pager (swipe up for the next one).
 *
 * The controls (close button, page indicator, swipe hint) are hidden so the clocks are truly
 * full screen: a tap anywhere shows them, another tap hides them, and they fade out on their own
 * after [CONTROLS_AUTO_HIDE_MS]. They show briefly on open (if [controlsInitiallyVisible]) so the
 * close button and swipe gesture can be discovered.
 */
@Composable
fun LuxuryClockScreen(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    weather: Weather? = null,
    timeSource: TimeSource = SystemTimeSource,
    pagerState: PagerState = rememberPagerState { ClockPage.entries.size },
    controlsInitiallyVisible: Boolean = true,
) {
    val colors = DashTheme.colors
    // One time source for all pages; only the visible page(s) are composed.
    val timeState = rememberTickingTime(timeSource, beatsPerSecond = 8)
    var controlsVisible by remember { mutableStateOf(controlsInitiallyVisible) }

    // Auto-hide; restarts whenever the controls (re)appear or the page changes.
    // withInfiniteAnimationFrameMillis makes this a no-op under Compose UI tests (deterministic).
    LaunchedEffect(controlsVisible, pagerState.currentPage) {
        if (controlsVisible) {
            withInfiniteAnimationFrameMillis { }
            delay(CONTROLS_AUTO_HIDE_MS)
            controlsVisible = false
        }
    }

    Box(
        modifier
            .fillMaxSize()
            // Tap anywhere toggles the controls (and touches never fall through to the dashboard).
            .pointerInput(Unit) { detectTapGestures { controlsVisible = !controlsVisible } },
    ) {
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize().testTag(TestTags.LUXURY_CLOCK_PAGER),
        ) { page ->
            when (ClockPage.entries[page]) {
                ClockPage.COCKPIT -> CockpitClockPage(timeState)
                ClockPage.RECTANGLE -> RectangleClockPage(timeState)
                ClockPage.WEATHER -> WeatherClockPage(timeState, weather)
            }
        }

        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(tween(250)),
            exit = fadeOut(tween(400)),
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(Modifier.fillMaxSize()) {
                PageIndicator(
                    current = pagerState.currentPage,
                    count = ClockPage.entries.size,
                    modifier = Modifier.align(Alignment.CenterEnd).padding(end = 30.dp),
                )

                if (pagerState.currentPage < ClockPage.entries.lastIndex) {
                    Column(
                        Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(Icons.Outlined.KeyboardArrowUp, contentDescription = null, tint = colors.accent)
                        SectionLabel("Swipe up", color = colors.textSecondary)
                    }
                }

                val closeFocus = remember { FocusRequester() }
                LaunchedEffect(Unit) { closeFocus.requestFocus() } // rotary users land on Close (no-op in touch mode)
                GlowButton(
                    icon = Icons.Outlined.Close,
                    contentDescription = "Close clock",
                    onClick = onClose,
                    shape = CircleShape,
                    iconSize = 26.dp,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(20.dp)
                        .focusRequester(closeFocus)
                        .testTag(TestTags.LUXURY_CLOCK_CLOSE),
                )
            }
        }
    }
}

/** Vertical pill indicator: the active page stretches into a champagne bar. */
@Composable
private fun PageIndicator(current: Int, count: Int, modifier: Modifier = Modifier) {
    val colors = DashTheme.colors
    Column(
        modifier
            .testTag(TestTags.CLOCK_PAGE_INDICATOR)
            .clearAndSetSemantics { contentDescription = "Clock ${current + 1} of $count" },
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        repeat(count) { index ->
            val active = index == current
            val height by animateDpAsState(if (active) 28.dp else 8.dp, label = "dot$index")
            Box(
                Modifier
                    .width(4.dp)
                    .height(height)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (active) colors.accent else colors.glassBorder),
            )
        }
    }
}

