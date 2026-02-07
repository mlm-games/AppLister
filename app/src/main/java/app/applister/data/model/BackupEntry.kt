package app.applister.data.model

import kotlinx.serialization.Serializable

@Serializable
data class BackupBundle(
    val createdAt: Long = System.currentTimeMillis(),
    val deviceName: String = android.os.Build.MODEL,
    val androidVersion: String = android.os.Build.VERSION.RELEASE,
    val apps: List<BackupAppEntry>
)

@Serializable
data class BackupAppEntry(
    val packageName: String,
    val appName: String,
    val versionName: String? = null,
    val versionCode: Long = 0,
    val isSystemApp: Boolean = false
)
