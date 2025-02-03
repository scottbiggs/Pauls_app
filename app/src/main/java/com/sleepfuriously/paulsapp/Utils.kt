package com.sleepfuriously.paulsapp

import android.os.Build
import java.time.ZonedDateTime


/**
 * Returns the current time in human-readable string.
 */
fun getTime(): String {
    val zdt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        ZonedDateTime.now()
    } else {
        TODO("VERSION.SDK_INT < O")
    }
    return "${zdt.hour}:${zdt.minute}:${zdt.second}"
}
