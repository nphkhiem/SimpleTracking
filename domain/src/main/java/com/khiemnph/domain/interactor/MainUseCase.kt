package com.khiemnph.domain.interactor

import com.khiemnph.domain.model.ActivityRecord

/**
 * Created by Khiem Nguyen on 1/3/2021.
 */
interface MainUseCase: BaseUseCase {

    interface Callback {
        fun onReceiveSingleRecordSuccess(record: ActivityRecord)
        fun onReceiveSingleRecordFail(e: Throwable)
        fun onReceiveRecordListSuccess(records: ArrayList<ActivityRecord>)
        fun onReceiveRecordListFail(e: Throwable)
    }

    fun setCallback(callback: Callback)
    fun loadSingleRecord(recordId: String)
    fun loadAllRecords()
}