package com.sleepfuriously.paulsapp.viewmodels

import android.content.Context
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import com.sleepfuriously.paulsapp.R
import com.sleepfuriously.paulsapp.compose.pool.PoolOff
import com.sleepfuriously.paulsapp.compose.pool.PoolOn

/**
 * Viewmodel for the pool system.
 */
class PoolViewmodel: ViewModel(), MyViewModelInterface {
    override fun getTitle(ctx: Context): String {
        return ctx.getString(R.string.pool_main_title)
    }

    override fun getSelectedIcon(): ImageVector {
        return PoolOn
    }

    override fun getUnselectedIcon(): ImageVector {
        return PoolOff
    }

    /**
     * Call this to get the URL used by the pool system (its dashboard).
     */
    fun getUrl() : String {
        return URL
    }
}

/** should redirect to signin or dashboard */
private const val URL = "https://intellicenter2.com"
