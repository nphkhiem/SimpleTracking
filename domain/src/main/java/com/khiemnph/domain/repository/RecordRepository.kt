package com.khiemnph.domain.repository

import com.khiemnph.domain.model.ActivityRecord
import io.reactivex.Completable
import io.reactivex.Single

/**
 * Created by Khiem Nguyen on 1/3/2021.
 */
interface RecordRepository {
    fun addRecord(activityRecord: ActivityRecord): Completable
    fun getSingleRecord(recordId: String): Single<ActivityRecord>
    fun getAllRecords(): Single<ArrayList<ActivityRecord>>
}