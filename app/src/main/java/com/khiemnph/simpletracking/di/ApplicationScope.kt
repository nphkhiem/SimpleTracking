package com.khiemnph.simpletracking.di

import javax.inject.Qualifier

/**
 * Qualifies the process-lifetime [kotlinx.coroutines.CoroutineScope] provided by
 * [CoroutineScopeModule] - see that module's doc for why this exists.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope
