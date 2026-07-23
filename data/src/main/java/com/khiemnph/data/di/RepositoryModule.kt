package com.khiemnph.data.di

import com.khiemnph.data.location.FusedLocationTrackingRepository
import com.khiemnph.data.repository.SessionRepositoryImpl
import com.khiemnph.data.util.Clock
import com.khiemnph.data.util.WallClock
import com.khiemnph.domain.repository.LocationTrackingRepository
import com.khiemnph.domain.repository.SessionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindSessionRepository(impl: SessionRepositoryImpl): SessionRepository

    @Binds
    @Singleton
    abstract fun bindLocationTrackingRepository(impl: FusedLocationTrackingRepository): LocationTrackingRepository

    @Binds
    @Singleton
    abstract fun bindClock(impl: WallClock): Clock
}
