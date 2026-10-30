package com.pidash.companion.ui.clock

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pidash.companion.domain.model.Weather
import com.pidash.companion.ui.components.SectionLabel
import com.pidash.companion.ui.components.TestTags
import com.pidash.companion.ui.dashboard.icon
import com.pidash.companion.ui.theme.DashTheme
import java.time.LocalDateTime
import java.util.Locale

/**
 * Page 3 — full-screen digital clock with location and weather: oversized thin digits on the left,
 * a hairline divider, and location / condition / temperature / humidity on the right.
 */
@Composable
internal fun WeatherClockPage(timeState: State<LocalDateTime>, weather: Weather?, modifier: Modifier = Modifier) {
    val colors = DashTheme.colors
    val hhmm by remember { derivedStateOf { timeState.value.hhmm() } }
    val second by remember { derivedStateOf { timeState.value.second } }
    val colonOn by remember { derivedStateOf { timeState.value.nano < 500_000_000 } }
    val date by remember { derivedStateOf { timeState.value.format(DateFormat).uppercase() } }

    val intro = remember { Animatable(0f) }
    LaunchedEffect(Unit) { intro.animateTo(1f, tween(1200, delayMillis = 100, easing = EmphasizedDecelerate)) }

    Row(
        modifier.fillMaxSize().testTag(TestTags.CLOCK_PAGE_WEATHER),
        horizontalArrangement = Arrangement.spacedBy(32.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Time
        Column {
            SectionLabel("Local time", color = colors.accent, style = LabelStyle)
            Box(
                Modifier
                    .testTag(TestTags.CLOCK_WEATHER_TIME)
                    .clearAndSetSemantics {
                        text = AnnotatedString(hhmm)
                        contentDescription = "Time $hhmm"
                    },
            ) {
                Digits(hhmm = hhmm, colonOn = colonOn, intro = { intro.value + 0.3f }, fontSize = 250.sp, metallic = true)
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.graphicsLayer { alpha = intro.value }) {
                SectionLabel(date, color = colors.accent, style = DateStyle.metallicGold(colors))
                Spacer(Modifier.width(18.dp))
                Text(
                    "%02d".format(Locale.US, second),
                    style = MaterialTheme.typography.headlineMedium.copy(fontSize = 40.sp, fontWeight = FontWeight.Light, fontFeatureSettings = "tnum").metallicGold(colors),
                    color = colors.accent,
                )
            }
        }

        // Champagne hairline divider, fading at both ends
        Box(
            Modifier
                .width(1.dp)
                .height(280.dp)
                .graphicsLayer { scaleY = intro.value }
                .background(Brush.verticalGradient(listOf(Color.Transparent, colors.accent.copy(alpha = 0.7f), Color.Transparent))),
        )

        // Location + weather
        Column(
            Modifier
                .width(300.dp)
                .graphicsLayer {
                    alpha = intro.value
                    translationX = (1f - intro.value) * 32.dp.toPx()
                },
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Place, contentDescription = null, tint = colors.accent, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(8.dp))
                SectionLabel("Location", color = colors.textTertiary, style = LabelStyle)
            }
            Text(
                weather?.locationName ?: "Locating…",
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 38.sp),
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTag(TestTags.CLOCK_WEATHER_LOCATION),
            )
            Spacer(Modifier.height(22.dp))
            if (weather == null) {
                Text("Weather unavailable", style = MaterialTheme.typography.titleLarge.copy(fontSize = 26.sp), color = colors.textSecondary)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        weather.condition.icon(),
                        contentDescription = weather.condition.label,
                        tint = colors.accent,
                        modifier = Modifier.size(76.dp),
                    )
                    Spacer(Modifier.width(18.dp))
                    Text(
                        "${weather.temperatureCelsius}°",
                        style = MaterialTheme.typography.displayMedium.copy(fontSize = 92.sp),
                        color = colors.textPrimary,
                        modifier = Modifier.testTag(TestTags.CLOCK_WEATHER_TEMPERATURE),
                    )
                }
                Text(
                    weather.condition.label,
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 28.sp),
                    color = colors.textPrimary,
                    modifier = Modifier.testTag(TestTags.CLOCK_WEATHER_CONDITION),
                )
                Spacer(Modifier.height(4.dp))
                SectionLabel("Humidity ${weather.humidityPercent}%", color = colors.textSecondary, style = DetailStyle)
            }
        }
    }
}

/** Larger caption styles for this page: legible at a glance on the 7" head-unit screen. */
private val LabelStyle = TextStyle(fontWeight = FontWeight.Medium, fontSize = 18.sp, letterSpacing = 3.sp)
private val DetailStyle = TextStyle(fontWeight = FontWeight.Medium, fontSize = 20.sp, letterSpacing = 2.sp)
private val DateStyle = TextStyle(fontWeight = FontWeight.Light, fontSize = 34.sp, letterSpacing = 1.5.sp)
