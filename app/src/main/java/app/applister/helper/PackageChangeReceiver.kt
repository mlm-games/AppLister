package app.applister.helper

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat

class PackageChangeReceiver(
    private val onPackagesChanged: () -> Unit
) : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_PACKAGE_ADDED,
            Intent.ACTION_PACKAGE_REMOVED,
            Intent.ACTION_PACKAGE_REPLACED,
            Intent.ACTION_PACKAGE_CHANGED -> onPackagesChanged()
        }
    }

    companion object {
        fun filter() = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }
    }
}

fun Context.registerPackageChanges(onChange: () -> Unit): PackageChangeReceiver {
    val receiver = PackageChangeReceiver(onChange)
    ContextCompat.registerReceiver(
        this,
        receiver,
        PackageChangeReceiver.filter(),
        ContextCompat.RECEIVER_NOT_EXPORTED
    )
    return receiver
}