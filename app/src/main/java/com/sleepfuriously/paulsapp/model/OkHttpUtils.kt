package com.sleepfuriously.paulsapp.model

import android.annotation.SuppressLint
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

/**
 * Bare-bones way to send and receive data from something via network
 * calls
 */
object OkHttpUtils {

    /** reference to the okhttp library */
    private val okHttpClient = OkHttpClient.Builder()
        .callTimeout(2, TimeUnit.SECONDS)
        .readTimeout(2, TimeUnit.SECONDS)
        .writeTimeout(2, TimeUnit.SECONDS)
        .connectTimeout(2, TimeUnit.SECONDS)
        .build()

    /** use THIS instead of the regular OkHttpClient if we want to trust everyone */
    private val allTrustingOkHttpClient: OkHttpClient

    /** Use this client for the Server-sent Event messages (it has different time-outs) */
    private val allTrustingOkHttpSseClient: OkHttpClient


    init {
        /** an array of trust managers that does not validate certificate chains--it trusts everyone! */
        val trustAllCerts = arrayOf<TrustManager>(
            @SuppressLint("CustomX509TrustManager")
            object : X509TrustManager {
                @SuppressLint("TrustAllX509TrustManager")
                override fun checkClientTrusted(chain: Array<X509Certificate?>?, authType: String?) {
                    // this is empty on purpose: I don't want this check executed
                    Log.v(TAG, "checkClientTrust() called.  This is merely to prevent a warning")
                }

                @SuppressLint("TrustAllX509TrustManager")
                override fun checkServerTrusted(chain: Array<X509Certificate?>?, authType: String?) {
                    Log.v(TAG, "checkServerTrusted() called.  This is merely to prevent a warning")
                    // this is empty on purpose: I don't want this check executed
                }

                override fun getAcceptedIssuers(): Array<X509Certificate> {
                    return arrayOf()
                }
            }
        )

        //---------------
        // implementation of a socket trust manager that blindly trusts all
        val sslContext = SSLContext.getInstance("SSL")
        sslContext.init(null, trustAllCerts, java.security.SecureRandom())

        // a socket factory that uses the all-trusting trust manager
        val sslSocketFactory = sslContext.socketFactory

        val allTrustingBuilder = OkHttpClient.Builder()

        allTrustingBuilder.sslSocketFactory(sslSocketFactory, trustAllCerts[0] as X509TrustManager)
        allTrustingBuilder.hostnameVerifier { _, _ -> true }

        // now to make our all-trusting okhttp clients
        allTrustingOkHttpClient = allTrustingBuilder.build()

        //---------------
        // do the same thing with the special builder for server-sent events
        val allTrustingSseBuilder = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)    // time to wait while connecting
            .readTimeout(0, TimeUnit.MINUTES)      // 0 means never close
            .writeTimeout(1, TimeUnit.MINUTES)      // do I need to write ever?

        allTrustingSseBuilder.sslSocketFactory(sslSocketFactory, trustAllCerts[0] as X509TrustManager)
        allTrustingSseBuilder.hostnameVerifier { _, _ -> true }

