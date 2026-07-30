package com.khiemnph.simpletracking.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

private const val PREFERENCES_FILE = "user_preferences"

@Module
@InstallIn(SingletonComponent::class)
object PreferencesModule {

    /**
     * A process-lifetime scope, deliberately not the application scope used for tracking work.
     * DataStore runs an internal actor on whatever scope it is given, and cancelling that scope
     * would silently stop preferences from being written for the rest of the process.
     */
    @Provides
    @Singleton
    fun providePreferencesDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
        ) { context.preferencesDataStoreFile(PREFERENCES_FILE) }
}
