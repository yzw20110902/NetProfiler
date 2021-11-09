package com.mossmao.netprofiler


import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.lang.Exception
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.atomic.AtomicLong
import kotlin.Throws
import kotlin.jvm.Synchronized

class OkHttpProfilerInterceptor : Interceptor {
    private val dataTransfer: DataTransfer = LogDataTransfer()
    private val format: DateFormat = SimpleDateFormat("ddhhmmssSSS", Locale.US)
    private val previousTime = AtomicLong()
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val id = generateId()
        val startTime = System.currentTimeMillis()
        dataTransfer.sendRequest(id, chain.request())
        return try {
            val response: Response = chain.proceed(chain.request())
            dataTransfer.sendResponse(id, response)
            dataTransfer.sendDuration(id, System.currentTimeMillis() - startTime)
            response
        } catch (e: Exception) {
            dataTransfer.sendException(id, e)
            dataTransfer.sendDuration(id, System.currentTimeMillis() - startTime)
            throw e
        }
    }

    /**
     * Generates unique string id via a day and time
     * Based on a current time.
     *
     * @return string id
     */
    @Synchronized
    private fun generateId(): String {
        var currentTime = format.format(Date()).toLong()
        //Increase time if it the same, as previous (unique id)
        var previousTime = previousTime.get()
        if (currentTime <= previousTime) {
            currentTime = ++previousTime
        }
        this.previousTime.set(currentTime)
        return java.lang.Long.toString(currentTime, Character.MAX_RADIX)
    }
}