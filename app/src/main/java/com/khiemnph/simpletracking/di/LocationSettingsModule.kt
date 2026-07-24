package com.khiemnph.simpletracking.di

import com.khiemnph.simpletracking.location.LocationSettingsChecker
import com.khiemnph.simpletracking.location.PlayServicesLocationSettingsChecker
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class LocationSettingsModule {

    @Binds
    abstract fun bindLocationSettingsChecker(impl: PlayServicesLocationSettingsChecker): LocationSettingsChecker
}
