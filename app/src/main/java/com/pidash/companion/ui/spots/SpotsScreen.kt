package com.pidash.companion.ui.spots

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Navigation
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pidash.companion.data.repository.MockSpotsRepository
import com.pidash.companion.domain.model.Spot
import com.pidash.companion.domain.model.SpotCategory
import com.pidash.companion.ui.components.GlowButton
import com.pidash.companion.ui.components.PremiumGlassCard
import com.pidash.companion.ui.components.SectionLabel
import com.pidash.companion.ui.components.TestTags
import com.pidash.companion.ui.components.rememberPressScale
import com.pidash.companion.ui.components.rotaryFocused
import com.pidash.companion.ui.dashboard.icon
import com.pidash.companion.ui.theme.DashTheme
import com.pidash.companion.ui.theme.Dimens
import com.pidash.companion.ui.theme.PiDashTheme
import com.pidash.companion.ui.theme.ambientBackground
import java.util.Locale

@Composable
fun SpotsScreen(
    state: SpotsUiState,
    onCategorySelected: (SpotCategory?) -> Unit,
    onNavigate: (Spot) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = DashTheme.colors
    Column(modifier.fillMaxSize().padding(start = 8.dp, top = 18.dp, end = 18.dp).testTag(TestTags.SPOTS_SCREEN)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text("Favorite Spots", style = MaterialTheme.typography.headlineMedium, color = colors.textPrimary)
            Spacer(Modifier.width(16.dp))
            SectionLabel("${state.spots.size} places nearby", modifier = Modifier.padding(bottom = 6.dp))
        }
        Spacer(Modifier.height(14.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                CategoryChip(
                    label = "All",
                    icon = Icons.Outlined.Apps,
                    selected = state.selectedCategory == null,
                    onClick = { onCategorySelected(null) },
                    modifier = Modifier.testTag(TestTags.CHIP_ALL),
                )
            }
            items(SpotCategory.entries, key = { it.name }) { category ->
                CategoryChip(
                    label = category.label,
                    icon = category.icon(),
                    selected = state.selectedCategory == category,
                    onClick = { onCategorySelected(category) },
                    modifier = Modifier.testTag(TestTags.chip(category)),
                )
            }
        }
        Spacer(Modifier.height(16.dp))

        if (!state.isLoading && state.spots.isEmpty()) {
            Box(Modifier.fillMaxSize().testTag(TestTags.SPOTS_EMPTY), contentAlignment = Alignment.Center) {
                Text("No spots in this category", style = MaterialTheme.typography.titleLarge, color = colors.textSecondary)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().testTag(TestTags.SPOTS_LIST),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 18.dp, start = 2.dp, end = 2.dp, top = 2.dp),
            ) {
                items(state.spots, key = { it.id }) { spot ->
                    SpotCard(spot, onNavigate = { onNavigate(spot) })
                }
            }
        }
    }
}

/** Hairline pill; the selected chip lights up in the accent metal rather than filling solid. */
@Composable
private fun CategoryChip(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = DashTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val scale = rememberPressScale(interaction)
    val tint by animateColorAsState(if (selected) colors.accent else colors.textSecondary, label = "chipTint")
    val fill by animateColorAsState(if (selected) colors.accent.copy(alpha = 0.12f) else Color.Transparent, label = "chipFill")
    val shape = RoundedCornerShape(32.dp)

    Row(
        modifier = modifier
            .heightIn(min = Dimens.MinTouchTarget)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(colors.glassFill)
            .background(fill)
            .border(
                if (focused) 2.dp else 1.dp,
                when {
                    focused -> SolidColor(colors.accentLight)
                    selected -> SolidColor(colors.accent.copy(alpha = 0.7f))
                    else -> colors.panelBorder
                },
                shape,
            )
            .selectable(selected = selected, interactionSource = interaction, indication = null, role = Role.Tab, onClick = onClick)
            .semantics { rotaryFocused = focused }
            .padding(horizontal = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(10.dp))
        Text(label.uppercase(), style = MaterialTheme.typography.labelLarge, color = if (selected) colors.accent else colors.textPrimary)
    }
}

@Composable
private fun SpotCard(spot: Spot, onNavigate: () -> Unit) {
    val colors = DashTheme.colors
    PremiumGlassCard(
        modifier = Modifier.fillMaxWidth().testTag(TestTags.spotCard(spot.id)),
        shape = RoundedCornerShape(Dimens.CardRadiusSmall),
        contentPadding = PaddingValues(start = 18.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
        elevation = 8.dp,
        onClick = onNavigate,
        onClickLabel = "Navigate to ${spot.name}",
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(52.dp).border(1.dp, colors.accent.copy(alpha = 0.45f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(spot.category.icon(), contentDescription = spot.category.label, tint = colors.accent, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(18.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        spot.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (spot.isFavorite) {
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Rounded.Star, contentDescription = "Favorite", tint = colors.accent, modifier = Modifier.size(18.dp))
                    }
                }
                Text(spot.address, style = MaterialTheme.typography.bodyLarge, color = colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(16.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(String.format(Locale.US, "%.1f", spot.distanceKm), style = MaterialTheme.typography.headlineMedium, color = colors.textPrimary)
                Spacer(Modifier.width(4.dp))
                Text("km", style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary, modifier = Modifier.padding(bottom = 4.dp))
            }
            Spacer(Modifier.width(20.dp))
            GlowButton(
                icon = Icons.Outlined.Navigation,
                contentDescription = "Navigate to ${spot.name}",
                onClick = onNavigate,
                filled = true,
                shape = CircleShape,
                iconSize = 26.dp,
                modifier = Modifier.testTag(TestTags.spotNavigate(spot.id)),
            )
        }
    }
}

@Preview(widthDp = 960, heightDp = 640)
@Composable
private fun SpotsPreview() {
    PiDashTheme {
        Box(Modifier.fillMaxSize().ambientBackground(DashTheme.colors)) {
            SpotsScreen(SpotsUiState(spots = MockSpotsRepository.SAMPLE_SPOTS, isLoading = false), {}, {})
        }
    }
}
