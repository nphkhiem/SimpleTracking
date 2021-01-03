package com.khiemnph.domain.interactor

import com.khiemnph.domain.model.ActivityRecord

/**
 * Created by Khiem Nguyen on 1/3/2021.
 */
interface ActivityRecordUseCase: BaseUseCase {

    interface Callback {
        fun onInsertRecordSuccess(record: ActivityRecord)
        fun onInsertRecordFail(e: Throwable)
    }

    fun setCallback(callback: Callback)
    fun saveRecord(record: ActivityRecord)
    fun saveRecordList(records: ArrayList<ActivityRecord>)
}