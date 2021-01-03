package com.khiemnph.simpletracking.di.component

import com.khiemnph.simpletracking.di.Activity
import com.khiemnph.simpletracking.di.module.MainModule
import com.khiemnph.simpletracking.ui.activity.MainActivity
import dagger.Component

/**
 * Created by Khiem Nguyen on 1/3/2021.
 */

@Activity
@Component(dependencies = [AppComponent::class], modules = [MainModule::class])
interface MainComponent {
    fun inject(activity: MainActivity)
}