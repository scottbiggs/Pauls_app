package com.sleepfuriously.paulsapp.model

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/**
 * General utility functions that deal with connectivity, primarily
 * wifi (for now).
 */


/**
 * Checks to see if wifi is enabled and operating currently.
 * Tested - works!
 *
 * WARNING: As this may take an indeterminate amount of time, you should
 * probably call this off the main thread.
 *
 * from
 *      https://stackoverflow.com/a/69472532/624814
 */
fun isConnectivityWifiWorking(ctx: Context) : Boolean {
    val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val capabilities = cm.getNetworkCapabilities(cm.activeNetwork)

    val isWorking = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
    return isWorking
}

