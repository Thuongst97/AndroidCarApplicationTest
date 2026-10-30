package com.pidash.companion.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.pidash.companion.ui.components.SectionLabel
import com.pidash.companion.ui.components.rememberPressScale
import com.pidash.companion.ui.components.rotaryFocused
import com.pidash.companion.ui.theme.Dimens
import com.pidash.companion.ui.theme.ambientBackground
import java.time.LocalDateTime
import java.util.Locale
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.pidash.companion.domain.model.Spot
import com.pidash.companion.domain.model.SpotCategory
import com.pidash.companion.ui.components.TestTags
import com.pidash.companion.ui.dashboard.DashboardActions
import com.pidash.companion.ui.dashboard.DashboardScreen
import com.pidash.companion.ui.dashboard.DashboardViewModel
import com.pidash.companion.ui.media.MediaActions
import com.pidash.companion.ui.media.MediaScreen
import com.pidash.companion.ui.media.MediaViewModel
import com.pidash.companion.ui.spots.SpotsScreen
import com.pidash.companion.ui.spots.SpotsViewModel
import com.pidash.companion.ui.theme.DashTheme
import kotlinx.coroutines.delay
import java.time.format.DateTimeFormatter

object Routes {
    const val DASHBOARD = "dashboard"
    const val MEDIA = "media"
    const val SPOTS = "spots?${SpotsViewModel.ARG_CATEGORY}={${SpotsViewModel.ARG_CATEGORY}}"
    fun spots(category: SpotCategory? = null) = if (category == null) "spots" else "spots?${SpotsViewModel.ARG_CATEGORY}=${category.name}"
}

/**
 * Rail navigation: keep only the start destination underneath the selected screen.
 * Deliberately no saveState/restoreState — the start destination is itself a rail item, and
 * restoring its saved stack would re-push whatever screen was just popped (e.g. Spots).
 */
fun NavHostController.navigateToTopLevel(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id)
        launchSingleTop = true
    }
}

/** Side effects that leave the app (maps, phone). Implemented by the Activity. */
interface ExternalActions {
    fun navigateHome()
    fun callEmergencyContact()
    fun navigateTo(spot: Spot)
}

private data class RailDestination(val route: String, val baseRoute: String, val label: String, val icon: ImageVector, val tag: String)

private val destinations = listOf(
    RailDestination(Routes.DASHBOARD, "dashboard", "Drive", Icons.Outlined.Speed, TestTags.NAV_DASHBOARD),
    RailDestination(Routes.MEDIA, "media", "Media", Icons.Outlined.LibraryMusic, TestTags.NAV_MEDIA),
    RailDestination(Routes.SPOTS, "spots", "Spots", Icons.Outlined.Place, TestTags.NAV_SPOTS),
)

@Composable
fun PiDashApp(
    externalActions: ExternalActions,
    snackbarHostState: SnackbarHostState,
    navController: NavHostController = rememberNavController(),
) {
    val focusManager = LocalFocusManager.current
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    Box(
        Modifier
            .fillMaxSize()
            .ambientBackground(DashTheme.colors)
            // Rotary controllers (or a DIY encoder knob) emit scroll events: map them to focus traversal.
            .onRotaryScrollEvent { event ->
                focusManager.moveFocus(if (event.verticalScrollPixels > 0) FocusDirection.Next else FocusDirection.Previous)
            }
            .focusable(),
    ) {
        Row(Modifier.fillMaxSize()) {
            NavigationRail(currentRoute = currentRoute, onSelect = navController::navigateToTopLevel)
            NavHost(navController, startDestination = Routes.DASHBOARD, modifier = Modifier.weight(1f).fillMaxHeight()) {
                composable(Routes.DASHBOARD) {
                    val vm: DashboardViewModel = hiltViewModel()
                    val state by vm.uiState.collectAsStateWithLifecycle()
                    DashboardScreen(
                        state = state,
                        actions = DashboardActions(
                            onHomeNavigation = externalActions::navigateHome,
                            onEmergencyContact = externalActions::callEmergencyContact,
                            onToggleMute = vm::onToggleMute,
                            onToggleTheme = vm::onToggleTheme,
                            onQuickStop = { navController.navigate(Routes.spots(it)) },
                            onTogglePlayPause = vm::onTogglePlayPause,
                            onOpenMedia = { navController.navigate(Routes.MEDIA) { launchSingleTop = true } },
                        ),
                    )
                }
                composable(Routes.MEDIA) {
                    val vm: MediaViewModel = hiltViewModel()
                    val state by vm.uiState.collectAsStateWithLifecycle()
                    MediaScreen(
                        state = state,
                        actions = MediaActions(vm::onTogglePlayPause, vm::onSkipNext, vm::onSkipPrevious, vm::onSeek),
                    )
                }
                composable(
                    Routes.SPOTS,
                    arguments = listOf(navArgument(SpotsViewModel.ARG_CATEGORY) { type = NavType.StringType; nullable = true }),
                ) {
                    val vm: SpotsViewModel = hiltViewModel()
                    val state by vm.uiState.collectAsStateWithLifecycle()
                    SpotsScreen(state, vm::onCategorySelected, externalActions::navigateTo)
                }
            }
        }
        SnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter).padding(24.dp))
    }
}

