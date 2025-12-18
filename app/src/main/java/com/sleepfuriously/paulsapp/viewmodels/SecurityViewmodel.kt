package com.sleepfuriously.paulsapp.viewmodels

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.outlined.Security
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import com.sleepfuriously.paulsapp.R
import com.sleepfuriously.paulsapp.compose.security.SecurityOff
import com.sleepfuriously.paulsapp.compose.security.SecurityOn

/**
 * Viewmodel for the security system
 */
class SecurityViewmodel : ViewModel(), MyViewModelInterface {
    override fun getTitle(ctx: Context): String {
        return ctx.getString(R.string.security_main_title)
    }

    override fun getSelectedIcon(): ImageVector {
        return SecurityOn
    }

    override fun getUnselectedIcon(): ImageVector {
        return SecurityOff
    }
}