package com.khiemnph.simpletracking.presenter.impl

import android.util.Log
import com.khiemnph.domain.interactor.ActivityRecordUseCase
import com.khiemnph.domain.model.ActivityRecord
import com.khiemnph.simpletracking.presenter.ActivityRecordPresenter
import com.khiemnph.simpletracking.ui.view.ActivityRecordView
import javax.inject.Inject

/**
 * Created by Khiem Nguyen on 1/3/2021.
 */
class ActivityRecordPresenterImpl @Inject constructor(
    val useCase: ActivityRecordUseCase
): ActivityRecordPresenter {

    private lateinit var recordView: ActivityRecordView

    init {
        useCase.setCallback(object : ActivityRecordUseCase.Callback {
            override fun onInsertRecordSuccess(record: ActivityRecord) {
                recordView.onSaveRecordSuccess(record)
            }

            override fun onInsertRecordFail(e: Throwable) {
                Log.i("WTF", "fail: ${e.localizedMessage}")
            }
        })
    }

    override fun saveRecord(record: ActivityRecord) {
        useCase.saveRecord(record)
    }

    override fun setView(view: ActivityRecordView) {
        recordView = view
    }
}