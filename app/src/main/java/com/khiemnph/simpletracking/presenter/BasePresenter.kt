package com.khiemnph.simpletracking.presenter

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.khiemnph.simpletracking.ui.view.BaseView

/**
 * Created by Khiem Nguyen on 1/3/2021.
 */
interface BasePresenter<T: BaseView> : LifecycleObserver {

    fun setView(view: T)

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    fun create() {}

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun start() {}

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun resume() {}

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun pause() {}

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun stop() {}

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun destroy() {}
}