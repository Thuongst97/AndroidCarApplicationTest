package com.pidash.companion.ui.media

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pidash.companion.data.source.MockMediaCatalog
import com.pidash.companion.domain.model.MediaState
import com.pidash.companion.ui.components.AlbumArtwork
import com.pidash.companion.ui.components.GlowButton
import com.pidash.companion.ui.components.SectionLabel
import com.pidash.companion.ui.components.TestTags
import com.pidash.companion.ui.components.WaveVisualizer
import com.pidash.companion.ui.components.artworkBrush
import com.pidash.companion.ui.theme.DashTheme
import com.pidash.companion.ui.theme.PiDashTheme
import com.pidash.companion.ui.theme.ambientBackground
import java.util.Locale

data class MediaActions(
    val onTogglePlayPause: () -> Unit = {},
    val onSkipNext: () -> Unit = {},
    val onSkipPrevious: () -> Unit = {},
    val onSeek: (Float) -> Unit = {},
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaScreen(state: MediaState, actions: MediaActions, modifier: Modifier = Modifier) {
    val colors = DashTheme.colors
    val track = state.track
    // Local drag state so the thumb follows the finger without seeking on every frame.
    var dragFraction by remember { mutableStateOf<Float?>(null) }
    val shownFraction = dragFraction ?: state.progress
    val durationMs = track?.durationMs ?: 0L

    Box(modifier.fillMaxSize().testTag(TestTags.MEDIA_SCREEN)) {
        // Album colours bleed softly into the cabin, like ambient lighting matched to the music.
        if (track != null) {
            Box(
                Modifier
                    .fillMaxSize()
                    .blur(90.dp)
                    .background(track.artworkBrush(), alpha = if (colors.isDark) 0.5f else 0.3f),
            )
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        if (colors.isDark) {
                            listOf(Color(0x5505070A), Color(0xC005070A), Color(0xF205070A))
                        } else {
                            listOf(Color(0x55F5F2EC), Color(0xC0F5F2EC), Color(0xF2F5F2EC))
                        },
                    ),
                ),
        )

        Row(
            Modifier.fillMaxSize().padding(start = 16.dp, end = 36.dp, top = 28.dp, bottom = 28.dp),
            horizontalArrangement = Arrangement.spacedBy(44.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AlbumArtwork(
                track = track,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxHeight(0.86f)
                    .aspectRatio(1f)
                    .shadow(40.dp, RoundedCornerShape(24.dp), ambientColor = Color.Black, spotColor = Color.Black)
                    .border(1.dp, colors.panelBorder, RoundedCornerShape(24.dp))
                    .testTag(TestTags.MEDIA_ARTWORK),
            )

            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SectionLabel("Now playing", color = colors.accent)
                Spacer(Modifier.height(2.dp))
                Text(
                    track?.title ?: "Nothing playing",
                    style = MaterialTheme.typography.displaySmall,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag(TestTags.MEDIA_TITLE),
                )
                Text(
                    listOfNotNull(track?.artist, track?.album).joinToString(" · "),
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag(TestTags.MEDIA_ARTIST),
                )

                WaveVisualizer(
                    isPlaying = state.isPlaying,
                    color = colors.accent,
                    bars = 48,
                    modifier = Modifier.padding(top = 18.dp).height(44.dp).testTag(TestTags.MEDIA_VISUALIZER),
                )

                Slider(
                    value = shownFraction,
                    onValueChange = { dragFraction = it },
                    onValueChangeFinished = {
                        dragFraction?.let(actions.onSeek)
                        dragFraction = null
                    },
                    thumb = {
                        Box(
                            Modifier
                                .size(18.dp)
                                .drawBehind {
                                    drawCircle(
                                        Brush.radialGradient(listOf(colors.accent.copy(alpha = 0.5f), Color.Transparent)),
                                        radius = size.minDimension,
                                    )
                                }
                                .background(colors.accentLight, CircleShape),
                        )
                    },
                    track = {
                        Box(
                            Modifier.fillMaxWidth().height(4.dp).drawBehind {
                                val y = size.height / 2
                                val end = size.width * shownFraction
                                drawLine(colors.hairline, Offset(0f, y), Offset(size.width, y), 3.dp.toPx(), StrokeCap.Round)
                                if (end > 0f) {
                                    drawLine(
                                        Brush.horizontalGradient(listOf(colors.accent.copy(alpha = 0.35f), colors.accentLight), endX = end),
                                        Offset(0f, y),
                                        Offset(end, y),
                                        3.dp.toPx(),
                                        StrokeCap.Round,
                                    )
                                }
                            },
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .testTag(TestTags.MEDIA_SLIDER)
                        .semantics { contentDescription = "Track progress" },
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        formatTime((durationMs * shownFraction).toLong()),
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.textSecondary,
                        modifier = Modifier.testTag(TestTags.MEDIA_POSITION),
                    )
                    Text(
                        formatTime(durationMs),
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.textTertiary,
                        modifier = Modifier.testTag(TestTags.MEDIA_DURATION),
                    )
                }

                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(32.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    GlowButton(
                        icon = Icons.Rounded.SkipPrevious,
                        contentDescription = "Previous track",
                        onClick = actions.onSkipPrevious,
                        size = 80.dp,
                        iconSize = 36.dp,
                        shape = CircleShape,
                        modifier = Modifier.testTag(TestTags.MEDIA_PREVIOUS),
                    )
                    GlowButton(
                        icon = if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (state.isPlaying) "Pause" else "Play",
                        onClick = actions.onTogglePlayPause,
                        active = state.isPlaying,
                        filled = true,
                        size = 104.dp,
                        iconSize = 52.dp,
                        shape = CircleShape,
                        stateDescription = if (state.isPlaying) "Playing" else "Paused",
                        modifier = Modifier.testTag(TestTags.MEDIA_PLAY_PAUSE),
                    )
                    GlowButton(
                        icon = Icons.Rounded.SkipNext,
                        contentDescription = "Next track",
                        onClick = actions.onSkipNext,
                        size = 80.dp,
                        iconSize = 36.dp,
                        shape = CircleShape,
                        modifier = Modifier.testTag(TestTags.MEDIA_NEXT),
                    )
                }
            }
        }
    }
}

internal fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    return String.format(Locale.US, "%d:%02d", totalSeconds / 60, totalSeconds % 60)
}

@Preview(widthDp = 960, heightDp = 640)
@Composable
private fun MediaPreview() {
    PiDashTheme {
        Box(Modifier.fillMaxSize().ambientBackground(DashTheme.colors)) {
            MediaScreen(MediaState(MockMediaCatalog.tracks.first(), isPlaying = true, positionMs = 64_000), MediaActions())
        }
    }
}
