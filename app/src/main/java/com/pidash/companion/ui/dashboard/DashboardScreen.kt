package com.pidash.companion.ui.dashboard

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeOff
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.EmergencyShare
import androidx.compose.material.icons.outlined.FilterDrama
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.LocalGasStation
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Thunderstorm
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.Weekend
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pidash.companion.domain.model.MediaState
import com.pidash.companion.domain.model.SpotCategory
import com.pidash.companion.domain.model.TripData
import com.pidash.companion.domain.model.Weather
import com.pidash.companion.domain.model.WeatherCondition
import com.pidash.companion.ui.components.AlbumArtwork
import com.pidash.companion.ui.components.GlowButton
import com.pidash.companion.ui.components.NeonArcGauge
import com.pidash.companion.ui.components.PremiumGlassCard
import com.pidash.companion.ui.components.SectionLabel
import com.pidash.companion.ui.components.TestTags
import com.pidash.companion.ui.components.WaveVisualizer
import com.pidash.companion.ui.components.speedAlert
import com.pidash.companion.ui.components.speedKmh
import com.pidash.companion.ui.theme.DashTheme
import com.pidash.companion.ui.theme.PiDashTheme
import com.pidash.companion.ui.theme.ambientBackground
import java.util.Locale

/** Callbacks for every dashboard interaction; the screen itself holds no logic. */
data class DashboardActions(
    val onHomeNavigation: () -> Unit = {},
    val onEmergencyContact: () -> Unit = {},
    val onToggleMute: () -> Unit = {},
    val onToggleTheme: () -> Unit = {},
    val onQuickStop: (SpotCategory) -> Unit = {},
    val onTogglePlayPause: () -> Unit = {},
    val onOpenMedia: () -> Unit = {},
)

@Composable
fun DashboardScreen(state: DashboardUiState, actions: DashboardActions, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(start = 8.dp, top = 18.dp, end = 18.dp, bottom = 18.dp)
            .testTag(TestTags.DASHBOARD_SCREEN),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        SpeedometerCard(state.trip, Modifier.weight(1f).fillMaxHeight())
        Column(Modifier.weight(1.15f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                WeatherCard(state.weather, Modifier.weight(1f).fillMaxHeight())
                MiniPlayerCard(state.media, actions.onTogglePlayPause, actions.onOpenMedia, Modifier.weight(1f).fillMaxHeight())
            }
            QuickActionsCard(state, actions, Modifier.fillMaxWidth())
            QuickStopsCard(actions.onQuickStop, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun SpeedometerCard(trip: TripData, modifier: Modifier) {
    val colors = DashTheme.colors
    val animatedSpeed by animateIntAsState(trip.speedKmh, tween(500), label = "speed")
    PremiumGlassCard(modifier, contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SectionLabel("Drive")
                SectionLabel(if (trip.isSpeeding) "Over limit" else "Cruise", color = if (trip.isSpeeding) colors.danger else colors.accent)
            }
            NeonArcGauge(
                value = trip.speedKmh.toFloat(),
                maxValue = 180f,
                warnValue = trip.speedLimitKmh.toFloat(),
                modifier = Modifier
                    .weight(1f)
                    .testTag(TestTags.SPEED_GAUGE)
                    .semantics {
                        speedKmh = trip.speedKmh
                        speedAlert = trip.isSpeeding
                        contentDescription = "Speed ${trip.speedKmh} kilometers per hour"
                    },
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = animatedSpeed.toString(),
                        style = MaterialTheme.typography.displayLarge,
                        color = if (trip.isSpeeding) colors.danger else colors.textPrimary,
                        modifier = Modifier.testTag(TestTags.SPEED_VALUE),
                    )
                    SectionLabel("km/h", color = colors.textSecondary)
                    Spacer(Modifier.height(14.dp))
                    SpeedLimitBadge(trip.speedLimitKmh, trip.isSpeeding)
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TripStat("Trip", String.format(Locale.US, "%.1f", trip.tripDistanceKm), "km", Modifier.weight(1f).testTag(TestTags.TRIP_DISTANCE))
                StatDivider()
                TripStat("Average", "${trip.averageSpeedKmh}", "km/h", Modifier.weight(1f).testTag(TestTags.TRIP_AVERAGE))
                StatDivider()
                TripStat("Time", formatDuration(trip.elapsedSeconds), "h", Modifier.weight(1f))
            }
        }
    }
}

