package com.pidash.companion

import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pidash.companion.domain.model.Spot
import com.pidash.companion.domain.model.SpotCategory
import com.pidash.companion.domain.repository.SpotsRepository
import com.pidash.companion.domain.usecase.FilterSpotsUseCase
import com.pidash.companion.robots.spots
import com.pidash.companion.ui.spots.SpotsViewModel
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** C. Filtering & list interaction, with a real SpotsViewModel over a fake repository. */
@RunWith(AndroidJUnit4::class)
class FavoriteSpotsTest {

    @get:Rule
    val rule = piDashComposeRule()

    private val navigated = mutableListOf<Spot>()

    private fun viewModel(spots: List<Spot> = TestData.spots, initialCategory: SpotCategory? = null): SpotsViewModel {
        val repository = object : SpotsRepository {
            override fun spots() = flowOf(spots)
        }
        val args = initialCategory?.let { mapOf(SpotsViewModel.ARG_CATEGORY to it.name) } ?: emptyMap()
        return SpotsViewModel(FilterSpotsUseCase(repository), SavedStateHandle(args))
    }

    private fun idsOf(category: SpotCategory) =
        TestData.spots.filter { it.category == category }.sortedBy { it.distanceKm }.map { it.id }

    @Test
    fun categoryChip_coffeeSelected_listShowsOnlyCoffeeSpots() {
        // Given
        val vm = viewModel()
        rule.spots {
            launch(vm)
            assertSelectedCategory(null)

            // When
            selectCategory(SpotCategory.COFFEE)

            // Then
            assertSelectedCategory(SpotCategory.COFFEE)
            assertListShowsOnly(idsOf(SpotCategory.COFFEE))
        }
        assertEquals(idsOf(SpotCategory.COFFEE), vm.uiState.value.spots.map { it.id })
    }

    @Test
    fun categoryChip_selectedAgain_clearsFilter() = rule.spots {
        launch(viewModel())
        selectCategory(SpotCategory.FOOD)
        assertTrue(renderedSpotIds().all { it.startsWith("f") })

        selectCategory(SpotCategory.FOOD)

        assertSelectedCategory(null)
        assertTrue(renderedSpotIds().map { it.first() }.toSet().size > 1)
    }

    @Test
    fun categoryChip_fromNavigationArgument_isPreselected() = rule.spots {
        launch(viewModel(initialCategory = SpotCategory.GAS_STATION))

        assertSelectedCategory(SpotCategory.GAS_STATION)
        assertTrue(renderedSpotIds().all { it.startsWith("g") })
    }

    @Test
    fun navigateButton_farSpotAfterScrolling_firesNavigationAction() {
        val farthest = TestData.spots.maxBy { it.distanceKm }

        rule.spots {
            launch(viewModel(), onNavigate = { navigated += it })
            tapNavigate(farthest.id)
        }

        assertEquals(listOf(farthest), navigated)
    }

    @Test
    fun spotCard_tapped_firesNavigationAction() {
        val target = TestData.spots.first { it.id == "r2" }

        rule.spots {
            launch(viewModel(), onNavigate = { navigated += it })
            selectCategory(SpotCategory.REST_STOP)
            tapCard(target.id)
        }

        assertEquals(listOf(target), navigated)
    }

    @Test
    fun categoryChip_categoryWithoutSpots_showsEmptyState() = rule.spots {
        launch(viewModel(spots = TestData.spots.filter { it.category != SpotCategory.REST_STOP }))

        selectCategory(SpotCategory.REST_STOP)

        assertEmptyState()
    }
}
