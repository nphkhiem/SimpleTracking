package com.khiemnph.simpletracking.di.component

import android.content.Context
import android.content.res.Resources
import com.khiemnph.domain.repository.RecordRepository
import com.khiemnph.simpletracking.SimpleTrackingApp
import com.khiemnph.simpletracking.di.module.AppModule
import dagger.Component
import javax.inject.Singleton

/**
 * Created by Khiem Nguyen on 1/3/2021.
 */

@Singleton
@Component(modules = [AppModule::class])
interface AppComponent {

    fun inject(app: SimpleTrackingApp)

    fun context(): Context

    fun resources(): Resources

    fun activityRecordRepository(): RecordRepository
}