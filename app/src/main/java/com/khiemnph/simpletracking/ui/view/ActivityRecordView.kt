package com.khiemnph.simpletracking.ui.view

import com.khiemnph.domain.model.ActivityRecord

/**
 * Created by Khiem Nguyen on 1/3/2021.
 */
interface ActivityRecordView: BaseView {

    fun onSaveRecordSuccess(record: ActivityRecord)
}