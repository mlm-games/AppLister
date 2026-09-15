package app.applister.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class RestoreError {
    MALFORMED_JSON
}

enum class VersionStatus {
    INSTALLED,
    MISSING,
    OUTDATED,
    NEWER_THAN_BACKUP,
    VERSION_DIFFERS
}

data class RestoreResult(
    val totalApps: Int,
    val foundApps: List<RestoredApp>,
    val missingApps: List<RestoredApp>,
    val error: RestoreError? = null,
    val skippedInvalidEntries: Int = 0
) {
    val isDecodeError: Boolean get() = error != null
}

data class RestoredApp(
    val packageName: String,
    val appName: String,
    val versionInBackup: String? = null,
    val versionStatus: VersionStatus = VersionStatus.INSTALLED
)
