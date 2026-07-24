package com.khiemnph.domain.repository

import com.khiemnph.domain.model.RawLocationFix
import kotlinx.coroutines.flow.Flow

interface LocationTrackingRepository {

    fun locationUpdates(sessionId: String): Flow<RawLocationFix>
}
