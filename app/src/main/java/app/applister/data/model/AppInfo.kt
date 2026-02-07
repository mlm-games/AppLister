package app.applister.data.model

import kotlinx.serialization.Serializable

@Serializable
data class AppInfo(
    val packageName: String,
    val appName: String,
    val versionName: String?,
    val versionCode: Long,
    val isSystemApp: Boolean,
    val installTimeMillis: Long,
    val updateTimeMillis: Long,
    val apkSizeBytes: Long,
    val isSelected: Boolean = false
) {
    val installDateFormatted: String
        get() = formatTimestamp(installTimeMillis)

    val updateDateFormatted: String
        get() = formatTimestamp(updateTimeMillis)

    val apkSizeFormatted: String
        get() = formatSize(apkSizeBytes)

    companion object {
        private fun formatTimestamp(millis: Long): String {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
            return sdf.format(java.util.Date(millis))
        }

        private fun formatSize(bytes: Long): String {
            if (bytes <= 0) return "0 B"
            val units = arrayOf("B", "KB", "MB", "GB")
            val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
            val idx = digitGroups.coerceIn(0, units.lastIndex)
            return "%.1f %s".format(bytes / Math.pow(1024.0, idx.toDouble()), units[idx])
        }
    }
}
