package com.sleepfuriously.paulsapp

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import java.time.ZonedDateTime


/**
 * Returns the current time in human-readable string.
 */
fun getTime(): String {
    val zdt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        ZonedDateTime.now()
    } else {
        Log.e(TAG, "getTime() - unhandled build version. Crashing now!")
        TODO("VERSION.SDK_INT < O")
    }
    return "${zdt.hour}:${zdt.minute}:${zdt.second}"
}


fun getVersionName(context: Context): String {
    val packageInfo: PackageInfo
    try {
        packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
    } catch (e: PackageManager.NameNotFoundException) {
        throw RuntimeException(e)
    }
    return packageInfo.versionName
}

fun getVersionCode(context: Context): Int {
    val packageInfo: PackageInfo
    try {
        packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
    } catch (e: PackageManager.NameNotFoundException) {
        throw RuntimeException(e)
    }
    return packageInfo.versionCode
}

private const val TAG = "Utils"