package app.applister.data.model

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri

enum class AppStore(val displayName: String) {
    PLAY_STORE("Google Play"),
    FDROID("F-Droid"),
    AMAZON("Amazon Appstore"),
    SAMSUNG("Galaxy Store"),
    HUAWEI("AppGallery");

    fun openApp(context: Context, packageName: String): StoreOpenResult {
        val pm = context.packageManager

        val deepLinkIntent = getDeepLinkIntent(packageName)
        val canHandleDeepLink = deepLinkIntent.resolveActivity(pm) != null

        val intentToUse = if (canHandleDeepLink) {
            deepLinkIntent
        } else {
            getWebIntent(packageName)
        }

        return try {
            context.startActivity(intentToUse.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            StoreOpenResult.Success
        } catch (_: ActivityNotFoundException) {
            StoreOpenResult.NoAppFound
        } catch (e: Exception) {
            StoreOpenResult.Error(e.message ?: "Unknown error")
        }
    }

    private fun getDeepLinkIntent(packageName: String): Intent {
        val uri = when (this) {
            PLAY_STORE -> "market://details?id=$packageName".toUri()
            FDROID -> "fdroid.app://details?id=$packageName".toUri()
            AMAZON -> "amzn://apps/android?p=$packageName".toUri()
            SAMSUNG -> "samsungapps://ProductDetail/$packageName".toUri()
            HUAWEI -> "appmarket://details?id=$packageName".toUri()
        }
        return Intent(Intent.ACTION_VIEW, uri)
    }

    private fun getWebIntent(packageName: String): Intent {
        val url = when (this) {
            PLAY_STORE -> "https://play.google.com/store/apps/details?id=$packageName"
            FDROID -> "https://f-droid.org/packages/$packageName"
            AMAZON -> "https://www.amazon.com/gp/mas/dl/android?p=$packageName"
            SAMSUNG -> "https://galaxystore.samsung.com/detail/$packageName"
            HUAWEI -> "https://appgallery.huawei.com/app/$packageName"
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
