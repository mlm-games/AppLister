package app.applister.data.model

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.net.toUri

enum class AppStore(val displayName: String) {
    PLAY_STORE("Google Play"),
    FDROID("F-Droid"),
    AMAZON("Amazon Appstore"),
    SAMSUNG("Galaxy Store"),
    HUAWEI("AppGallery");

    fun openApp(context: Context, packageName: String): StoreOpenResult {
        if (!packageName.isValidPackageName()) {
            return StoreOpenResult.Error("Invalid package name")
        }
        val pm = context.packageManager
        val safePackage = Uri.encode(packageName)

        val deepLinkIntent = getDeepLinkIntent(safePackage)
        val webIntent = getWebIntent(safePackage)

        val deepLinkOk = canHandle(pm, deepLinkIntent)
        if (deepLinkOk && tryStart(context, deepLinkIntent)) return StoreOpenResult.Success
        if (tryStart(context, webIntent)) return StoreOpenResult.Success
        return if (canHandle(pm, webIntent)) {
            StoreOpenResult.Error("Could not open store link")
        } else {
            StoreOpenResult.NoAppFound
        }
    }

    private fun canHandle(pm: PackageManager, intent: Intent): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.resolveActivity(
                    intent,
                    PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong())
                ) != null
            } else {
                @Suppress("DEPRECATION")
                pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun tryStart(context: Context, intent: Intent): Boolean {
        return try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        } catch (_: ActivityNotFoundException) {
            false
        } catch (_: SecurityException) {
            false
        }
    }

    private fun getDeepLinkIntent(encodedPackage: String): Intent {
        val uri = when (this) {
            PLAY_STORE -> "market://details?id=$encodedPackage".toUri()
            FDROID -> "fdroid.app://details?id=$encodedPackage".toUri()
            AMAZON -> "amzn://apps/android?p=$encodedPackage".toUri()
            SAMSUNG -> "samsungapps://ProductDetail/$encodedPackage".toUri()
            HUAWEI -> "appmarket://details?id=$encodedPackage".toUri()
        }
        return Intent(Intent.ACTION_VIEW, uri)
    }

    private fun getWebIntent(encodedPackage: String): Intent {
        val url = when (this) {
            PLAY_STORE -> "https://play.google.com/store/apps/details?id=$encodedPackage"
            FDROID -> "https://f-droid.org/packages/$encodedPackage"
            AMAZON -> "https://www.amazon.com/gp/mas/dl/android?p=$encodedPackage"
            SAMSUNG -> "https://galaxystore.samsung.com/detail/$encodedPackage"
            HUAWEI -> "https://appgallery.huawei.com/app/$encodedPackage"
        }
        return Intent(Intent.ACTION_VIEW, url.toUri())
    }

    fun getMissingStoreMessage(): String {
        return when (this) {
            PLAY_STORE -> "Google Play Store not installed"
            FDROID -> "F-Droid not installed or app not in repository"
            AMAZON -> "Amazon Appstore not installed"
            SAMSUNG -> "Galaxy Store not installed"
            HUAWEI -> "AppGallery not installed"
        }
    }

    fun getGuidanceMessage(): String {
        return when (this) {
            PLAY_STORE -> "Install Google Play Store or enable 'Open supported links' in browser settings"
            FDROID -> "Install F-Droid client or check if the app is available in your repositories"
            AMAZON -> "Install Amazon Appstore from Amazon's website"
            SAMSUNG -> "Galaxy Store is only available on Samsung devices"
            HUAWEI -> "AppGallery is only available on Huawei devices"
        }
    }

    companion object {
        fun fromIndex(index: Int): AppStore = entries.getOrElse(index) { PLAY_STORE }
    }
}

sealed class StoreOpenResult {
    object Success : StoreOpenResult()
    object NoAppFound : StoreOpenResult()
    data class Error(val message: String) : StoreOpenResult()
}
