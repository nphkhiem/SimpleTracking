package com.khiemnph.simpletracking.di

import com.khiemnph.data.di.RepositoryModule
import com.khiemnph.data.util.Clock
import com.khiemnph.data.util.WallClock
import com.khiemnph.domain.fake.MockedLocationTrackingRepository
import com.khiemnph.domain.fake.MockedSessionRepository
import com.khiemnph.domain.repository.LocationTrackingRepository
import com.khiemnph.domain.repository.SessionRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

/**
 * Test-only replacement for [RepositoryModule], installed automatically in every
 * `@HiltAndroidTest` in this module. Swaps the real Room/Play-Services-backed bindings for
 * in-memory fakes so classes injected through the real Dagger-generated path (e.g.
 * [com.khiemnph.simpletracking.ui.MainActivity]) can be exercised deterministically without a
 * real database or GPS, while still going through genuine Hilt injection rather than a manual
 * bypass.
 */
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [RepositoryModule::class])
object TestRepositoryModule {

    @Provides
    @Singleton
    fun provideSessionRepository(): SessionRepository = MockedSessionRepository()

    @Provides
    @Singleton
    fun provideLocationTrackingRepository(): LocationTrackingRepository = MockedLocationTrackingRepository()

    @Provides
    @Singleton
    fun provideClock(): Clock = WallClock()
}
