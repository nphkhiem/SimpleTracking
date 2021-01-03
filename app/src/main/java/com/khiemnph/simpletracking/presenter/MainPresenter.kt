package com.khiemnph.simpletracking.presenter

import com.khiemnph.simpletracking.ui.view.MainView

/**
 * Created by Khiem Nguyen on 1/3/2021.
 */
interface MainPresenter: BasePresenter<MainView> {

    fun loadSingleRecord(recordId: String)
    fun loadAllRecords()
    fun reloadAllRecords()
}