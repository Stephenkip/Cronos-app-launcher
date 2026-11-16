package com.cronos.data

import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.LauncherApps
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

data class AppInfo(
    val packageName: String,
    val label: String,
    val icon: ImageBitmap? = null,
)

class AppRepository(private val context: Context) {
    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    val apps: StateFlow<List<AppInfo>> = _apps.asStateFlow()

    private val launcherApps = context.getSystemService(LauncherApps::class.java)
    private val callback = object : LauncherApps.Callback() {
        override fun onPackageAdded(packageName: String, user: android.os.UserHandle) = refresh()
        override fun onPackageRemoved(packageName: String, user: android.os.UserHandle) = refresh()
        override fun onPackageChanged(packageName: String, user: android.os.UserHandle) = refresh()
        override fun onPackagesAvailable(packageNames: Array<out String>, user: android.os.UserHandle, replacing: Boolean) = refresh()
        override fun onPackagesUnavailable(packageNames: Array<out String>, user: android.os.UserHandle, replacing: Boolean) = refresh()
    }

    init {
        launcherApps?.registerCallback(callback)
        refresh()
    }

    fun refresh() {
        val packageManager = context.packageManager
        val appInfos = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            .asSequence()
            .mapNotNull { appInfo ->
                val packageName = appInfo.packageName
                if (packageName == context.packageName) return@mapNotNull null
                val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
                if (launchIntent == null) return@mapNotNull null

                val label = try {
                    packageManager.getApplicationLabel(appInfo).toString().trim()
                } catch (_: Exception) {
                    packageName
                }

                val icon = try {
                    packageManager.getApplicationIcon(packageName).toBitmap().asImageBitmap()
                } catch (_: Exception) {
                    null
                }

                AppInfo(packageName = packageName, label = label.ifBlank { packageName }, icon = icon)
            }
            .distinctBy { it.packageName }
            .sortedWith(compareBy<AppInfo> { it.label.lowercase(Locale.getDefault()) }.thenBy { it.packageName })
            .toList()

        _apps.value = appInfos
    }

    fun close() {
        launcherApps?.unregisterCallback(callback)
    }
}
