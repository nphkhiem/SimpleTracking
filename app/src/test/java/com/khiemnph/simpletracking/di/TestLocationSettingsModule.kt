package com.khiemnph.simpletracking.di

import com.khiemnph.simpletracking.location.FakeLocationSettingsChecker
import com.khiemnph.simpletracking.location.LocationSettingsChecker
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

/**
 * Test-only replacement for [LocationSettingsModule], installed automatically in every
 * `@HiltAndroidTest` in this module. Swaps the real Play-Services-backed
 * [com.khiemnph.simpletracking.location.PlayServicesLocationSettingsChecker] for a
 * [FakeLocationSettingsChecker] a test can steer directly, since Play Services `Task` callbacks
 * aren't exercised meaningfully under Robolectric at the Fragment level (see
 * [com.khiemnph.simpletracking.location.PlayServicesLocationSettingsCheckerTest] for the real
 * implementation's own direct coverage).
 */
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [LocationSettingsModule::class])
object TestLocationSettingsModule {

    @Provides
    @Singleton
    fun provideLocationSettingsChecker(): LocationSettingsChecker = FakeLocationSettingsChecker()
}