        allTrustingOkHttpSseClient = allTrustingSseBuilder.build()
    }

    /**
     * Returns a reference to a regular [OkHttpClient].  This one insists on
     * full security.
     */
    @Suppress("unused")
    fun getClient() : OkHttpClient {
        return okHttpClient
    }

    /**
     * Returns a reference to a special [OkHttpClient] that trusts everyone
     * unconditionally and is designed to receive server-sent events.
     */
    fun getAllTrustingSseClient() : OkHttpClient {
        return allTrustingOkHttpSseClient
    }

    /**
     * Yep, this is the big blunt hammer.  Kills all outstanding requests.
     */
    fun cancelAll() {
//        okHttpClient.     todo
    }

    /**
     * Sends the GET request with the supplied url.
     *
     * @return      The response from the call.  Could be anything.
     *              Use getBodyFromResponse() or getCodeFromResponse()
     *              to decipher it.
     *
     * NOTE
     *      You must call this from a coroutine (or any non Main thread,
     *      as if anyone uses other threads anymore!)
     */
    suspend fun synchronousGet(
        url: String,
        headerList: List<Pair<String, String>> = listOf(),
        trustAll: Boolean = false,
        debug: Boolean = false
    ): MyResponse = withContext(Dispatchers.IO)  {

        if (debug) {
            Log.d(TAG, "synchronousGet() for $url, headers = $headerList")
        }

        val requestBuilder = Request.Builder()

        // Making the request is a little tricky because of the headers.
        // Rather than one chain of builds, I have to separate because the
        // first header must be added with .header(), whereas successive
        // headers need to be done with .addHeader().

        try {
            requestBuilder.url(url)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Can't make a real url with $url!")
            e.printStackTrace()
            return@withContext MyResponse(
                isSuccessful = false,
                code = -1,
                message = e.message ?: "",
                body = e.cause.toString(),
                headers = listOf()
            )
        }

        // because of the need to break a good ol' for loop is the way to go
        for (i in headerList.indices) {
            val header = headerList[i]
            if (header.first.isEmpty()) {
                break   // empty header means nothing to do
            }
            if (i == 0) {
                // the first header will replace
                requestBuilder.header(header.first, header.second)
            } else {
                // successive headers add
                requestBuilder.addHeader(header.first, header.second)
            }
        }

        val request = requestBuilder.build()
        if (debug) {
            Log.d(TAG, "   request = $request")
        }

        try {
            if (trustAll) {
                val response = allTrustingOkHttpClient.newCall(request).execute()
                return@withContext MyResponse(response)
            } else {
                val response = okHttpClient.newCall(request).execute()
                return@withContext MyResponse(response)
            }
        } catch (e: Exception) {
            Log.e(TAG, "exception in synchronousGetRequest($url, $trustAll)")
            e.printStackTrace()
            return@withContext MyResponse(
                isSuccessful = false,
                code = -1,
                message = e.message ?: "",
                body = e.cause.toString(),
                headers = listOf()
            )
        }
    }

    /**
     * Same as [synchronousGet] except takes a simple pair for the
     * header.
     */
    suspend fun synchronousGet(
        url: String,
        header: Pair<String, String>,
        trustAll: Boolean = false
    ): MyResponse = withContext(Dispatchers.IO)  {
        val headerList = listOf(header)
        return@withContext synchronousGet(url, headerList, trustAll)
    }


    /**
     * This delivers a POST to the specified url with a String data.
     *
     * @param   url         Where to send the POST
     *
     * @param   bodyStr     The string to send as data
     *
     * @param   headerList  Any headers that needs to be attached.
     *                      This is a Map<String, String> as there
     *                      can be multiple headers.
     *                      Use null (default) if no headers are needed
     *                      (they rarely are in our case).
     *
     * @param   trustAll    When true, all ssl trust checks are skipped.
     *                      Hope you know what you're doing!
     *
     * @return  The response from the server
     *
     * NOTE
     *      May NOT be called on the main thread!
     */
    suspend fun synchronousPost(
        url: String,
        bodyStr: String,
        headerList: List<Pair<String, String>> = listOf(),
        trustAll: Boolean = false
    ): MyResponse = withContext(Dispatchers.IO) {

        Log.d(
            TAG,
            "synchronousPost( url = $url, body = $bodyStr, headers = $headerList, trustAll = $trustAll )"
        )

        // need to make a RequestBody from the string (complicated!)
        val body = bodyStr.toRequestBody("application/json; charset=utf-8".toMediaType())

        val requestBuilder = Request.Builder()

        try {
            requestBuilder
                .url(url)
                .post(body)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Can't make a real url with $url!")
            e.printStackTrace()
            return@withContext MyResponse(
                isSuccessful = false,
                code = -1,
                message = e.message ?: "",
                body = e.cause.toString(),
                headers = listOf()
            )
        }

        headerList.forEachIndexed { i, header ->
            if (i == 0) {
                if (header.first.isNotEmpty()) {
                    requestBuilder.header(header.first, header.second)
                }
            } else {
                if (header.first.isNotEmpty()) {
                    requestBuilder.addHeader(header.first, header.second)
                }
            }
        }
        val request = requestBuilder.build()

        try {
            val myResponse: MyResponse

            // two kinds of requests: unsafe and regular
            if (trustAll) {
                myResponse = MyResponse(allTrustingOkHttpClient.newCall(request).execute())
                return@withContext myResponse
            } else {
                myResponse = MyResponse(okHttpClient.newCall(request).execute())
                return@withContext myResponse
            }

        } catch (e: Exception) {
            Log.e(TAG, "exception in synchronousPost($url, $bodyStr, $headerList, $trustAll)")
            e.printStackTrace()
            return@withContext MyResponse(
                isSuccessful = false,
                code = -1,
                message = e.message ?: "",
                body = e.cause.toString(),
                headers = listOf()
            )
        }
    }


    suspend fun synchronousPut(
        url: String,
        bodyStr: String,
        headerList: List<Pair<String, String>> = listOf(),
        trustAll: Boolean
    ): MyResponse = withContext(Dispatchers.IO) {
        Log.d(
            TAG,
            "synchronousPut( url = $url, body = $bodyStr, headers = $headerList, trustAll = $trustAll )"
        )

        // need to make a RequestBody from the string (complicated!)
        val body = bodyStr.toRequestBody("application/json; charset=utf-8".toMediaType())

        val requestBuilder = Request.Builder()

        try {
            requestBuilder
                .url(url)
                .put(body)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Can't make a real url with $url!")
            e.printStackTrace()
            return@withContext MyResponse(
                isSuccessful = false,
                code = -1,
                message = e.message ?: "",
                body = e.cause.toString(),
                headers = listOf()
            )
        }

        headerList.forEachIndexed { i, header ->
            if (i == 0) {
                if (header.first.isNotEmpty()) {
                    requestBuilder.header(header.first, header.second)
                }
            } else {
                if (header.first.isNotEmpty()) {
                    requestBuilder.addHeader(header.first, header.second)
                }
            }
        }
        val request = requestBuilder.build()

        try {
            val myResponse: MyResponse

            // two kinds of requests: unsafe and regular
            if (trustAll) {
                myResponse = MyResponse(allTrustingOkHttpClient.newCall(request).execute())
                return@withContext myResponse
            } else {
                myResponse = MyResponse(okHttpClient.newCall(request).execute())
                return@withContext myResponse
            }

        } catch (e: Exception) {
            Log.e(
                TAG,
                "exception in synchronousPut($url, $bodyStr, $headerList, $trustAll)"
            )
            e.printStackTrace()
            return@withContext MyResponse(
                isSuccessful = false,
                code = -1,
                message = e.message ?: "",
                body = e.cause.toString(),
                headers = listOf()
            )
        }
    }

    /**
     * Sends a DELETE method to the given url and returns the result.
     */
    suspend fun synchronousDelete(
        url: String,
        bodyStr: String? = null,
        headerList: List<Pair<String, String>> = listOf(),
        trustAll: Boolean
    ): MyResponse = withContext(Dispatchers.IO) {

        val body = if (bodyStr == null) {
            Log.v(TAG, "syncrhonizeDelete(): bodyStr is null, passing null (this is to prevent warning)")
            null
        }
        else {
            // convert to an actual RequestBody
            bodyStr.toRequestBody("application/json; charset=utf-8".toMediaType())
        }

        val requestBuilder = Request.Builder()
        try {
            requestBuilder
                .url(url)
                .delete(body)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "synchronousDelete(): can't make a real url with $url!")
            e.printStackTrace()
            return@withContext MyResponse(
                isSuccessful = false,
                code = -1,
                message = e.message ?: "",
                body = e.cause.toString(),
                headers = listOf()
            )
        }

        val request = requestBuilder.build()

        try {
            val myResponse: MyResponse

            // two kinds of requests: unsafe and regular
            if (trustAll) {
                Log.d(TAG, "making DELETE call with all-trusting okhttpClient: $request")
                myResponse = MyResponse(allTrustingOkHttpClient.newCall(request).execute())
                return@withContext myResponse
            } else {
                Log.d(TAG, "making DELETE call with high-security okhttpClient: $request")
                myResponse = MyResponse(okHttpClient.newCall(request).execute())
                return@withContext myResponse
            }

        } catch (e: Exception) {
            Log.e(
                TAG,
                "exception in synchronousDelete($url, $bodyStr, $headerList, $trustAll)"
            )
            e.printStackTrace()
            return@withContext MyResponse(
                isSuccessful = false,
                code = -1,
                message = e.message ?: "",
                body = e.cause.toString(),
                headers = listOf()
            )
        }
    }


}

