package com.sleepfuriously.paulsapp.viewmodels

import android.content.Context
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import com.sleepfuriously.paulsapp.R
import com.sleepfuriously.paulsapp.compose.climate.ClimateOff
import com.sleepfuriously.paulsapp.compose.climate.ClimateOn
import dev.romainguy.kotlin.math.Float2

/**
 * todo:  describe class' purpose and context
 */
class ClimateViewmodel: ViewModel(), MyViewModelInterface {
    override fun getTitle(ctx: Context): String {
        return ctx.getString(R.string.climate_main_title)
    }

    override fun getSelectedIcon(): ImageVector {
        return ClimateOn
    }

    override fun getUnselectedIcon(): ImageVector {
        return ClimateOff
    }

    override fun getIconPos(): Float2? {
        return null
    }
}