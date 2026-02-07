package app.applister.data.repository

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import app.applister.data.db.AppDatabase
import app.applister.data.model.AppInfo
import app.applister.data.model.FilterMode
import app.applister.data.model.SortMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class AppListRepository(
    private val context: Context,
    private val db: AppDatabase
) {
    suspend fun getInstalledApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val packages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.getInstalledPackages(0)
        }

        packages.mapNotNull { pkg -> packageToAppInfo(pm, pkg) }
    }

    private fun packageToAppInfo(pm: PackageManager, pkg: PackageInfo): AppInfo? {
        return try {
            val appInfo = pkg.applicationInfo ?: return null
            val appName = appInfo.loadLabel(pm).toString()
            val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pkg.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                pkg.versionCode.toLong()
            }

            val apkSize = try {
                File(appInfo.sourceDir).length()
            } catch (_: Exception) {
                0L
            }

            AppInfo(
                packageName = pkg.packageName,
                appName = appName,
                versionName = pkg.versionName,
                versionCode = versionCode,
                isSystemApp = isSystem,
                installTimeMillis = pkg.firstInstallTime,
                updateTimeMillis = pkg.lastUpdateTime,
                apkSizeBytes = apkSize
            )
        } catch (_: Exception) {
            null
        }
    }

    fun isPackageInstalled(packageName: String): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    packageName,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(packageName, 0)
            }
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun sortApps(apps: List<AppInfo>, sortMode: SortMode): List<AppInfo> {
        return when (sortMode) {
            SortMode.NAME_ASC -> apps.sortedBy { it.appName.lowercase() }
            SortMode.NAME_DESC -> apps.sortedByDescending { it.appName.lowercase() }
            SortMode.INSTALL_DATE_NEWEST -> apps.sortedByDescending { it.installTimeMillis }
            SortMode.INSTALL_DATE_OLDEST -> apps.sortedBy { it.installTimeMillis }
            SortMode.UPDATE_DATE_NEWEST -> apps.sortedByDescending { it.updateTimeMillis }
            SortMode.UPDATE_DATE_OLDEST -> apps.sortedBy { it.updateTimeMillis }
            SortMode.SIZE_LARGEST -> apps.sortedByDescending { it.apkSizeBytes }
            SortMode.SIZE_SMALLEST -> apps.sortedBy { it.apkSizeBytes }
            SortMode.PACKAGE_NAME -> apps.sortedBy { it.packageName.lowercase() }
        }
    }

    fun filterApps(apps: List<AppInfo>, filter: FilterMode): List<AppInfo> {
        return when (filter) {
            FilterMode.ALL -> apps
            FilterMode.USER -> apps.filter { !it.isSystemApp }
            FilterMode.SYSTEM -> apps.filter { it.isSystemApp }
        }
    }

    fun searchApps(apps: List<AppInfo>, query: String): List<AppInfo> {
        if (query.isBlank()) return apps
        val q = query.trim().lowercase()
        return apps.filter {
            it.appName.lowercase().contains(q) || it.packageName.lowercase().contains(q)
        }
    }
}
