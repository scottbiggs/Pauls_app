package com.sleepfuriously.paulsapp.model

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

/**
 * Bare-bones way to send and receive data from something via network
 * calls
 */
object OkHttpUtils {

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
    suspend fun synchronousGetRequest(url: String) : Response {
        val client = OkHttpClient()

        val request = Request.Builder()
            .url(url)
            .build()

        val response = client.newCall(request).execute()
        Log.d(TAG, "synchronousGetRequest( $url ) -> $response")
        return response
    }

    /**
     * Gets the code from a response.  Remember that 200 is good.  Anything
     * in the 400s is bad.
     *
     * @return      The response number.
     */
    fun getCodeFromResponse(response: Response) : Int {
        return response.code
    }

    /**
     * So you have a response, good.  And you want the body, yeah?
     * This is the function to extract the body from that response.
     *
     * @return      The entire body (as a string) from an GET response.
     *              Note that this could be null.
     */
    fun getBodyFromResponse(response: Response) : String? {
        return response.body?.string()
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


private const val TAG = "OkHttpUtils"