package com.khiemnph.simpletracking.di

import com.khiemnph.domain.interactor.DeleteSessionUseCase
import com.khiemnph.domain.interactor.ObserveSessionSummaryUseCase
import com.khiemnph.domain.interactor.ObserveActiveSessionUseCase
import com.khiemnph.domain.interactor.ObserveSessionHistoryUseCase
import com.khiemnph.domain.interactor.PauseSessionUseCase
import com.khiemnph.domain.interactor.RecordLocationFixUseCase
import com.khiemnph.domain.interactor.ResumeSessionUseCase
import com.khiemnph.domain.interactor.StartSessionUseCase
import com.khiemnph.domain.interactor.StopSessionUseCase
import com.khiemnph.domain.repository.SessionRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Wires `:domain` use cases for injection. These classes deliberately have no `@Inject`
 * constructor of their own so `:domain` stays a pure Kotlin module with zero framework
 * dependencies; this module is where that wiring happens instead.
 */
@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {

    @Provides
    fun provideDeleteSessionUseCase(sessionRepository: SessionRepository): DeleteSessionUseCase =
        DeleteSessionUseCase(sessionRepository)

    @Provides
    fun provideObserveSessionSummaryUseCase(sessionRepository: SessionRepository): ObserveSessionSummaryUseCase =
        ObserveSessionSummaryUseCase(sessionRepository)

    @Provides
    fun providePauseSessionUseCase(sessionRepository: SessionRepository): PauseSessionUseCase =
        PauseSessionUseCase(sessionRepository)

    @Provides
    fun provideResumeSessionUseCase(sessionRepository: SessionRepository): ResumeSessionUseCase =
        ResumeSessionUseCase(sessionRepository)

    @Provides
    fun provideStopSessionUseCase(sessionRepository: SessionRepository): StopSessionUseCase =
        StopSessionUseCase(sessionRepository)

    @Provides
    fun provideStartSessionUseCase(sessionRepository: SessionRepository): StartSessionUseCase =
        StartSessionUseCase(sessionRepository)

    @Provides
    fun provideRecordLocationFixUseCase(sessionRepository: SessionRepository): RecordLocationFixUseCase =
        RecordLocationFixUseCase(sessionRepository)

    @Provides
    fun provideObserveActiveSessionUseCase(sessionRepository: SessionRepository): ObserveActiveSessionUseCase =
        ObserveActiveSessionUseCase(sessionRepository)

    @Provides
    fun provideObserveSessionHistoryUseCase(sessionRepository: SessionRepository): ObserveSessionHistoryUseCase =
        ObserveSessionHistoryUseCase(sessionRepository)
}
