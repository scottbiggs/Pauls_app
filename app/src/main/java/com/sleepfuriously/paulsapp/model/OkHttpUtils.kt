package com.sleepfuriously.paulsapp.model

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.util.concurrent.TimeUnit

/**
 * Bare-bones way to send and receive data from something via network
 * calls
 */
object OkHttpUtils {

    /** reference to the okhttp library */
//    private val okHttpClient = OkHttpClient()
    private val okHttpClient = OkHttpClient.Builder()
        .callTimeout(2, TimeUnit.SECONDS)
        .readTimeout(2, TimeUnit.SECONDS)
        .writeTimeout(2, TimeUnit.SECONDS)
        .connectTimeout(2, TimeUnit.SECONDS)
        .build()


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
    @Suppress("RedundantSuspendModifier")
    suspend fun synchronousGetRequest(url: String) : MyResponse {

        val request = Request.Builder()
            .url(url)
            .build()

        val response = okHttpClient.newCall(request).execute()
        return MyResponse(response)
    }


    /**
     * This delivers a POST to the specified url with a String data.
     *
     * @param   url         Where to send the POST
     *
     * @param   bodyStr        The string to send as data
     *
     * @param   header      Any headers that needs to be attached.
     *                      This is a Map<String, String> as there
     *                      can be multiple headers.
     *                      Use null (default) if no headers are needed
     *                      (they rarely are in our case).
     *
     * @return  The response from the server
     *
     * NOTE
     *      May NOT be called on the main thread!
     */
    suspend fun synchronousPostString(
        url: String,
        bodyStr: String,
        header: Map<String, String>? = null,
    ) : MyResponse {

        Log.d(TAG, "synchronousSendPostRequest( url = $url, body = $bodyStr, header = $header )")

        // need to make a RequestBody from the string (complicated!)
        val body = bodyStr.toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        val myResponse: MyResponse

        // this insures that we're not on the main thread
        withContext(Dispatchers.IO) {   // does this even work?
            myResponse = MyResponse(okHttpClient.newCall(request).execute())
        }

        return myResponse
    }

    /**
     * Yep, this is the big blunt hammer.  Kills all outstanding requests.
     */
    fun cancelAll() {
//        okHttpClient.     todo
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
        return false
    }

    // only allow numbers and dots
    if (str.contains(Regex("[^0-9.]")) == true) {
        return false
    }

    // there must be exactly 3 periods
    if (str.count { ".".contains(it) } != 3) {
        return false
    }

    // get list of numbers
    val numList = str.split(".")

    // there should be 4 items
    if (numList.size != 4) {
        return false
    }

    // Each item should exist (ie not be blank or null).
    // Also, each should be a number:  0 <= n <= 255
    numList.forEach { s ->
        if (s.isEmpty()) {
            return false
        }
        val n = s.toInt()
        if ((n < 0) || (n > 255)) {
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
         *  main thread as it accesses [Response] data. todo: test this!
         */
        operator fun invoke(response: Response): MyResponse {

            // add the headers one-by-one to header list
            val headerList = mutableListOf<Pair<String, String>>()
            response.headers.forEach() { header ->
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