package com.khiemnph.data.di

import com.khiemnph.data.location.LocationSettingsChecker
import com.khiemnph.data.location.PlayServicesLocationSettingsChecker
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Binds the location-settings check inside `:data`, alongside the `SettingsClient` it needs.
 *
 * It used to be bound in `:app`, which meant `:app` reached into `:data` for
 * `buildTrackingLocationRequest` and handled `com.google.android.gms.location` types directly,
 * contradicting the promise in `FusedLocationTrackingRepository`'s own KDoc that those types never
 * leave that layer.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class LocationSettingsModule {

    @Binds
    @Singleton
    abstract fun bindLocationSettingsChecker(impl: PlayServicesLocationSettingsChecker): LocationSettingsChecker
}
