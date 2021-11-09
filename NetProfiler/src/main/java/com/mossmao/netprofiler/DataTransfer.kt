package com.mossmao.netprofiler

import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.lang.Exception
import kotlin.Throws

interface DataTransfer {
    @Throws(IOException::class)
    fun sendRequest(id: String?, request: Request?)

    @Throws(IOException::class)
    fun sendResponse(id: String?, response: Response?)
    fun sendException(id: String?, response: Exception?)
    fun sendDuration(id: String?, duration: Long)
}