package com.sleepfuriously.paulsapp.viewmodels

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.outlined.Grass
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import com.sleepfuriously.paulsapp.R

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
        return Icons.Filled.Grass
    }

    override fun getUnselectedIcon(): ImageVector {
        return Icons.Outlined.Grass
    }

    /**
     * Call this to get the URL used by the sprinkler system (its dashboard).
     */
    fun getUrl() : String {
        return URL
    }
}

private const val URL = "https://www.mysmartyard.com/dashboard"