package com.pidash.companion.robots

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.pidash.companion.domain.model.Spot
import com.pidash.companion.domain.model.SpotCategory
import com.pidash.companion.testing.AppComposeRule
import com.pidash.companion.testing.BaseRobot
import com.pidash.companion.ui.components.TestTags
import com.pidash.companion.ui.spots.SpotsScreen
import com.pidash.companion.ui.spots.SpotsViewModel
import org.junit.Assert.assertTrue

class SpotsRobot(rule: AppComposeRule) : BaseRobot(rule) {

    /** Render the screen driven by a real [SpotsViewModel]. */
    fun launch(viewModel: SpotsViewModel, onNavigate: (Spot) -> Unit = {}) {
        rule.setContent {
            val state by viewModel.uiState.collectAsState()
            SpotsScreen(state, viewModel::onCategorySelected, onNavigate)
        }
    }

    // ---- actions ----
    /** `null` selects the "All" chip. */
    fun selectCategory(category: SpotCategory?) = tap(chipTag(category))

    fun scrollToSpot(id: String) = scrollTo(TestTags.SPOTS_LIST, TestTags.spotCard(id))

    fun tapNavigate(id: String) {
        scrollTo(TestTags.SPOTS_LIST, TestTags.spotNavigate(id))
        tap(TestTags.spotNavigate(id))
    }

    fun tapCard(id: String) {
        scrollToSpot(id)
        tap(TestTags.spotCard(id))
    }

    // ---- checks ----
    fun assertSelectedCategory(category: SpotCategory?) {
        assertSelected(chipTag(category))
        (listOf<SpotCategory?>(null) + SpotCategory.entries).filter { it != category }.forEach { assertSelected(chipTag(it), false) }
    }

    /** IDs of the spot cards currently composed (visible part of the LazyColumn). */
    fun renderedSpotIds(): List<String> = taggedIds("spot_card_")

    /** Only [expectedIds] are rendered, and every one of them is reachable by scrolling. */
    fun assertListShowsOnly(expectedIds: List<String>) {
        val rendered = renderedSpotIds()
        assertTrue("No cards rendered", rendered.isNotEmpty())
        assertTrue("Unexpected cards rendered: ${rendered - expectedIds.toSet()}", rendered.all { it in expectedIds })
        expectedIds.forEach { id ->
            scrollToSpot(id)
            assertDisplayed(TestTags.spotCard(id))
        }
    }

    fun assertEmptyState() {
        assertDisplayed(TestTags.SPOTS_EMPTY)
        assertNotPresent(TestTags.SPOTS_LIST)
    }

    private fun chipTag(category: SpotCategory?) = category?.let(TestTags::chip) ?: TestTags.CHIP_ALL
}

fun AppComposeRule.spots(block: SpotsRobot.() -> Unit) {
    SpotsRobot(this).block()
}
