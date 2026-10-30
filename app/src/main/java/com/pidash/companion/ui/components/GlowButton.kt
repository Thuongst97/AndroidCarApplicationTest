package com.pidash.companion.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pidash.companion.ui.theme.DashTheme
import com.pidash.companion.ui.theme.Dimens

/**
 * Tactile luxury control. Always at least 64dp x 64dp. At rest it is a quiet hairline outline; when
 * [active] it lights up in [activeColor] with a soft halo (breathing if [pulse]); [filled] renders a
 * brushed-metal face for the primary action. Shows an accent ring when focused by a rotary controller.
 */
@Composable
fun GlowButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    active: Boolean = false,
    activeColor: Color = DashTheme.colors.accent,
    size: Dp = Dimens.MinTouchTarget,
    iconSize: Dp = 28.dp,
    shape: Shape = RoundedCornerShape(Dimens.CardRadiusSmall),
    stateDescription: String? = null,
    pulse: Boolean = active,
    filled: Boolean = false,
    /** Tint the icon with [activeColor] even when inactive (e.g. SOS stays red). */
    tintIcon: Boolean = false,
) {
    val colors = DashTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val scale = rememberPressScale(interaction, pressedScale = 0.92f)

    val haloAlpha = when {
        pulse -> rememberInfiniteTransition(label = "halo").animateFloat(
            initialValue = 0.18f,
            targetValue = 0.42f,
            animationSpec = infiniteRepeatable(tween(1800), RepeatMode.Reverse),
            label = "haloAlpha",
        ).value
        active -> 0.3f
        else -> 0f
    }

    val contentColor by animateColorAsState(
        when {
            filled -> colors.onAccent
            active || tintIcon -> activeColor
            else -> colors.textPrimary
        },
        label = "contentColor",
    )
    val border = when {
        focused -> SolidColor(colors.accentLight)
        active || tintIcon -> SolidColor(activeColor.copy(alpha = if (active) 0.75f else 0.4f))
        filled -> SolidColor(colors.accentLight.copy(alpha = 0.6f))
        else -> colors.panelBorder
    }
    val face = when {
        filled -> Brush.linearGradient(listOf(colors.accentLight, colors.accent, colors.accent.copy(alpha = 0.85f)))
        active -> Brush.radialGradient(listOf(activeColor.copy(alpha = 0.22f), activeColor.copy(alpha = 0.04f)))
        else -> colors.glassFill
    }

    Box(
        modifier = modifier
            .sizeIn(minWidth = size, minHeight = size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .drawBehind {
                if (haloAlpha > 0f) {
                    // Halo bleeds past the bounds so it reads as light, not as a filled box.
                    val radius = this.size.maxDimension * 0.85f
                    drawCircle(
                        Brush.radialGradient(listOf(activeColor.copy(alpha = haloAlpha), Color.Transparent), center = center, radius = radius),
                        radius = radius,
                    )
                }
            }
            .clip(shape)
            .background(face)
            .border(if (focused) 2.dp else 1.dp, border, shape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .semantics {
                glowActive = active
                rotaryFocused = focused
                if (stateDescription != null) this.stateDescription = stateDescription
            }
            .padding(horizontal = 4.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(icon, contentDescription = contentDescription, tint = contentColor, modifier = Modifier.size(iconSize))
            if (label != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = label.uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (active || filled) contentColor else colors.textSecondary,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }
    }
}
