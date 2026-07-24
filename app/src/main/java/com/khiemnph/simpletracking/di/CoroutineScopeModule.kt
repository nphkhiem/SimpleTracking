package com.khiemnph.simpletracking.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Provides a process-lifetime [CoroutineScope], qualified by [ApplicationScope], for
 * fire-and-forget actions that must reliably complete even if the Fragment/ViewModel that
 * triggered them is torn down first. [com.khiemnph.simpletracking.ui.record.RecordViewModel.onStopClicked]
 * is the motivating case: it's invoked from an async map-snapshot callback that can fire after
 * navigation has already popped the Fragment and cleared its `viewModelScope`, so launching on
 * this scope instead keeps the thumbnail save and the Stop intent from being silently dropped.
 * A [SupervisorJob] means one failing action never cancels this scope or any other action already
 * running on it.
 */
@Module
@InstallIn(SingletonComponent::class)
object CoroutineScopeModule {

    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
}
