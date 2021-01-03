package com.khiemnph.simpletracking.presenter

import com.khiemnph.domain.model.ActivityRecord
import com.khiemnph.simpletracking.presenter.impl.BasePresenterImpl
import com.khiemnph.simpletracking.ui.view.ActivityRecordView

/**
 * Created by Khiem Nguyen on 1/3/2021.
 */
interface ActivityRecordPresenter: BasePresenter<ActivityRecordView> {

    fun saveRecord(record: ActivityRecord)
}