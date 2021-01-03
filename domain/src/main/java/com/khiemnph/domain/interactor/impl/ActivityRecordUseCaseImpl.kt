package com.khiemnph.domain.interactor.impl

import com.khiemnph.domain.interactor.ActivityRecordUseCase
import com.khiemnph.domain.model.ActivityRecord
import com.khiemnph.domain.repository.RecordRepository
import javax.inject.Inject

/**
 * Created by Khiem Nguyen on 1/3/2021.
 */
class ActivityRecordUseCaseImpl @Inject constructor(
    val repository: RecordRepository
) : BaseUseCaseImpl(), ActivityRecordUseCase {

    private var callback: ActivityRecordUseCase.Callback? = null

    override fun setCallback(callback: ActivityRecordUseCase.Callback) {
        this.callback = callback
    }

    override fun saveRecord(record: ActivityRecord) {
        addTask(
            completable  = repository.addRecord(record),
            doOnComplete = { callback?.onInsertRecordSuccess(record) },
            doOnError    = { e -> callback?.onInsertRecordFail(e) }
        )

    }

    override fun saveRecordList(records: ArrayList<ActivityRecord>) {

    }

}