/**
 * Determines if the given string is a valid basic ip.
 * In other words, does it have the basic format #.#.#.#
 * and is not a domain name, nor does it have a prefix.
 *
 * Note that this is an extension function of String.  :)
 */
fun isValidBasicIp(str: String) : Boolean {
    if (str.isBlank()) {
        Log.d(TAG, "isValidBasicIp($str) is blank--that's a hard No!")
        return false
    }

    // only allow numbers and dots
    if (str.contains(Regex("[^0-9.]")) == true) {
        Log.d(TAG, "isValidBasicIp($str) contains only dots--nope!")
        return false
    }

    // there must be exactly 3 periods
    if (str.count { ".".contains(it) } != 3) {
        Log.d(TAG, "isValidBasicIp($str) only has 3 periods, nah!")
        return false
    }

    // get list of numbers
    val numList = str.split(".")

    // there should be 4 items
    if (numList.size != 4) {
        Log.d(TAG, "isValidBasicIp($str) should have 4 items, try again!")
        return false
    }

    // Each item should exist (ie not be blank or null).
    // Also, each should be a number:  0 <= n <= 255
    numList.forEach { s ->
        if (s.isEmpty()) {
            Log.d(TAG, "isValidBasicIp($str) an item is empty, really??")
            return false
        }
        val n = s.toInt()
        if ((n < 0) || (n > 255)) {
            Log.d(TAG, "isValidBasicIp($str) an item can't be converted to an Int!")
            return false
        }
    }

    // that's all I can think of!
    return true
}

