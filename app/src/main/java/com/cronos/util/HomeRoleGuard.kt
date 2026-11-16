package com.cronos.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

class HomeRoleGuard(private val context: Context) {
    fun isCurrentHomeApp(): Boolean {
        val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val resolved = context.packageManager.resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
        return resolved?.activityInfo?.packageName == context.packageName
    }

    fun shouldStopClaimingHomeRole(crashCount: Int): Boolean = crashCount >= 3
}
