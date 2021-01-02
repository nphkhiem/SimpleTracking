package com.khiemnph.simpletracking

import android.app.Application
import android.content.Context

/**
 * Created by Khiem Nguyen on 12/24/2020.
 */
class SimpleTrackingApp: Application() {

    companion object {
        @Volatile
        lateinit var instance: SimpleTrackingApp
        lateinit var appContext: Context
    }


    override fun onCreate() {
        super.onCreate()
        instance = this
        appContext = applicationContext
    }

}