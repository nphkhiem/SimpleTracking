package com.khiemnph.simpletracking.presenter.impl

import android.content.Context
import com.khiemnph.simpletracking.presenter.BasePresenter
import com.khiemnph.simpletracking.ui.view.BaseView

/**
 * Created by Khiem Nguyen on 1/3/2021.
 */
open class BasePresenterImpl<T : BaseView>: BasePresenter<T> {

    protected lateinit var baseView     : T
    protected          var context      : Context? = null
    protected          var isForeground : Boolean  = false

    override fun setView(view: T) {
        baseView = view
        context  = baseView.getContext()
    }

    override fun start() {
        super.start()
        isForeground = true
    }

    override fun stop() {
        isForeground = false
        super.stop()
    }
}