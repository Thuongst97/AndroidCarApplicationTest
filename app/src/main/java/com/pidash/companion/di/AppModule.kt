package com.pidash.companion.di

import com.pidash.companion.data.repository.AudioSettingsRepository
import com.pidash.companion.data.repository.MockMediaRepository
import com.pidash.companion.data.repository.MockSpotsRepository
import com.pidash.companion.data.repository.MockTripRepository
import com.pidash.companion.data.repository.MockWeatherRepository
import com.pidash.companion.domain.repository.MediaRepository
import com.pidash.companion.domain.repository.SettingsRepository
import com.pidash.companion.domain.repository.SpotsRepository
import com.pidash.companion.domain.repository.TripRepository
import com.pidash.companion.domain.repository.WeatherRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds @Singleton abstract fun bindTripRepository(impl: MockTripRepository): TripRepository
    @Binds @Singleton abstract fun bindWeatherRepository(impl: MockWeatherRepository): WeatherRepository
    @Binds @Singleton abstract fun bindMediaRepository(impl: MockMediaRepository): MediaRepository
    @Binds @Singleton abstract fun bindSpotsRepository(impl: MockSpotsRepository): SpotsRepository
    @Binds @Singleton abstract fun bindSettingsRepository(impl: AudioSettingsRepository): SettingsRepository
}

@Module
@InstallIn(SingletonComponent::class)
object CoroutinesModule {
    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
}
