package com.pidash.companion.ui.media

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pidash.companion.domain.model.MediaState
import com.pidash.companion.domain.usecase.MediaControlUseCase
import com.pidash.companion.domain.usecase.ObserveMediaStateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MediaViewModel @Inject constructor(
    observeMedia: ObserveMediaStateUseCase,
    private val mediaControl: MediaControlUseCase,
) : ViewModel() {

    val uiState: StateFlow<MediaState> =
        observeMedia().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MediaState())

    fun onTogglePlayPause() = mediaControl.togglePlayPause()
    fun onSkipNext() = mediaControl.skipNext()
    fun onSkipPrevious() = mediaControl.skipPrevious()
    fun onSeek(fraction: Float) = mediaControl.seekTo(fraction)
}
