package com.khiemnph.domain.repository

import com.khiemnph.domain.model.LocationPoint
import kotlinx.coroutines.flow.Flow

interface LocationTrackingRepository {

    fun locationUpdates(): Flow<LocationPoint>
}
