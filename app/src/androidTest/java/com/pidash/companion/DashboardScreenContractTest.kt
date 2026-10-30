package com.pidash.companion

import androidx.compose.runtime.Composable
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pidash.companion.ui.dashboard.DashboardActions
import com.pidash.companion.ui.dashboard.DashboardScreen
import org.junit.runner.RunWith

/** Standard UI contract (themes, font scale, labels, 64dp targets, rotary reachability). */
@RunWith(AndroidJUnit4::class)
class DashboardScreenContractTest : PiDashScreenContract() {
    @Composable
    override fun Content() = DashboardScreen(TestData.dashboard, DashboardActions())
}
