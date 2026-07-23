package com.khiemnph.simpletracking.di

import com.khiemnph.domain.interactor.ObserveActiveSessionUseCase
import com.khiemnph.domain.interactor.PauseSessionUseCase
import com.khiemnph.domain.interactor.RecordLocationFixUseCase
import com.khiemnph.domain.interactor.ResumeSessionUseCase
import com.khiemnph.domain.interactor.StopSessionUseCase
import com.khiemnph.domain.repository.SessionRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import io.mockk.mockk
import javax.inject.Singleton

/**
 * Test-only replacement for [UseCaseModule], installed automatically in every `@HiltAndroidTest`
 * in this module. Binds [PauseSessionUseCase]/[ResumeSessionUseCase]/[StopSessionUseCase]/
 * [RecordLocationFixUseCase] to `mockk(relaxed = true)` instances so
 * [com.khiemnph.simpletracking.service.TrackingServiceTest] can keep its interaction-style
 * (`coVerify`) assertions while [com.khiemnph.simpletracking.service.TrackingService] is exercised
 * through a real Dagger-generated injection path - `Hilt_TrackingService`'s `onCreate()` - instead
 * of manual field assignment. Each binding is `@Singleton`-scoped so the same mockk instance is
 * shared between a test's own `@Inject` field and whatever `TrackingService` instance the same
 * test method creates, which is what makes `coVerify` against the test's field a valid check of
 * what the Service actually invoked.
 *
 * [ObserveActiveSessionUseCase] is deliberately NOT mocked here: it's wired exactly the way
 * [UseCaseModule] wires it in production, delegating to whatever [SessionRepository] the graph
 * provides (the in-memory fake from [TestRepositoryModule]), because
 * [com.khiemnph.simpletracking.ui.MainActivityTest] depends on its real state-based behavior.
 */
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [UseCaseModule::class])
object TestUseCaseModule {

    @Provides
    @Singleton
    fun providePauseSessionUseCase(): PauseSessionUseCase = mockk(relaxed = true)

    @Provides
    @Singleton
    fun provideResumeSessionUseCase(): ResumeSessionUseCase = mockk(relaxed = true)

    @Provides
    @Singleton
    fun provideStopSessionUseCase(): StopSessionUseCase = mockk(relaxed = true)

    @Provides
    @Singleton
    fun provideRecordLocationFixUseCase(): RecordLocationFixUseCase = mockk(relaxed = true)

    @Provides
    @Singleton
    fun provideObserveActiveSessionUseCase(sessionRepository: SessionRepository): ObserveActiveSessionUseCase =
        ObserveActiveSessionUseCase(sessionRepository)
}
