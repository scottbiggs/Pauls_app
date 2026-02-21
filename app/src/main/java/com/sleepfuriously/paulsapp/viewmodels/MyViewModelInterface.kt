package com.sleepfuriously.paulsapp.viewmodels

import android.content.Context
import androidx.compose.ui.graphics.vector.ImageVector
import com.sleepfuriously.paulsapp.MainActivity
import dev.romainguy.kotlin.math.Float2

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

    /**
     * Finds the position of the viewmodel's icon.  If no position is known
     * then null is returned.
     *
     * NOTE
     *  The returned position is done as a percent (range 0..1).  Thus a
     *  0 means that that the icon is all the way to the left, and 1 is all
     *  the way to the right.  Similar for y.
     */
    abstract fun getIconPos() : Float2?

}