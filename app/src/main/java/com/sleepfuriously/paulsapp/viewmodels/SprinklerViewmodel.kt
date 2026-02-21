package com.sleepfuriously.paulsapp.viewmodels

import android.content.Context
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import com.sleepfuriously.paulsapp.R
import com.sleepfuriously.paulsapp.compose.sprinkler.SprinklerOff
import com.sleepfuriously.paulsapp.compose.sprinkler.SprinklerOn
import dev.romainguy.kotlin.math.Float2

/**
 * Viewmodel for Paul's sprinkler system.
 *
 * This system is accessed through a web interface.  So this is pretty easy:
 * just open a webview to the url that his sprinkler system accesses.
 */
class SprinklerViewmodel : ViewModel(), MyViewModelInterface {
    override fun getTitle(ctx: Context): String {
        return ctx.getString(R.string.sprinkler_main_title)
    }

    override fun getSelectedIcon(): ImageVector {
        return SprinklerOn
    }

    override fun getUnselectedIcon(): ImageVector {
        return SprinklerOff
    }

    override fun getIconPos(): Float2? {
        // todo
        return null
    }

    /**
     * Call this to get the URL used by the sprinkler system (its dashboard).
     */
    fun getUrl() : String {
        return URL
    }
}

private const val URL = "https://www.mysmartyard.com/dashboard"