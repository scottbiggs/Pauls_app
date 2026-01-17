package com.sleepfuriously.paulsapp.utils

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


/**
 * Convert raw IP address to string.
 * from:
 *      https://kodejava.org/how-do-i-convert-raw-ip-address-to-string/
 *
 * @param rawBytes raw IP address (should be array of 4 bytes).
 *
 * @return a string representation of the raw ip address.
 */
fun convertRawBytesToIpAddress(rawBytes: ByteArray): String {
    var i = 4
    val ipAddress = StringBuilder()
    for (raw in rawBytes) {
        ipAddress.append(raw.toInt() and 0xFF)
        if (--i > 0) {
            ipAddress.append(".")
        }
    }
    return ipAddress.toString()
}
