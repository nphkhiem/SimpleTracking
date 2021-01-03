package com.khiemnph.data.di

import com.khiemnph.data.local.RecordRepositoryImpl
import com.khiemnph.domain.repository.RecordRepository
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

/**
 * Created by Khiem Nguyen on 1/3/2021.
 */

@Module
class DbModule {
    @Provides
    @Singleton
    fun provideActivityRecordRepository(repoImpl: RecordRepositoryImpl): RecordRepository {
        return repoImpl
    }
}