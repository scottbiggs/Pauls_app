package com.sleepfuriously.paulsapp.model

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response

/**
 * Bare-bones way to send and receive data from something via network
 * calls
 */
object OkHttpUtils {

    /** reference to the okhttp library */
    private val okHttpClient = OkHttpClient()


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
     * This delivers a POST to the specified url.
     *
     * @param   postBodyName        The name of the contents
     *
     * @param   postBodyContents    Contents of the body of the POST as a String.
     *                              You (caller) do all the json translation stuff.
     *
     * @return  The response from the server
     *
     * NOTE
     *      Must be called from a coroutine.  It changes the
     *      context so the Main thread won't be blocked.
     */
    suspend fun synchronousSendPostRequest(
        url: String,
        postBodyName: String,
        postBodyContents: String
    ) : MyResponse {

        Log.d(TAG, "synchronousSendPostRequest( $url, $postBodyName, $postBodyContents)")

        val formBody : RequestBody = MultipartBody.Builder()
            .addFormDataPart(postBodyName, postBodyContents)
            .build()

        val request = Request.Builder()
            .url(url)
            .post(formBody)
            .build()

        // this insures that we're not on the main thread
        val myResponse: MyResponse

        withContext(Dispatchers.IO) {
            myResponse = MyResponse(okHttpClient.newCall(request).execute())
        }

        return myResponse
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
    /** List of headers.  The first of each pair is the name, the 2nd is the value. */
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