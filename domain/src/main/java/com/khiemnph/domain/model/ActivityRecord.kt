package com.khiemnph.domain.model

import android.os.Parcelable
import com.google.android.gms.maps.model.LatLng
import kotlinx.android.parcel.Parcelize

/**
 * Created by Khiem Nguyen on 1/3/2021.
 */
@Parcelize
class ActivityRecord(
    var id                  : String?            = null,
//    var movingRoute         : ArrayList<LatLng>? = null, // create a map from the moving route contains latitude and longitude
    var recordThumbByteArr  : ByteArray?         = null,
    var totalDistance       : Float              = 0f,
    var speed               : Float              = 0f,
    var avgSpeed            : Float              = 0f,
    var elapsedTime         : Long               = 0L,
    var recordDate          : Long               = System.currentTimeMillis()
): Parcelable {

}