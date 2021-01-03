package com.khiemnph.data.local.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.core.database.getBlobOrNull
import com.khiemnph.domain.model.ActivityRecord
import io.reactivex.Completable
import io.reactivex.Single
import java.lang.Exception

/**
 * Created by Khiem Nguyen on 1/3/2021.
 */
class DatabaseHelper(
    private val context: Context
) : SQLiteOpenHelper(context, DatabaseDefines.DB_NAME, null, DatabaseDefines.DB_VERSION) {

    companion object {
        @Volatile
        private var instance: DatabaseHelper? = null

        fun getInstance(context: Context): DatabaseHelper {
            if (instance == null) {
                synchronized(DatabaseHelper::class.java) {
                    if (instance == null)
                        instance =
                            DatabaseHelper(context)
                }
            }

            return instance!!
        }
    }


    override fun onCreate(db: SQLiteDatabase?) {
        createTableActivityRecord(db)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        //simple project ... no need to implement this
    }

    private fun createTableActivityRecord(db: SQLiteDatabase?) {
        db?.let {
            it.execSQL("CREATE TABLE ${DatabaseDefines.TBL_RECORD}(" +
                    "${TblActivityRecord.recordId} TEXT, " +
                    "${TblActivityRecord.recordThumb} BLOB, " +
                    "${TblActivityRecord.totalDistance} REAL, " +
                    "${TblActivityRecord.speed} REAL, " +
                    "${TblActivityRecord.elapsedTime} INTEGER, " +
                    "${TblActivityRecord.date} INTEGER, PRIMARY KEY (${TblActivityRecord.recordId}))"
            )
        }
    }

    fun addNewRecord(activityRecord: ActivityRecord): Completable {
        try {
            val result: Long
            val db = writableDatabase
            val value = ContentValues()
            value.put(TblActivityRecord.recordId, activityRecord.id)
            value.put(TblActivityRecord.recordThumb, activityRecord.recordThumbByteArr)
            value.put(TblActivityRecord.totalDistance, activityRecord.totalDistance)
            value.put(TblActivityRecord.speed, activityRecord.speed)
            value.put(TblActivityRecord.elapsedTime, activityRecord.elapsedTime)
            value.put(TblActivityRecord.date, activityRecord.recordDate)
            result = db.insert(DatabaseDefines.TBL_RECORD, null, value)
            db.close()
            return Completable.fromCallable {
                return@fromCallable result
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return Completable.complete()
    }

    fun getRecordById(recordId: String): Single<ActivityRecord> {
        try {
            val record = ActivityRecord()
            val db = readableDatabase
            val queriedColumns = arrayOf(
                TblActivityRecord.recordId,
                TblActivityRecord.recordThumb,
                TblActivityRecord.totalDistance,
                TblActivityRecord.speed,
                TblActivityRecord.elapsedTime,
                TblActivityRecord.date
            )
            val whereClause = "${TblActivityRecord.recordId} = ?"
            val whereArgs   = arrayOf(recordId)
            val cursor = db.query(
                DatabaseDefines.TBL_RECORD,
                queriedColumns,
                whereClause, whereArgs,
                null, null, "${TblActivityRecord.recordId} DESC"
            )
            while (cursor.moveToNext()) {
                record.id = cursor.getString(cursor.getColumnIndex(TblActivityRecord.recordId))
                record.recordThumbByteArr = cursor.getBlobOrNull(cursor.getColumnIndex(TblActivityRecord.recordThumb))
                record.totalDistance = cursor.getFloat(cursor.getColumnIndex(TblActivityRecord.totalDistance))
                record.speed = cursor.getFloat(cursor.getColumnIndex(TblActivityRecord.speed))
                record.elapsedTime = cursor.getLong(cursor.getColumnIndex(TblActivityRecord.elapsedTime))
                record.recordDate = cursor.getLong(cursor.getColumnIndex(TblActivityRecord.date))
            }
            db.close()
            cursor.close()
            return Single.just(record)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return Single.never()
    }

    fun getAllRecords(): Single<ArrayList<ActivityRecord>> {
        try {
            val records = ArrayList<ActivityRecord>()
            val db = readableDatabase
            val queriedColumns = arrayOf(
                TblActivityRecord.recordId,
                TblActivityRecord.recordThumb,
                TblActivityRecord.totalDistance,
                TblActivityRecord.speed,
                TblActivityRecord.elapsedTime,
                TblActivityRecord.date
            )

            val cursor = db.query(
                DatabaseDefines.TBL_RECORD,
                queriedColumns,
                null, null,
                null, null, null
            )

            if (cursor.moveToFirst()) {
                do {
                    val record = ActivityRecord()
                    record.id = cursor.getString(cursor.getColumnIndex(TblActivityRecord.recordId))
                    record.recordThumbByteArr = cursor.getBlobOrNull(cursor.getColumnIndex(TblActivityRecord.recordThumb))
                    record.totalDistance = cursor.getFloat(cursor.getColumnIndex(TblActivityRecord.totalDistance))
                    record.speed = cursor.getFloat(cursor.getColumnIndex(TblActivityRecord.speed))
                    record.elapsedTime = cursor.getLong(cursor.getColumnIndex(TblActivityRecord.elapsedTime))
                    record.recordDate = cursor.getLong(cursor.getColumnIndex(TblActivityRecord.date))
                    records.add(record)
                } while (cursor.moveToNext())
            }
            db.close()
            cursor.close()
            return Single.just(records)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return Single.never()
    }

}