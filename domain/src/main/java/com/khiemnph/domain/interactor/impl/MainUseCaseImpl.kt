package com.khiemnph.domain.interactor.impl

import com.khiemnph.domain.interactor.MainUseCase
import com.khiemnph.domain.repository.RecordRepository
import javax.inject.Inject

/**
 * Created by Khiem Nguyen on 1/3/2021.
 */
class MainUseCaseImpl @Inject constructor(
    val repository: RecordRepository
): BaseUseCaseImpl(), MainUseCase {

    private var callback: MainUseCase.Callback? = null

    override fun setCallback(callback: MainUseCase.Callback) {
        this.callback = callback
    }

    override fun loadSingleRecord(recordId: String) {
        addTask(
            single = repository.getSingleRecord(recordId),
            doOnSuccess = { record -> callback?.onReceiveSingleRecordSuccess(record) },
            doOnError = { e -> callback?.onReceiveSingleRecordFail(e) }
        )

    }

    override fun loadAllRecords() {
        addTask(
            single = repository.getAllRecords(),
            doOnSuccess = { records -> callback?.onReceiveRecordListSuccess(records) },
            doOnError = { e -> callback?.onReceiveRecordListFail(e) }
        )
    }


}