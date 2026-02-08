package app.applister.viewmodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.core.net.toUri
import app.applister.data.model.AppStore
import app.applister.data.model.StoreOpenResult

class AppDetailViewModel : ViewModel() {

    fun launchApp(context: Context, packageName: String): Boolean {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        return true
    }

    fun openAppStore(context: Context, packageName: String, store: AppStore = AppStore.PLAY_STORE): StoreOpenResult {
        return store.openApp(context, packageName)
    }

    fun openAppInfo(context: Context, packageName: String) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = "package:$packageName".toUri()
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun uninstallApp(context: Context, packageName: String) {
        val intent = Intent(Intent.ACTION_DELETE).apply {
            data = "package:$packageName".toUri()
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun shareApp(context: Context, packageName: String, appName: String, store: AppStore = AppStore.PLAY_STORE) {
        val storeUrl = when (store) {
            AppStore.PLAY_STORE -> "https://play.google.com/store/apps/details?id=$packageName"
            AppStore.FDROID -> "https://f-droid.org/packages/$packageName"
            AppStore.AMAZON -> "https://www.amazon.com/gp/mas/dl/android?p=$packageName"
            AppStore.SAMSUNG -> "https://galaxystore.samsung.com/detail/$packageName"
            AppStore.HUAWEI -> "https://appgallery.huawei.com/app/$packageName"
        }
        val shareText = "$appName\n$storeUrl"
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
            putExtra(Intent.EXTRA_SUBJECT, appName)
        }
        context.startActivity(Intent.createChooser(intent, "Share $appName"))
    }
}
