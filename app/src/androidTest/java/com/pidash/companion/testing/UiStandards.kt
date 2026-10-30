package com.pidash.companion.testing

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.unit.Dp
import org.junit.Assert.assertTrue

/**
 * Reusable UI-standard checks over whatever is currently composed. They are generic: they look at
 * every clickable node, so new controls are covered without touching the tests.
 */
object UiStandards {

    fun clickableNodes(rule: AppComposeRule): List<SemanticsNode> =
        rule.onAllNodes(hasClickAction()).fetchSemanticsNodes()

    /** Every clickable has an accessible label; icon-only ones need an explicit contentDescription. */
    fun assertClickablesHaveLabels(rule: AppComposeRule, screen: String = "") {
        val nodes = clickableNodes(rule)
        assertTrue("$screen: no clickable nodes found", nodes.isNotEmpty())
        nodes.forEach { node ->
            val description = node.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty().joinToString()
            val text = node.config.getOrNull(SemanticsProperties.Text).orEmpty().joinToString { it.text }
            val clickLabel = node.config.getOrNull(SemanticsActions.OnClick)?.label
            assertTrue(
                "$screen: clickable ${node.describe()} has no accessible label",
                description.isNotBlank() || text.isNotBlank() || !clickLabel.isNullOrBlank(),
            )
            if (text.isBlank()) {
                assertTrue("$screen: icon-only control ${node.describe()} lacks contentDescription", description.isNotBlank())
            }
        }
    }

    /** Every clickable is at least [min] x [min]. */
    fun assertClickablesMeetTouchTarget(rule: AppComposeRule, min: Dp = rule.config.minTouchTarget, screen: String = "") {
        val minPx = with(rule.density) { min.toPx() }
        clickableNodes(rule).forEach { node ->
            assertTrue(
                "$screen: ${node.describe()} is ${node.size} px, below $min ($minPx px)",
                node.size.width >= minPx - 1 && node.size.height >= minPx - 1,
            )
        }
    }

    /**
     * Every clickable composed at the start can be reached by rotating the knob (focus traversal).
     * Requires [AppComposeRule.enableRotaryInput] to have been called.
     */
    fun assertClickablesReachableByRotary(rule: AppComposeRule, screen: String = "") {
        val targets = clickableNodes(rule).associateBy { it.id }
        val visited = mutableSetOf<Int>()
        repeat(targets.size * 2 + 4) {
            rule.rotate(1)
            rule.onAllNodes(isFocused()).fetchSemanticsNodes().forEach { visited += it.id }
        }
        val unreachable = targets.filterKeys { it !in visited }.values.map { it.describe() }
        assertTrue("$screen: not reachable by rotary focus: $unreachable", unreachable.isEmpty())
    }
}
