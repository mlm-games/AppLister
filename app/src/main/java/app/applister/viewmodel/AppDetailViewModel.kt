package app.applister.viewmodel

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import app.applister.data.model.AppStore
import app.applister.data.model.StoreOpenResult
import app.applister.data.model.isValidPackageName

class AppDetailViewModel : ViewModel() {

    sealed interface DetailActionResult {
        data object Done : DetailActionResult
        data class Failed(val reason: String) : DetailActionResult
    }

    fun launchApp(context: Context, packageName: String): Boolean {
        if (!packageName.isValidPackageName()) return false
        val intent = try {
            context.packageManager.getLaunchIntentForPackage(packageName)
        } catch (_: Exception) {
            null
        } ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        } catch (_: SecurityException) {
            false
        } catch (_: Exception) {
            false
        }
    }

    fun openAppStore(
        context: Context,
        packageName: String,
        store: AppStore = AppStore.PLAY_STORE
    ): StoreOpenResult {
        return store.openApp(context, packageName)
    }

    fun openAppInfo(context: Context, packageName: String): DetailActionResult {
        if (!packageName.isValidPackageName()) return DetailActionResult.Failed("Invalid package")
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = "package:$packageName".toUri()
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            DetailActionResult.Done
        } catch (_: ActivityNotFoundException) {
            DetailActionResult.Failed("System settings not available")
        } catch (_: SecurityException) {
            DetailActionResult.Failed("Not allowed")
        } catch (e: Exception) {
            DetailActionResult.Failed(e.message ?: "Failed to open")
        }
    }

    fun uninstallApp(context: Context, packageName: String): DetailActionResult {
        if (!packageName.isValidPackageName()) return DetailActionResult.Failed("Invalid package")
        val intent = Intent(Intent.ACTION_DELETE).apply {
            data = "package:$packageName".toUri()
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            DetailActionResult.Done
        } catch (_: ActivityNotFoundException) {
            DetailActionResult.Failed("Uninstaller not available")
        } catch (_: SecurityException) {
            DetailActionResult.Failed("Not allowed")
        } catch (e: Exception) {
            DetailActionResult.Failed(e.message ?: "Failed to uninstall")
        }
    }

    fun shareApp(
        context: Context,
        packageName: String,
        appName: String,
        store: AppStore = AppStore.PLAY_STORE
    ): DetailActionResult {
        val safePackage = if (packageName.isValidPackageName()) packageName else return DetailActionResult.Failed("Invalid package")
        val storeUrl = when (store) {
            AppStore.PLAY_STORE -> "https://play.google.com/store/apps/details?id=$safePackage"
            AppStore.FDROID -> "https://f-droid.org/packages/$safePackage"
            AppStore.AMAZON -> "https://www.amazon.com/gp/mas/dl/android?p=$safePackage"
            AppStore.SAMSUNG -> "https://galaxystore.samsung.com/detail/$safePackage"
            AppStore.HUAWEI -> "https://appgallery.huawei.com/app/$safePackage"
        }
        val shareText = "$appName\n$storeUrl"
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
            putExtra(Intent.EXTRA_SUBJECT, appName)
            clipData = ClipData.newPlainText("share", shareText)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            val chooser = Intent.createChooser(send, null).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            DetailActionResult.Done
        } catch (_: ActivityNotFoundException) {
            DetailActionResult.Failed("No share target")
        } catch (e: Exception) {
            DetailActionResult.Failed(e.message ?: "Failed to share")
        }
    }
}
