package com.khiemnph.simpletracking.di

import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.UnconfinedTestDispatcher

/**
 * Test-only replacement for [CoroutineScopeModule], installed automatically in every
 * `@HiltAndroidTest` in this module. Uses [UnconfinedTestDispatcher] instead of the production
 * [kotlinx.coroutines.Dispatchers.Default] so [com.khiemnph.simpletracking.ui.record.RecordFragment]
 * tests that tap Stop can assert on the resulting state deterministically, without a race against a
 * real background thread pool.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [CoroutineScopeModule::class])
object TestCoroutineScopeModule {

    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + UnconfinedTestDispatcher())
}