/**
 * Responses returned from OkHttp requests are stored in this data
 * structure.  This is done as the regular [Response] data is
 * very picky about how it is accessed (which thread, how many
 * times, etc.).  This is a regular data class that can be used
 * in normal ways.
 *
 * Note that this can be constructed in two ways: directly (as seen
 * below) and through the companion object invoke operator.  You'll
 * almost certainly want to use the operator (see [MyResponse.invoke]).
 */
data class MyResponse(
    val isSuccessful: Boolean,
    val code: Int,
    val message: String,
    val body: String,
    /**
     * List of headers.  The first of each pair is the name, the 2nd is the value.
     * I will probably never use these as these are technical details of the transmission.
     */
    val headers: List<Pair<String, String>>
) {

    companion object {
        /**
         * Create a 2nd way to construct a MyResponse.
         * This uses a Response as its input param.
         *
         * example:
         *      val response = some_network_call()
         *      val myresponse = MyResponse(response)
         *
         * WARNING
         *  Unlike the normal constructor, this needs to be called off the
         *  main thread as it accesses [Response] data.
         */
        operator fun invoke(response: Response) : MyResponse {

            // add the headers one-by-one to header list
            val headerList = mutableListOf<Pair<String, String>>()
            response.headers.forEach { header ->
                val name = header.first
                val value = header.second
                headerList.add(Pair(name, value))
            }

            return MyResponse(
                response.isSuccessful,
                response.code,
                response.message,
                response.peekBody(BODY_PEEK_SIZE).string(),
                headerList
            )
        }
    }
}

/** The max number of bytes that we'll look at for a [Response] body (100k) */
private const val BODY_PEEK_SIZE = 100000L

private const val TAG = "OkHttpUtils"