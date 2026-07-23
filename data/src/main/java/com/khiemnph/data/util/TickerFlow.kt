package com.khiemnph.data.util

import kotlin.time.Duration
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** Emits [Unit] immediately, then again every [period], forever — a coroutine-native heartbeat. */
fun tickerFlow(period: Duration): Flow<Unit> = flow {
    while (true) {
        emit(Unit)
        delay(period)
    }
}
