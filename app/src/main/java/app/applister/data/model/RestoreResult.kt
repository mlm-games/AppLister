package app.applister.data.model

data class RestoreResult(
    val totalApps: Int,
    val foundApps: List<RestoredApp>,
    val missingApps: List<RestoredApp>
)

data class RestoredApp(
    val packageName: String,
    val appName: String,
    val versionInBackup: String?
)
