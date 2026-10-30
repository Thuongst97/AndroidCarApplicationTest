package com.pidash.companion

import androidx.compose.runtime.Composable
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pidash.companion.ui.spots.SpotsScreen
import org.junit.runner.RunWith

/** Standard UI contract (themes, font scale, labels, 64dp targets, rotary reachability). */
@RunWith(AndroidJUnit4::class)
class SpotsScreenContractTest : PiDashScreenContract() {
    @Composable
    override fun Content() = SpotsScreen(TestData.spotsState, {}, {})
}
