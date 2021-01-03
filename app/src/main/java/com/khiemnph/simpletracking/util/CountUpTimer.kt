package com.khiemnph.simpletracking.util

import android.os.*
import java.lang.StringBuilder

abstract class CountUpTimer(var interval: Long = 1000L) {

    @Volatile
    private var elapsedTime         : Long                              = 0L
    private var lastElapsedTime     : Long                              = 0L
    private var baseTime            : Long                              = 0L
    private var hour                : Int                               = 0
    private var minute              : Int                               = 0
    private var second              : Int                               = 0
    private var countUpHandler      : Handler
    private var fullDisplayedTime   : StringBuilder = StringBuilder()
    private val backgroundHandler   : HandlerThread = HandlerThread(CountUpTimer::class.java.simpleName)

    companion object {
        private const val MESSAGE = 1
    }

    init {
        backgroundHandler.start()
        countUpHandler = object : Handler(backgroundHandler.looper) {
            override fun handleMessage(msg: Message) {
                synchronized(this@CountUpTimer) {
                    elapsedTime = SystemClock.elapsedRealtime() - baseTime + lastElapsedTime
                    second = ((elapsedTime / 1000) % 60).toInt()
                    minute = ((elapsedTime / 1000) / 60).toInt()
                    hour= ((elapsedTime / 1000) / 60 / 60).toInt()
                    fullDisplayedTime.clear()
                    fullDisplayedTime
                        .append(if (hour < 10) "0$hour" else hour).append(":")
                        .append(if (minute < 10) "0$minute" else minute).append(":")
                        .append(if (second < 10) "0$second" else second)
                    onTick(fullDisplayedTime.toString())
                    sendMessageDelayed(obtainMessage(MESSAGE), interval)
                }
            }
        }
    }

    fun getElapsedTime(): Long = elapsedTime

    fun start() {
        baseTime = SystemClock.elapsedRealtime()
        countUpHandler.sendMessage(countUpHandler.obtainMessage(MESSAGE))
    }

    fun stop() {
        lastElapsedTime = elapsedTime + 300L //offset 100ms
        countUpHandler.removeMessages(MESSAGE)
    }


    fun reset() {
        synchronized(this) {
            baseTime        = SystemClock.elapsedRealtime()
            lastElapsedTime = 0L
            hour            = 0
            minute          = 0
            second          = 0
            fullDisplayedTime.clear()
        }
    }

    fun destroy() {
        backgroundHandler.quitSafely()
    }

    abstract fun onTick(displayedTime: String)

}