@Composable
private fun NavigationRail(currentRoute: String?, onSelect: (String) -> Unit) {
    val colors = DashTheme.colors
    Column(
        Modifier
            .fillMaxHeight()
            .width(112.dp)
            .drawBehind {
                // Hairline separator that fades out at both ends, like trim inlay.
                drawLine(
                    Brush.verticalGradient(listOf(Color.Transparent, colors.glassBorder, Color.Transparent)),
                    Offset(size.width - 1f, 0f),
                    Offset(size.width - 1f, size.height),
                    strokeWidth = 1.dp.toPx(),
                )
            }
            .padding(vertical = 20.dp)
            .testTag(TestTags.NAV_RAIL),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Clock()
        Spacer(Modifier.height(18.dp))
        destinations.forEach { dest ->
            val selected = currentRoute?.substringBefore('?') == dest.baseRoute
            RailItem(
                icon = dest.icon,
                label = dest.label,
                selected = selected,
                onClick = { if (!selected) onSelect(if (dest.baseRoute == "spots") Routes.spots() else dest.route) },
                modifier = Modifier.testTag(dest.tag),
            )
        }
    }
}

/** Rail entry without a box: the selected item glows in the accent metal with a light bar on its edge. */
@Composable
private fun RailItem(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = DashTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val scale = rememberPressScale(interaction, pressedScale = 0.9f)
    val tint by animateColorAsState(if (selected) colors.accent else colors.textSecondary, label = "railTint")
    val barAlpha by animateFloatAsState(if (selected) 1f else 0f, label = "railBar")
    val shape = RoundedCornerShape(Dimens.CardRadiusSmall)

    Box(
        modifier
            .size(width = 112.dp, height = 84.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .drawBehind {
                if (barAlpha > 0f) {
                    val barHeight = 36.dp.toPx()
                    val top = (size.height - barHeight) / 2
                    drawCircle(
                        Brush.radialGradient(listOf(colors.accent.copy(alpha = 0.22f * barAlpha), Color.Transparent), center = Offset(0f, size.height / 2), radius = size.height * 0.7f),
                        radius = size.height * 0.7f,
                        center = Offset(0f, size.height / 2),
                    )
                    drawRoundRect(
                        colors.accent.copy(alpha = barAlpha),
                        topLeft = Offset(0f, top),
                        size = Size(3.dp.toPx(), barHeight),
                        cornerRadius = CornerRadius(2.dp.toPx()),
                    )
                }
            }
            .padding(horizontal = 12.dp)
            .clip(shape)
            .border(if (focused) 2.dp else 0.dp, if (focused) colors.accentLight else Color.Transparent, shape)
            .clickable(interactionSource = interaction, indication = null, role = Role.Tab, onClick = onClick)
            .semantics {
                this.selected = selected
                rotaryFocused = focused
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(8.dp))
            Text(label.uppercase(), style = MaterialTheme.typography.labelLarge, color = tint, maxLines = 1, softWrap = false)
        }
    }
}

@Composable
private fun Clock() {
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now()
            delay(1_000)
        }
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            now.format(DateTimeFormatter.ofPattern("HH:mm")),
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraLight, fontSize = 32.sp),
            color = DashTheme.colors.textPrimary,
        )
        Text(
            now.format(DateTimeFormatter.ofPattern("EEE d MMM", Locale.US)).uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
            color = DashTheme.colors.accent,
            maxLines = 1,
        )
    }
}
