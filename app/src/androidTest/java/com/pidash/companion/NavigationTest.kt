package com.pidash.companion

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pidash.companion.domain.model.SpotCategory
import com.pidash.companion.ui.navigation.Routes
import com.pidash.companion.ui.navigation.navigateToTopLevel
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Exercises the real route table and rail navigation helper with placeholder screens. */
@RunWith(AndroidJUnit4::class)
class NavigationTest {

    @get:Rule
    val rule = piDashComposeRule()

    private lateinit var navController: NavHostController

    @Before
    fun setUp() {
        rule.setContent {
            navController = rememberNavController()
            NavHost(navController, startDestination = Routes.DASHBOARD) {
                composable(Routes.DASHBOARD) { Text("Dashboard") }
                composable(Routes.MEDIA) { Text("Media") }
                composable(Routes.SPOTS) { entry -> Text("Spots:${entry.arguments?.getString("category")}") }
            }
        }
    }

    private fun backStackRoutes() = navController.currentBackStack.value.mapNotNull { it.destination.route }

    @Test
    @Smoke
    fun railReturnsToDashboard_afterQuickStopOpenedSpots() {
        // Regression: Media -> Drive via rail saved a stack under the start destination, which
        // was then restored when tapping Drive again from a quick-stop Spots screen.
        rule.runOnIdle { navController.navigateToTopLevel(Routes.MEDIA) }
        rule.observe() // real-time pause in observation mode (no-op otherwise)
        rule.runOnIdle { navController.navigateToTopLevel(Routes.DASHBOARD) }
        rule.observe() // real-time pause in observation mode (no-op otherwise)
        rule.runOnIdle { navController.navigate(Routes.spots(SpotCategory.COFFEE)) }
        rule.observe() // real-time pause in observation mode (no-op otherwise)
        rule.onNodeWithText("Spots:COFFEE").assertIsDisplayed()

        rule.runOnIdle { navController.navigateToTopLevel(Routes.DASHBOARD) }
        rule.observe() // real-time pause in observation mode (no-op otherwise)

        rule.onNodeWithText("Dashboard").assertIsDisplayed()
        rule.runOnIdle { assertEquals(listOf(Routes.DASHBOARD), backStackRoutes()) }
    }

    @Test
    fun railSwitchesBetweenAllDestinations_keepingDashboardAsRoot() {
        rule.runOnIdle { navController.navigate(Routes.spots(SpotCategory.FOOD)) }
        rule.observe() // real-time pause in observation mode (no-op otherwise)
        rule.runOnIdle { navController.navigateToTopLevel(Routes.MEDIA) }
        rule.observe() // real-time pause in observation mode (no-op otherwise)
        rule.onNodeWithText("Media").assertIsDisplayed()
        rule.runOnIdle { assertEquals(listOf(Routes.DASHBOARD, Routes.MEDIA), backStackRoutes()) }

        rule.runOnIdle { navController.navigateToTopLevel(Routes.spots()) }
        rule.observe() // real-time pause in observation mode (no-op otherwise)
        rule.onNodeWithText("Spots:null").assertIsDisplayed()
        rule.runOnIdle { assertEquals(listOf(Routes.DASHBOARD, Routes.SPOTS), backStackRoutes()) }
    }
}
