package com.khiemnph.simpletracking.di

import com.khiemnph.simpletracking.location.FakeLocationSettingsChecker
import com.khiemnph.data.di.LocationSettingsModule
import com.khiemnph.data.location.LocationSettingsChecker
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

/**
 * Test-only replacement for [LocationSettingsModule], installed automatically in every
 * `@HiltAndroidTest` under `androidTest`. Swaps the real Play-Services-backed
 * [com.khiemnph.data.location.PlayServicesLocationSettingsChecker] for a
 * [FakeLocationSettingsChecker] a test can steer directly, avoiding a real Play Services network
 * round-trip to resolve the device's Location Service state.
 */
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [LocationSettingsModule::class])
object TestLocationSettingsModule {

    @Provides
    @Singleton
    fun provideLocationSettingsChecker(): LocationSettingsChecker = FakeLocationSettingsChecker()
}