/** Minimal European-style limit sign: hairline red ring, thin numerals. */
@Composable
private fun SpeedLimitBadge(limit: Int, speeding: Boolean) {
    val colors = DashTheme.colors
    Box(
        Modifier
            .size(46.dp)
            .border(if (speeding) 3.dp else 2.dp, colors.danger.copy(alpha = if (speeding) 1f else 0.85f), CircleShape)
            .testTag(TestTags.SPEED_LIMIT)
            .semantics { contentDescription = "Speed limit $limit" },
        contentAlignment = Alignment.Center,
    ) {
        Text(limit.toString(), color = colors.textPrimary, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun TripStat(label: String, value: String, unit: String, modifier: Modifier = Modifier) {
    val colors = DashTheme.colors
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        SectionLabel(label)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, style = MaterialTheme.typography.headlineSmall, color = colors.textPrimary)
            Spacer(Modifier.width(4.dp))
            Text(unit, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary, modifier = Modifier.padding(bottom = 3.dp))
        }
    }
}

@Composable
private fun StatDivider() {
    Box(Modifier.width(1.dp).height(32.dp).background(DashTheme.colors.hairline))
}

@Composable
private fun WeatherCard(weather: Weather?, modifier: Modifier) {
    val colors = DashTheme.colors
    PremiumGlassCard(modifier.testTag(TestTags.WEATHER_CARD)) {
        if (weather == null) {
            Text("Loading weather…", color = colors.textSecondary, modifier = Modifier.align(Alignment.Center))
            return@PremiumGlassCard
        }
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            SectionLabel("Outside")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${weather.temperatureCelsius}°",
                    style = MaterialTheme.typography.displayMedium,
                    color = colors.textPrimary,
                    modifier = Modifier.testTag(TestTags.WEATHER_TEMPERATURE),
                )
                Spacer(Modifier.weight(1f))
                Icon(
                    weather.condition.icon(),
                    contentDescription = weather.condition.label,
                    tint = colors.accent,
                    modifier = Modifier.size(40.dp).testTag(TestTags.WEATHER_ICON),
                )
            }
            Column {
                Text(
                    weather.locationName,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag(TestTags.WEATHER_LOCATION),
                )
                Text(
                    "${weather.condition.label} · ${weather.humidityPercent}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun MiniPlayerCard(media: MediaState, onTogglePlayPause: () -> Unit, onOpen: () -> Unit, modifier: Modifier) {
    val colors = DashTheme.colors
    PremiumGlassCard(
        modifier.testTag(TestTags.MINI_PLAYER),
        onClick = onOpen,
        onClickLabel = "Open media player",
        contentPadding = PaddingValues(18.dp),
    ) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            SectionLabel("Now playing")
            Row(verticalAlignment = Alignment.CenterVertically) {
                AlbumArtwork(media.track, RoundedCornerShape(14.dp), Modifier.size(56.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        media.track?.title ?: "Nothing playing",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        media.track?.artist.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            WaveVisualizer(isPlaying = media.isPlaying, color = colors.accent, bars = 28, modifier = Modifier.height(22.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                HairlineProgress(media.progress, Modifier.weight(1f))
                Spacer(Modifier.width(12.dp))
                GlowButton(
                    icon = if (media.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (media.isPlaying) "Pause" else "Play",
                    onClick = onTogglePlayPause,
                    active = media.isPlaying,
                    shape = CircleShape,
                )
            }
        }
    }
}

@Composable
private fun HairlineProgress(progress: Float, modifier: Modifier) {
    val colors = DashTheme.colors
    Box(
        modifier.height(4.dp).drawBehind {
            val y = size.height / 2
            drawLine(colors.hairline, Offset(0f, y), Offset(size.width, y), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
            if (progress > 0f) {
                drawLine(
                    Brush.horizontalGradient(listOf(colors.accent.copy(alpha = 0.4f), colors.accentLight), endX = size.width * progress),
                    Offset(0f, y),
                    Offset(size.width * progress, y),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }
        },
    )
}

@Composable
private fun QuickActionsCard(state: DashboardUiState, actions: DashboardActions, modifier: Modifier) {
    val colors = DashTheme.colors
    val muted = state.settings.isMuted
    val dark = state.settings.isDarkTheme
    PremiumGlassCard(modifier, contentPadding = PaddingValues(14.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GlowButton(
                icon = Icons.Outlined.Home,
                contentDescription = "Navigate home",
                label = "Home",
                onClick = actions.onHomeNavigation,
                modifier = Modifier.weight(1f).height(92.dp).testTag(TestTags.ACTION_HOME),
            )
            GlowButton(
                icon = Icons.Outlined.EmergencyShare,
                contentDescription = "Call emergency contact",
                label = "SOS",
                onClick = actions.onEmergencyContact,
                activeColor = colors.danger,
                tintIcon = true,
                modifier = Modifier.weight(1f).height(92.dp).testTag(TestTags.ACTION_EMERGENCY),
            )
            GlowButton(
                icon = if (muted) Icons.AutoMirrored.Outlined.VolumeOff else Icons.AutoMirrored.Outlined.VolumeUp,
                contentDescription = if (muted) "Unmute audio" else "Mute audio",
                label = if (muted) "Muted" else "Sound",
                onClick = actions.onToggleMute,
                active = muted,
                activeColor = colors.warning,
                stateDescription = if (muted) "Muted" else "Sound on",
                modifier = Modifier.weight(1f).height(92.dp).testTag(TestTags.ACTION_MUTE),
            )
            GlowButton(
                icon = if (dark) Icons.Outlined.DarkMode else Icons.Outlined.LightMode,
                contentDescription = if (dark) "Switch to day theme" else "Switch to night theme",
                label = if (dark) "Night" else "Day",
                onClick = actions.onToggleTheme,
                stateDescription = if (dark) "Night theme" else "Day theme",
                modifier = Modifier.weight(1f).height(92.dp).testTag(TestTags.ACTION_THEME),
            )
        }
    }
}

@Composable
private fun QuickStopsCard(onQuickStop: (SpotCategory) -> Unit, modifier: Modifier) {
    PremiumGlassCard(modifier, contentPadding = PaddingValues(14.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf(SpotCategory.COFFEE, SpotCategory.GAS_STATION, SpotCategory.REST_STOP, SpotCategory.FOOD).forEach { category ->
                GlowButton(
                    icon = category.icon(),
                    contentDescription = "Find nearby ${category.label}",
                    label = category.shortLabel(),
                    onClick = { onQuickStop(category) },
                    modifier = Modifier.weight(1f).height(80.dp).testTag(TestTags.quickStop(category)),
                    iconSize = 26.dp,
                )
            }
        }
    }
}

fun SpotCategory.icon(): ImageVector = when (this) {
    SpotCategory.COFFEE -> Icons.Outlined.LocalCafe
    SpotCategory.GAS_STATION -> Icons.Outlined.LocalGasStation
    SpotCategory.REST_STOP -> Icons.Outlined.Weekend
    SpotCategory.FOOD -> Icons.Outlined.Restaurant
}

private fun SpotCategory.shortLabel() = when (this) {
    SpotCategory.GAS_STATION -> "Fuel"
    SpotCategory.REST_STOP -> "Rest"
    else -> label
}

private fun WeatherCondition.icon(): ImageVector = when (this) {
    WeatherCondition.CLEAR -> Icons.Outlined.WbSunny
    WeatherCondition.PARTLY_CLOUDY -> Icons.Outlined.FilterDrama
    WeatherCondition.CLOUDY -> Icons.Outlined.Cloud
    WeatherCondition.RAIN -> Icons.Outlined.WaterDrop
    WeatherCondition.STORM -> Icons.Outlined.Thunderstorm
    WeatherCondition.NIGHT_CLEAR -> Icons.Outlined.NightsStay
}

private fun formatDuration(seconds: Long): String =
    String.format(Locale.US, "%d:%02d", seconds / 3600, (seconds % 3600) / 60)

@Preview(widthDp = 960, heightDp = 640)
@Composable
private fun DashboardPreview() {
    PiDashTheme {
        Box(Modifier.fillMaxSize().ambientBackground(DashTheme.colors)) {
            DashboardScreen(
                state = DashboardUiState(
                    trip = TripData(speedKmh = 72, speedLimitKmh = 80, tripDistanceKm = 12.4, averageSpeedKmh = 46, elapsedSeconds = 980),
                    weather = Weather(29, WeatherCondition.CLEAR, "Da Nang City", 70),
                ),
                actions = DashboardActions(),
            )
        }
    }
}
