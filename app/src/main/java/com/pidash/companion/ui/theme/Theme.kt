package com.pidash.companion.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// "Midnight & Champagne" — restrained luxury palette: deep blacks, platinum text, one warm metal accent.
val Midnight950 = Color(0xFF05070A)
val Midnight900 = Color(0xFF0A0D12)
val Midnight800 = Color(0xFF11151C)
val Midnight700 = Color(0xFF1A1F28)
val Champagne = Color(0xFFD9BC8C)
val ChampagneLight = Color(0xFFF1DDB8)
val ChampagneDeep = Color(0xFFA8875A)
val IceBlue = Color(0xFF9ADCFF)
val Platinum = Color(0xFFF4F1EA)
val Silver = Color(0xFF9A9EA6)
val Graphite = Color(0xFF5F646D)
val SoftAmber = Color(0xFFFFB547)
val SoftRed = Color(0xFFFF5A5F)

// "Ivory & Bronze" day theme
val Ivory = Color(0xFFF5F2EC)
val Parchment = Color(0xFFE8E2D6)
val Bronze = Color(0xFF8C6A3E)
val Ink = Color(0xFF16181D)

/** Tokens Material3 does not model: panels, hairlines, ambient lighting and accent metals. */
@Immutable
data class DashColors(
    val isDark: Boolean,
    val background: Brush,
    /** Soft light pools painted over the background, like interior ambient lighting. */
    val ambientWarm: Color,
    val ambientCool: Color,
    val glassFill: Brush,
    /** Hairline border, brighter at the top edge to suggest a specular highlight. */
    val panelBorder: Brush,
    val glassBorder: Color,
    val hairline: Color,
    val accent: Color,
    val accentLight: Color,
    val accentSecondary: Color,
    val onAccent: Color,
    val warning: Color,
    val danger: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
)

private val DarkDashColors = DashColors(
    isDark = true,
    background = Brush.verticalGradient(listOf(Midnight800, Midnight900, Midnight950)),
    ambientWarm = Champagne.copy(alpha = 0.07f),
    ambientCool = IceBlue.copy(alpha = 0.05f),
    glassFill = Brush.verticalGradient(listOf(Color(0xE61A1F28), Color(0xE60E1117))),
    panelBorder = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.16f), Color.White.copy(alpha = 0.03f))),
    glassBorder = Color.White.copy(alpha = 0.10f),
    hairline = Color.White.copy(alpha = 0.07f),
    accent = Champagne,
    accentLight = ChampagneLight,
    accentSecondary = IceBlue,
    onAccent = Midnight900,
    warning = SoftAmber,
    danger = SoftRed,
    textPrimary = Platinum,
    textSecondary = Silver,
    textTertiary = Graphite,
)

private val LightDashColors = DashColors(
    isDark = false,
    background = Brush.verticalGradient(listOf(Color(0xFFFBF9F5), Ivory, Parchment)),
    ambientWarm = Bronze.copy(alpha = 0.08f),
    ambientCool = Color(0xFF2F6F9F).copy(alpha = 0.05f),
    glassFill = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.92f), Color(0xFFFBF9F5).copy(alpha = 0.85f))),
    panelBorder = Brush.verticalGradient(listOf(Color.White, Ink.copy(alpha = 0.10f))),
    glassBorder = Ink.copy(alpha = 0.12f),
    hairline = Ink.copy(alpha = 0.08f),
    accent = Bronze,
    accentLight = Color(0xFFB08A55),
    accentSecondary = Color(0xFF2F6F9F),
    onAccent = Color.White,
    warning = Color(0xFFB86E00),
    danger = Color(0xFFC62E3A),
    textPrimary = Ink,
    textSecondary = Color(0xFF5E626B),
    textTertiary = Color(0xFF9A9CA2),
)

val LocalDashColors = staticCompositionLocalOf { DarkDashColors }

/** Roboto ships every weight; thin numerals + tracked uppercase labels give the instrument-cluster look. */
private val Sans = FontFamily.SansSerif

private val DashTypography = Typography(
    displayLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Thin, fontSize = 124.sp, letterSpacing = (-5).sp),
    displayMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.ExtraLight, fontSize = 60.sp, letterSpacing = (-2).sp),
    displaySmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Light, fontSize = 40.sp, letterSpacing = (-1).sp),
    headlineMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Light, fontSize = 28.sp),
    headlineSmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Light, fontSize = 24.sp),
    titleLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 20.sp),
    titleMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 17.sp),
    bodyLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 16.sp),
    bodyMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 14.sp),
    labelLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 12.sp, letterSpacing = 1.8.sp),
    labelMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 12.sp, letterSpacing = 2.6.sp),
    labelSmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 10.sp, letterSpacing = 2.sp),
)

object Dimens {
    /** Automotive minimum touch target (spec section 4.1). */
    val MinTouchTarget = 64.dp
    val CardRadius = 28.dp
    val CardRadiusSmall = 20.dp
}

@Composable
fun PiDashTheme(darkTheme: Boolean = true, content: @Composable () -> Unit) {
    val dash = if (darkTheme) DarkDashColors else LightDashColors
    val scheme = if (darkTheme) {
        darkColorScheme(
            primary = Champagne,
            secondary = IceBlue,
            tertiary = SoftAmber,
            background = Midnight900,
            surface = Midnight800,
            onPrimary = Midnight900,
            onBackground = Platinum,
            onSurface = Platinum,
            onSurfaceVariant = Silver,
            error = SoftRed,
            inverseSurface = Midnight700,
            inverseOnSurface = Platinum,
        )
    } else {
        lightColorScheme(
            primary = Bronze,
            secondary = Color(0xFF2F6F9F),
            tertiary = Color(0xFFB86E00),
            background = Ivory,
            surface = Color.White,
            onPrimary = Color.White,
            onBackground = Ink,
            onSurface = Ink,
            onSurfaceVariant = Color(0xFF5E626B),
            inverseSurface = Ink,
            inverseOnSurface = Ivory,
        )
    }
    CompositionLocalProvider(LocalDashColors provides dash) {
        MaterialTheme(colorScheme = scheme, typography = DashTypography, content = content)
    }
}

object DashTheme {
    val colors: DashColors
        @Composable get() = LocalDashColors.current
}

/** Background gradient plus two soft light pools (warm top-left, cool bottom-right). */
fun Modifier.ambientBackground(colors: DashColors): Modifier = drawBehind {
    drawRect(colors.background)
    drawCircle(
        Brush.radialGradient(listOf(colors.ambientWarm, Color.Transparent), center = Offset(size.width * 0.18f, 0f), radius = size.maxDimension * 0.55f),
        radius = size.maxDimension * 0.55f,
        center = Offset(size.width * 0.18f, 0f),
    )
    drawCircle(
        Brush.radialGradient(listOf(colors.ambientCool, Color.Transparent), center = Offset(size.width, size.height), radius = size.maxDimension * 0.5f),
        radius = size.maxDimension * 0.5f,
        center = Offset(size.width, size.height),
    )
}
