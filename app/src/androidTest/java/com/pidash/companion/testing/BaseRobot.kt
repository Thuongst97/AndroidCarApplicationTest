package com.pidash.companion.testing

import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasStateDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.input.key.Key
import org.junit.Assert.assertEquals

/**
 * Base class for screen robots (a.k.a. screen objects). A robot owns the *how* (test tags,
 * gestures, semantics keys) so tests only express the *what*:
 *
 * ```
 * dashboard { tapMute(); assertMuted(true) }
 * ```
 *
 * Subclasses add screen-specific verbs (`tapX`) and checks (`assertX`) built on these primitives.
 */
abstract class BaseRobot(protected val rule: AppComposeRule) {

    protected fun node(tag: String, unmerged: Boolean = false): SemanticsNodeInteraction =
        rule.onNodeWithTagCompat(tag, unmerged)

    // ---- actions ------------------------------------------------------------------------------
    fun tap(tag: String) {
        node(tag).performClick()
    }

    fun scrollTo(containerTag: String, targetTag: String) {
        node(containerTag).performScrollToNode(hasTestTag(targetTag))
    }

    fun focus(tag: String) {
        node(tag).requestFocus()
    }

    /** Press the rotary knob / D-pad centre on [tag] (must be focused). */
    @OptIn(ExperimentalTestApi::class)
    fun pressCenter(tag: String) {
        node(tag).performKeyInput { pressKey(Key.DirectionCenter) }
    }

    // ---- checks -------------------------------------------------------------------------------
    fun assertDisplayed(vararg tags: String) = tags.forEach { node(it).assertIsDisplayed() }

    fun assertNotPresent(tag: String) = rule.onAllNodesWithTagCompat(tag).let { assertEquals("Expected no '$tag'", 0, it) }

    fun assertText(tag: String, text: String) {
        node(tag).assertTextEquals(text)
    }

    fun assertDescription(tag: String, description: String, unmerged: Boolean = false) {
        node(tag, unmerged).assert(hasContentDescription(description))
    }

    fun assertStateDescription(tag: String, state: String) {
        node(tag).assert(hasStateDescription(state))
    }

    fun <T> assertSemantics(tag: String, key: SemanticsPropertyKey<T>, value: T) {
        node(tag).assert(hasSemantics(key, value))
    }

    fun assertSelected(tag: String, selected: Boolean = true) {
        if (selected) node(tag).assertIsSelected() else node(tag).assertIsNotSelected()
    }

    fun assertFocused(tag: String) {
        node(tag).assertIsFocused()
    }

    /** Tags (with [prefix] removed) of all composed nodes whose tag starts with [prefix]. */
    fun taggedIds(prefix: String): List<String> =
        rule.onAllNodes(hasTestTagPrefix(prefix)).fetchSemanticsNodes().mapNotNull { it.testTag?.removePrefix(prefix) }
}

private fun AppComposeRule.onNodeWithTagCompat(tag: String, unmerged: Boolean) =
    onNode(hasTestTag(tag), useUnmergedTree = unmerged)

private fun AppComposeRule.onAllNodesWithTagCompat(tag: String): Int =
    onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().size
