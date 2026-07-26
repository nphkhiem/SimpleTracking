package com.khiemnph.simpletracking.di

import android.util.Log
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineExceptionHandler
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
 * running on it, but it does not swallow the failure: without a [CoroutineExceptionHandler] an
 * uncaught throwable reaches the thread's default handler and kills the process. Since the whole
 * point of this scope is work that must survive its caller, it must also survive its own failures.
 */
@Module
@InstallIn(SingletonComponent::class)
object CoroutineScopeModule {

    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope = CoroutineScope(
        SupervisorJob() + Dispatchers.Default + CoroutineExceptionHandler { _, throwable ->
            Log.e("ApplicationScope", "Unhandled failure on the application scope", throwable)
        },
    )
}
