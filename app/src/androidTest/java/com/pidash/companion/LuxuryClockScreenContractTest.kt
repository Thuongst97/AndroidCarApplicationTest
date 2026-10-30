package com.pidash.companion

import androidx.compose.runtime.Composable
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pidash.companion.ui.clock.LuxuryClockScreen
import org.junit.runner.RunWith
import java.time.LocalDateTime

/** Standard UI contract (themes, font scale, labels, 64dp targets, rotary reachability) + screenshots. */
@RunWith(AndroidJUnit4::class)
class LuxuryClockScreenContractTest : PiDashScreenContract() {
    @Composable
    override fun Content() = LuxuryClockScreen(onClose = {}, timeSource = { LocalDateTime.of(2026, 10, 31, 10, 9, 30) })
}
