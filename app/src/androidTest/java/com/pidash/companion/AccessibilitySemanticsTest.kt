package com.pidash.companion

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pidash.companion.robots.dashboard
import com.pidash.companion.robots.spots
import com.pidash.companion.ui.components.TestTags
import com.pidash.companion.ui.dashboard.DashboardActions
import com.pidash.companion.ui.spots.SpotsScreen
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * D. Accessibility & custom semantics that are specific to Pi-Dash behaviour.
 * The generic checks (labels, 64dp targets, rotary reachability, themes, font scale) run for every
 * screen through the `*ContractTest` classes.
 */
@RunWith(AndroidJUnit4::class)
class AccessibilitySemanticsTest {

    @get:Rule
    val rule = piDashComposeRule()

    @Test
    fun rotaryFocus_requested_isExposedThroughCustomSemantics() = rule.dashboard {
        launch { TestData.dashboard }
        rule.enableRotaryInput()
        assertRotaryFocus(TestTags.ACTION_HOME, focused = false)

        focus(TestTags.ACTION_HOME)

        assertRotaryFocus(TestTags.ACTION_HOME, focused = true)
    }

    @Test
    fun rotaryKnob_rotatedAndPressed_movesFocusAndActivatesControl() {
        var emergencyCalls = 0

        rule.dashboard {
            launch(DashboardActions(onEmergencyContact = { emergencyCalls++ })) { TestData.dashboard }
            rule.enableRotaryInput()
            focus(TestTags.ACTION_HOME)

            // When: one detent clockwise, then press the knob
            rule.rotate(1)
            assertRotaryFocus(TestTags.ACTION_HOME, focused = false)
            assertRotaryFocus(TestTags.ACTION_EMERGENCY, focused = true)
            pressCenter(TestTags.ACTION_EMERGENCY)
        }

        rule.runOnIdle { assertEquals(1, emergencyCalls) }
    }

    @Test
    fun spotControls_rendered_exposeSelectionAndDescriptiveLabels() {
        rule.setContent { SpotsScreen(TestData.spotsState, {}, {}) }

        rule.spots {
            assertSelected(TestTags.CHIP_ALL)
            assertDescription(TestTags.spotNavigate("c1"), "Navigate to Highlands Coffee")
        }
    }
}
