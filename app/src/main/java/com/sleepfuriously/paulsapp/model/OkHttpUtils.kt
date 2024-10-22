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

private const val TAG = "OkHttpUtils"