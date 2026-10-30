package com.pidash.companion

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pidash.companion.domain.model.Spot
import com.pidash.companion.domain.model.SystemSettings
import com.pidash.companion.domain.repository.SettingsRepository
import com.pidash.companion.ui.navigation.ExternalActions
import com.pidash.companion.ui.navigation.PiDashApp
import com.pidash.companion.ui.theme.PiDashTheme
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(settingsRepository: SettingsRepository) : ViewModel() {
    val settings: StateFlow<SystemSettings> = settingsRepository.settings
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        hideSystemBars()

        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            val snackbar = remember { SnackbarHostState() }
            val scope = rememberCoroutineScope()
            val actions = remember(snackbar, scope) { ActivityExternalActions(snackbar, scope) }
            // The dashboard type is already oversized for glanceability; a very large system font
            // scale would truncate labels, so cap it while still honoring moderate increases.
            val density = LocalDensity.current
            val cappedDensity = remember(density) { Density(density.density, density.fontScale.coerceAtMost(MAX_FONT_SCALE)) }
            CompositionLocalProvider(LocalDensity provides cappedDensity) {
                PiDashTheme(darkTheme = settings.isDarkTheme) {
                    PiDashApp(externalActions = actions, snackbarHostState = snackbar)
                }
            }
        }
    }

    /** Head units are kiosk-like: give the dashboard the whole screen. */
    private fun hideSystemBars() {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private inner class ActivityExternalActions(
        private val snackbar: SnackbarHostState,
        private val scope: CoroutineScope,
    ) : ExternalActions {
        override fun navigateHome() =
            launchOrNotify(Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=Home")), "Navigating home")

        override fun callEmergencyContact() =
            launchOrNotify(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$EMERGENCY_NUMBER")), "Calling emergency contact $EMERGENCY_NUMBER")

        override fun navigateTo(spot: Spot) {
            val uri = Uri.parse(
                String.format(Locale.US, "geo:%f,%f?q=%f,%f(%s)", spot.latitude, spot.longitude, spot.latitude, spot.longitude, Uri.encode(spot.name)),
            )
            launchOrNotify(Intent(Intent.ACTION_VIEW, uri), "Navigate to ${spot.name}")
        }

        /** A DIY head unit may have no maps/dialer app installed; tell the driver instead of crashing. */
        private fun launchOrNotify(intent: Intent, description: String) {
            try {
                startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            } catch (_: ActivityNotFoundException) {
                scope.launch { snackbar.showSnackbar("$description — no app available on this head unit") }
            }
        }
    }

    private companion object {
        const val EMERGENCY_NUMBER = "112"
        const val MAX_FONT_SCALE = 1.3f
    }
}
