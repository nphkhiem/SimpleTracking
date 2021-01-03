package com.khiemnph.simpletracking.presenter.impl

import android.util.Log
import com.khiemnph.domain.interactor.MainUseCase
import com.khiemnph.domain.model.ActivityRecord
import com.khiemnph.simpletracking.presenter.MainPresenter
import com.khiemnph.simpletracking.ui.view.MainView
import javax.inject.Inject

/**
 * Created by Khiem Nguyen on 1/3/2021.
 */
class MainPresenterImpl @Inject constructor(val useCase: MainUseCase): MainPresenter {

    private lateinit var mainView: MainView
    private var received = false

    init {
        useCase.setCallback(object : MainUseCase.Callback {
            override fun onReceiveSingleRecordSuccess(record: ActivityRecord) {

                mainView.doOnReceiveSingleRecordData(record)
            }

            override fun onReceiveSingleRecordFail(e: Throwable) {
                Log.i("WTF", ">>>> fail: ${e.localizedMessage}")
            }

            override fun onReceiveRecordListSuccess(records: ArrayList<ActivityRecord>) {
                received = true
                mainView.toggleLoading(false)
                mainView.toggleError(null)
                if (records.isEmpty()) {
                    mainView.toggleEmptyView(true)
                } else {
                    mainView.toggleEmptyView(false)
                    mainView.doOnReceiveRecordData(records)
                }

            }

            override fun onReceiveRecordListFail(e: Throwable) {
                TODO("Not yet implemented")
            }

        })
    }

    override fun loadSingleRecord(recordId: String) {
        mainView.toggleLoading(true)
        mainView.toggleError(null)
        useCase.loadSingleRecord(recordId)
    }

    override fun loadAllRecords() {
        if (!received) {
            mainView.toggleLoading(true)
            mainView.toggleError(null)
            useCase.loadAllRecords()
        }
    }

    override fun reloadAllRecords() {
        received = false
        loadAllRecords()
    }

    override fun setView(view: MainView) {
        mainView = view
    }
}