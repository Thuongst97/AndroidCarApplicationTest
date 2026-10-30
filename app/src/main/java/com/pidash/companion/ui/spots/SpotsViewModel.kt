package com.pidash.companion.ui.spots

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pidash.companion.domain.model.Spot
import com.pidash.companion.domain.model.SpotCategory
import com.pidash.companion.domain.usecase.FilterSpotsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class SpotsUiState(
    val selectedCategory: SpotCategory? = null,
    val spots: List<Spot> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class SpotsViewModel @Inject constructor(
    filterSpots: FilterSpotsUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val selectedCategory = MutableStateFlow(
        savedStateHandle.get<String>(ARG_CATEGORY)?.let { name -> SpotCategory.entries.firstOrNull { it.name == name } },
    )

    val uiState: StateFlow<SpotsUiState> =
        combine(selectedCategory, filterSpots(selectedCategory)) { category, spots ->
            SpotsUiState(selectedCategory = category, spots = spots, isLoading = false)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SpotsUiState(selectedCategory = selectedCategory.value))

    /** Selecting the active chip again clears the filter. */
    fun onCategorySelected(category: SpotCategory?) {
        selectedCategory.value = if (category == selectedCategory.value) null else category
    }

    companion object {
        const val ARG_CATEGORY = "category"
    }
}
