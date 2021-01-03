package com.khiemnph.simpletracking.di.module

import android.content.Context
import android.content.res.Resources
import com.khiemnph.data.di.DbModule
import com.khiemnph.simpletracking.SimpleTrackingApp
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

/**
 * Created by Khiem Nguyen on 1/3/2021.
 */

@Module(includes = [DbModule::class])
class AppModule(val app: SimpleTrackingApp) {

    @Provides
    @Singleton
    fun provideAppContext(): Context = app.applicationContext

    @Provides
    @Singleton
    fun provideResources(context: Context): Resources = context.resources


}