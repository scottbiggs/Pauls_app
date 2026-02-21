package com.sleepfuriously.paulsapp.viewmodels

import android.content.Context
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import com.sleepfuriously.paulsapp.R
import com.sleepfuriously.paulsapp.compose.pool.PoolOff
import com.sleepfuriously.paulsapp.compose.pool.PoolOn
import dev.romainguy.kotlin.math.Float2

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

    override fun getIconPos(): Float2? {
        // todo
        return null
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
