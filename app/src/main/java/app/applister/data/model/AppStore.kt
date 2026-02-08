package app.applister.data.model

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri

enum class AppStore(val displayName: String) {
    PLAY_STORE("Google Play"),
    FDROID("F-Droid"),
    AMAZON("Amazon Appstore"),
    SAMSUNG("Galaxy Store"),
    HUAWEI("AppGallery");

    fun openApp(context: Context, packageName: String) {
        val intent = when (this) {
            PLAY_STORE -> {
                try {
                    Intent(Intent.ACTION_VIEW, "market://details?id=$packageName".toUri())
                } catch (_: Exception) {
                    Intent(Intent.ACTION_VIEW,
                        "https://play.google.com/store/apps/details?id=$packageName".toUri())
                }
            }
            FDROID -> {
                try {
                    Intent(Intent.ACTION_VIEW, "fdroid.app://details?id=$packageName".toUri())
                } catch (_: Exception) {
                    Intent(Intent.ACTION_VIEW, "https://f-droid.org/packages/$packageName".toUri())
                }
            }
            AMAZON -> {
                try {
                    Intent(Intent.ACTION_VIEW, "amzn://apps/android?p=$packageName".toUri())
                } catch (_: Exception) {
                    Intent(Intent.ACTION_VIEW,
                        "https://www.amazon.com/gp/mas/dl/android?p=$packageName".toUri())
                }
            }
            SAMSUNG -> {
                try {
                    Intent(Intent.ACTION_VIEW, "samsungapps://ProductDetail/$packageName".toUri())
                } catch (_: Exception) {
                    Intent(Intent.ACTION_VIEW,
                        "https://galaxystore.samsung.com/detail/$packageName".toUri())
                }
            }
            HUAWEI -> {
                try {
                    Intent(Intent.ACTION_VIEW, "appmarket://details?id=$packageName".toUri())
                } catch (_: Exception) {
                    Intent(Intent.ACTION_VIEW, "https://appgallery.huawei.com/app/$packageName".toUri())
                }
            }
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    companion object {
        fun fromIndex(index: Int): AppStore = entries.getOrElse(index) { PLAY_STORE }
    }
}
