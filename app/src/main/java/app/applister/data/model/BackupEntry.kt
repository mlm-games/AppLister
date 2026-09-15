package app.applister.data.model

import app.applister.data.Constants
import kotlinx.serialization.Serializable

private val PACKAGE_NAME_REGEX = Regex("^[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+$")

fun String.isValidPackageName(): Boolean =
    length in 2..255 && PACKAGE_NAME_REGEX.matches(this)

fun List<BackupAppEntry>.deduplicated(): List<BackupAppEntry> = distinctBy { it.packageName }

@Serializable
data class BackupBundle(
    val schemaVersion: Int = Constants.BACKUP_SCHEMA_VERSION,
    val createdAt: Long = 0L,
    val deviceName: String = "",
    val androidVersion: String = "",
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
