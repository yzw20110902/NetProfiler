package com.mossmao.netprofiler

import android.content.Context
import android.net.ConnectivityManager
import com.koushikdutta.async.http.server.AsyncHttpServer
import com.koushikdutta.async.AsyncServer
import com.koushikdutta.async.http.server.AsyncHttpServerRequest
import android.os.Build
import com.koushikdutta.async.http.server.HttpServerRequestCallback
import android.content.res.AssetManager
import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.koushikdutta.async.http.WebSocket
import com.koushikdutta.async.http.server.AsyncHttpServerResponse
import org.json.JSONObject

import java.io.IOException
import java.io.InputStream
import java.lang.Exception
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.SocketException
import java.util.HashMap

object WebLogCat {
    private const val TAG = "IMOB-WebLogCat"
    private const val HTTP_POPRT = 8088
    private const val WEB_SOCKET_PORT = 8089
    private var currentWebSocket: WebSocket? = null
    private fun isWIFIConnected(context: Context): Boolean {
        val connectivityManager = context
            .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        var typeName: String? = null
        typeName = if (networkInfo != null) {
            networkInfo.typeName
        } else {
            "null"
        }
        return typeName!!.trim { it <= ' ' }.equals("wifi", ignoreCase = true)
    }

    private val iP: String?
        private get() {
            try {
                val en = NetworkInterface.getNetworkInterfaces()
                while (en.hasMoreElements()) {
                    val intf = en.nextElement()
                    val enumIpAddr = intf.inetAddresses
                    while (enumIpAddr.hasMoreElements()) {
                        val inetAddress = enumIpAddr.nextElement()
                        if (!inetAddress.isLoopbackAddress && inetAddress is Inet4Address) {
                            return inetAddress.getHostAddress()
                        }
                    }
                }
            } catch (ex: SocketException) {
                ex.printStackTrace()
            }
            return null
        }

    @JvmStatic
    fun init(context: Context) {
        if (isWIFIConnected(context)) {
            val server = AsyncHttpServer()
            val callback = HttpServerRequestCallbckImpl(context)
            server["[\\d\\D]*", callback]
            server.listen(AsyncServer.getDefault(), HTTP_POPRT)
            val webSocketServer = AsyncHttpServer()
            webSocketServer.websocket("/log") { webSocket: WebSocket?, request: AsyncHttpServerRequest? ->
                closePreviousSocket()
                currentWebSocket = webSocket
                //log(generateDeviceMsg(context));
                generateDeviceMsg(context)
            }
            webSocketServer.listen(WEB_SOCKET_PORT)
            Log.i(TAG, "open: http://" + iP + ":" + HTTP_POPRT + " to view logs")
            Log.i("OKPRFL_WS_URL", "ws://" + iP + ":" + WEB_SOCKET_PORT)
        } else {
            Log.e(TAG, "please connect wifi to use web logcat")
        }
    }



    private fun generateDeviceMsg(context: Context) {
        val model = Build.MODEL
        val manufacture = Build.MANUFACTURER
        val osVersion = Build.VERSION.RELEASE
        var pkgName = "unknown packageName"
        try {
            pkgName = context.packageName
        } catch (e: Exception) {
        }
        val str = "$model - $manufacture - $osVersion - < $pkgName >"
        log("device", str)

        //return generateToClientString("device",str);
    }

    private fun generateToClientString(type: String, s: String): String {
        val map: HashMap<String, String> = HashMap<String, String>()
        map["data"] = s
        map["type"] = type

        val gson = GsonBuilder().disableHtmlEscaping().create()
        return gson.toJson(map).toString()
        //return JSONObject.toJSON(jsonObject).toString();


    }

    @JvmStatic
    fun log(tag: String, msg: String) {
        if (currentWebSocket != null && currentWebSocket!!.isOpen) {
            currentWebSocket!!.send(generateToClientString(tag, msg))
        }
    }

    private fun closePreviousSocket() {
        if (currentWebSocket != null) {
            currentWebSocket!!.close()
        }
    }


    private class HttpServerRequestCallbckImpl(private val context: Context) :
        HttpServerRequestCallback {
        private val assetManager: AssetManager
        override fun onRequest(request: AsyncHttpServerRequest, response: AsyncHttpServerResponse) {
            val path = request.path
            var inputStream: InputStream? = null
            if (path == "/") {
                try {
                    inputStream = assetManager.open("imob_weblogcat.html")
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
                try {
                    inputStream = assetManager.open(path.substring(1))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            if (inputStream != null) {
                try {
                    response.sendStream(inputStream, inputStream.available().toLong())
                } catch (e: Exception) {
                    response.code(404).end()
                } finally {
                    try {
                        inputStream.close()
                    } catch (e: IOException) {
                    }
                }
            } else {
                response.code(404).end()
            }
        }

        init {
            assetManager = context.assets
        }
    }
}