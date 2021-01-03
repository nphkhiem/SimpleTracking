package com.khiemnph.data.local

import android.content.Context
import com.khiemnph.data.local.db.DatabaseHelper
import com.khiemnph.domain.model.ActivityRecord
import com.khiemnph.domain.repository.RecordRepository
import io.reactivex.Completable
import io.reactivex.Single
import javax.inject.Inject

/**
 * Created by Khiem Nguyen on 1/3/2021.
 */
class RecordRepositoryImpl @Inject constructor(val context: Context) : RecordRepository {
    override fun addRecord(activityRecord: ActivityRecord): Completable {
        return DatabaseHelper.getInstance(context).addNewRecord(activityRecord)
    }

    override fun getSingleRecord(recordId: String): Single<ActivityRecord> {
        return DatabaseHelper.getInstance(context).getRecordById(recordId)
    }

    override fun getAllRecords(): Single<ArrayList<ActivityRecord>> {
        return DatabaseHelper.getInstance(context).getAllRecords()
    }


}