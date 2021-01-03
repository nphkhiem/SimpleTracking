package com.khiemnph.simpletracking.utils

import android.os.Build

/**
 * Created by Khiem Nguyen on 12/25/2020.
 */
class AppUtil {
    companion object {
        fun reachMarshmallow(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
        fun reachNougat()     : Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
        fun reachOreo()       : Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
        fun reachPie()        : Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
    }
}