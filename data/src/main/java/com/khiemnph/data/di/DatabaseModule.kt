package com.khiemnph.data.di

import android.content.Context
import androidx.room.Room
import com.khiemnph.data.local.db.AppDatabase
import com.khiemnph.data.local.db.LocationPointDao
import com.khiemnph.data.local.db.MIGRATION_1_2
import com.khiemnph.data.local.db.MIGRATION_2_3
import com.khiemnph.data.local.db.MIGRATION_3_4
import com.khiemnph.data.local.db.MIGRATION_4_5
import com.khiemnph.data.local.db.MIGRATION_5_6
import com.khiemnph.data.local.db.SessionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private const val DATABASE_NAME = "simple_tracking.db"

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, DATABASE_NAME)
            // Every version bump needs its Migration registered here. There is deliberately no
            // fallbackToDestructiveMigration: silently wiping a user's recorded history is a worse
            // outcome than a build that fails until the migration is written.
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
            .build()

    @Provides
    fun provideSessionDao(database: AppDatabase): SessionDao = database.sessionDao()

    @Provides
    fun provideLocationPointDao(database: AppDatabase): LocationPointDao = database.locationPointDao()
}
