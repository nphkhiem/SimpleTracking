package com.khiemnph.simpletracking.ui.view

import com.khiemnph.domain.model.ActivityRecord

/**
 * Created by Khiem Nguyen on 1/3/2021.
 */
interface MainView: BaseView {

    fun toggleLoading(show: Boolean)
    fun toggleError(error: Throwable?)
    fun toggleEmptyView(show: Boolean)
    fun doOnReceiveRecordData(records: ArrayList<ActivityRecord>)
    fun doOnReceiveSingleRecordData(record: ActivityRecord)
}