package com.khiemnph.data.local.db

/**
 * Created by Khiem Nguyen on 1/3/2021.
 */

class DatabaseDefines {

    companion object {
        val DB_NAME     = "TrackingDB"
        val DB_VERSION  = 2101010
        val TBL_RECORD  = "table_activity_record"
    }
}

class TblActivityRecord {
    companion object {
        val recordId        = "record_id"
        val recordThumb     = "recordThumb"
        val totalDistance   = "total_distance"
        val speed           = "speed"
        val elapsedTime     = "elapsed_time"
        val date            = "date"
    }
}