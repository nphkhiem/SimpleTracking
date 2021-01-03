package com.khiemnph.domain.executor

import java.util.concurrent.Executor
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * Created by Khiem Nguyen on 1/3/2021.
 */

private const val CORE_POOL_SIZE = 3
private const val MAX_POOL_SIZE  = 5
private const val ALIVE_TIME     = 10L

class TaskExecutor : Executor {

    companion object {
        val instance: Executor by lazy { TaskExecutor() }
    }

    private var threadPoolExecutor: ThreadPoolExecutor = ThreadPoolExecutor(
        CORE_POOL_SIZE, MAX_POOL_SIZE, ALIVE_TIME, TimeUnit.SECONDS, LinkedBlockingDeque()
    )

    override fun execute(command: Runnable?) {
        threadPoolExecutor.execute(command)
    }

}