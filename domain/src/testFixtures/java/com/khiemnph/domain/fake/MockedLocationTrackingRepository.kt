package com.khiemnph.domain.fake

import com.khiemnph.domain.model.RawLocationFix
import com.khiemnph.domain.repository.LocationTrackingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** In-memory fake of [LocationTrackingRepository] for tests. */
class MockedLocationTrackingRepository : LocationTrackingRepository {

    private val fixes = MutableSharedFlow<RawLocationFix>(extraBufferCapacity = 64)

    override fun locationUpdates(): Flow<RawLocationFix> = fixes.asSharedFlow()

    /** Test-only hook: pushes a fix into [locationUpdates] on demand. */
    fun emitFix(fix: RawLocationFix) {
        check(fixes.tryEmit(fix)) { "Failed to emit fix, buffer capacity exceeded" }
    }
}
