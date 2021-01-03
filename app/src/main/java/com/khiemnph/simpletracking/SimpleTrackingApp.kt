package com.khiemnph.simpletracking

import android.app.Application
import android.content.Context
import com.khiemnph.simpletracking.di.module.AppModule
import com.khiemnph.simpletracking.di.component.AppComponent
import com.khiemnph.simpletracking.di.component.DaggerAppComponent

/**
 * Created by Khiem Nguyen on 12/24/2020.
 */
class SimpleTrackingApp: Application() {

    companion object {
        @Volatile
        lateinit var instance: SimpleTrackingApp
        lateinit var appContext: Context
        lateinit var appComponent: AppComponent
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        appContext = applicationContext
        initInjector()
    }

    private fun initInjector() {
        appComponent = DaggerAppComponent.builder()
            .appModule(AppModule(this))
            .build()
        appComponent.inject(this)
    }

}