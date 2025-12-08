package com.sleepfuriously.paulsapp.viewmodels

import android.content.Context
import androidx.compose.ui.graphics.vector.ImageVector
import com.sleepfuriously.paulsapp.MainActivity

/**
 * My Viewmodels should implement this interface so that each can return
 * a title and icons to the [MainActivity] ui.  Pretty simple and obvious.
 */
interface MyViewModelInterface {

    /**
     * Returns the title for this Viewmodel's UI.
     */
    abstract fun getTitle(ctx: Context) : String

    /**
     * Returns the icon for this Viewmodel's UI that should be displayed when
     * it is currently selected.
     */
    abstract fun getSelectedIcon() : ImageVector

    /**
     * Returns the icon for this Viewmodel's UI that should be displayed when
     * NOT selected.
     */
    abstract fun getUnselectedIcon() : ImageVector

}