package com.pidash.companion.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pidash.companion.ui.theme.DashTheme
import com.pidash.companion.ui.theme.Dimens

/**
 * Luxury panel: smoked-glass vertical gradient, a hairline border that is brighter along the top
 * edge (specular highlight) and a deep neutral shadow. When [onClick] is set it becomes a tactile,
 * rotary-focusable surface.
 */
@Composable
fun PremiumGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(Dimens.CardRadius),
    contentPadding: PaddingValues = PaddingValues(20.dp),
    elevation: Dp = 18.dp,
    glowColor: Color = Color.Black,
    onClick: (() -> Unit)? = null,
    onClickLabel: String? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = DashTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val scale = rememberPressScale(interaction, pressedScale = 0.97f)

    val clickModifier = if (onClick != null) {
        Modifier
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClickLabel = onClickLabel,
                onClick = onClick,
            )
            .semantics { rotaryFocused = focused }
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = elevation,
                shape = shape,
                ambientColor = if (colors.isDark) glowColor else glowColor.copy(alpha = 0.25f),
                spotColor = if (colors.isDark) glowColor else glowColor.copy(alpha = 0.25f),
            )
            .clip(shape)
            .background(colors.glassFill)
            .border(
                width = if (focused) 2.dp else 1.dp,
                brush = if (focused) SolidColor(colors.accent) else colors.panelBorder,
                shape = shape,
            )
            .then(clickModifier)
            .padding(contentPadding),
        content = content,
    )
}

/** Small tracked-uppercase caption used as a section header, instrument-cluster style. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, color: Color = DashTheme.colors.textTertiary) {
    Text(text.uppercase(), style = MaterialTheme.typography.labelMedium, color = color, modifier = modifier, maxLines = 1)
}

/** Spring-based "press down" feedback shared by all tappable components. */
@Composable
fun rememberPressScale(interaction: MutableInteractionSource, pressedScale: Float = 0.95f): Float {
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "pressScale",
    )
    return scale
}
