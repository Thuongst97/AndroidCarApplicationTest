package com.pidash.companion.data.repository

import android.content.Context
import android.media.AudioManager
import com.pidash.companion.domain.model.SystemSettings
import com.pidash.companion.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/** Theme preference is held in memory; mute is applied to the head unit's real media stream. */
@Singleton
class AudioSettingsRepository @Inject constructor(
    @ApplicationContext context: Context,
) : SettingsRepository {
    private val audioManager = context.getSystemService(AudioManager::class.java)

    private val _settings = MutableStateFlow(
        SystemSettings(isDarkTheme = true, isMuted = audioManager?.isStreamMute(AudioManager.STREAM_MUSIC) ?: false),
    )
    override val settings: StateFlow<SystemSettings> = _settings.asStateFlow()

    override fun setDarkTheme(enabled: Boolean) = _settings.update { it.copy(isDarkTheme = enabled) }

    override fun setMuted(muted: Boolean) {
        runCatching {
            audioManager?.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                if (muted) AudioManager.ADJUST_MUTE else AudioManager.ADJUST_UNMUTE,
                0,
            )
        }
        _settings.update { it.copy(isMuted = muted) }
    }
}
