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

    // todo: the rest of this Viewmodel!
}