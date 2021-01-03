package com.khiemnph.simpletracking.di.component

import com.khiemnph.simpletracking.di.Activity
import com.khiemnph.simpletracking.di.module.RecordModule
import com.khiemnph.simpletracking.ui.activity.RecordActivity
import dagger.Component


/**
 * Created by Khiem Nguyen on 1/3/2021.
 */

@Activity
@Component(dependencies = [AppComponent::class], modules = [RecordModule::class])
interface RecordComponent {
    fun inject(activity: RecordActivity)
}