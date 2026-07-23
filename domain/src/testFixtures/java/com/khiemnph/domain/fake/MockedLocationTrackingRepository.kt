package com.khiemnph.domain.fake

import com.khiemnph.domain.model.LocationPoint
import com.khiemnph.domain.repository.LocationTrackingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** In-memory fake of [LocationTrackingRepository] for tests. */
class MockedLocationTrackingRepository : LocationTrackingRepository {

    private val fixes = MutableSharedFlow<LocationPoint>(extraBufferCapacity = 64)

    override fun locationUpdates(): Flow<LocationPoint> = fixes.asSharedFlow()

    /** Test-only hook: pushes a fix into [locationUpdates] on demand. */
    fun emitFix(point: LocationPoint) {
        check(fixes.tryEmit(point)) { "Failed to emit fix, buffer capacity exceeded" }
    }
}
