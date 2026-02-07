package app.applister.data.repository

import android.content.Context
import app.applister.data.Constants
import app.applister.data.db.AppDatabase
import app.applister.data.db.BackupRecord
import app.applister.data.model.AppInfo
import app.applister.data.model.BackupAppEntry
import app.applister.data.model.BackupBundle
import app.applister.data.model.RestoreResult
import app.applister.data.model.RestoredApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BackupRepository(
    private val context: Context,
    private val db: AppDatabase,
    private val appListRepo: AppListRepository
) {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    private fun backupDir(): File {
        val dir = File(context.getExternalFilesDir(null), Constants.BACKUP_DIR)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun allBackups(): Flow<List<BackupRecord>> = db.backupDao().allBackups()

    suspend fun createBackup(
        apps: List<AppInfo>,
        format: Int = Constants.ExportFormat.MARKDOWN,
        isAuto: Boolean = false
    ): BackupRecord? = withContext(Dispatchers.IO) {
        try {
            val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
                .format(Date())
            val extension = formatExtension(format)
            val prefix = if (isAuto) "${Constants.BACKUP_PREFIX}-auto" else Constants.BACKUP_PREFIX
            val fileName = "${prefix}_${timestamp}.${extension}"

            val content = formatApps(apps, format)
            val file = File(backupDir(), fileName)
            file.writeText(content)

            if (isAuto) {
                cleanupOldAutoBackups()
            }

            val record = BackupRecord(
                fileName = fileName,
                filePath = file.absolutePath,
                appCount = apps.size,
                format = formatName(format),
                isAutoBackup = isAuto
            )
            val id = db.backupDao().insert(record)
            record.copy(id = id)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private suspend fun cleanupOldAutoBackups() {
        val autoBackups = db.backupDao().autoBackups()
        if (autoBackups.size > Constants.MAX_AUTO_BACKUPS) {
            val toDelete = autoBackups.drop(Constants.MAX_AUTO_BACKUPS)
            toDelete.forEach { record ->
                try {
                    File(record.filePath).delete()
                } catch (_: Exception) { }
            }
            db.backupDao().deleteByIds(toDelete.map { it.id })
        }
    }

    suspend fun deleteBackup(record: BackupRecord) = withContext(Dispatchers.IO) {
        try {
            File(record.filePath).delete()
        } catch (_: Exception) { }
        db.backupDao().delete(record)
    }

    suspend fun restoreFromJson(jsonContent: String): RestoreResult = withContext(Dispatchers.IO) {
        val bundle = json.decodeFromString<BackupBundle>(jsonContent)
        val found = mutableListOf<RestoredApp>()
        val missing = mutableListOf<RestoredApp>()

        bundle.apps.forEach { entry ->
            val restoredApp = RestoredApp(
                packageName = entry.packageName,
                appName = entry.appName,
                versionInBackup = entry.versionName
            )
            if (appListRepo.isPackageInstalled(entry.packageName)) {
                found.add(restoredApp)
            } else {
                missing.add(restoredApp)
            }
        }

        RestoreResult(
            totalApps = bundle.apps.size,
            foundApps = found,
            missingApps = missing
        )
    }

    fun formatApps(apps: List<AppInfo>, format: Int): String {
        return when (format) {
            Constants.ExportFormat.MARKDOWN -> formatMarkdown(apps)
            Constants.ExportFormat.PLAIN_TEXT -> formatPlainText(apps)
            Constants.ExportFormat.JSON -> formatJson(apps)
            Constants.ExportFormat.HTML -> formatHtml(apps)
            else -> formatMarkdown(apps)
        }
    }

    private fun formatMarkdown(apps: List<AppInfo>): String {
        val sb = StringBuilder()
        sb.appendLine("# My App List")
        sb.appendLine()
        sb.appendLine("Generated: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}")
        sb.appendLine("Device: ${android.os.Build.MODEL}")
        sb.appendLine("Android: ${android.os.Build.VERSION.RELEASE}")
        sb.appendLine("Total apps: ${apps.size}")
        sb.appendLine()
        sb.appendLine("| # | App Name | Package Name | Version | Installed | Size |")
        sb.appendLine("|---|----------|--------------|---------|-----------|------|")
        apps.forEachIndexed { index, app ->
            sb.appendLine("| ${index + 1} | ${app.appName} | ${app.packageName} | ${app.versionName ?: "-"} | ${app.installDateFormatted} | ${app.apkSizeFormatted} |")
        }
        return sb.toString()
    }

    private fun formatPlainText(apps: List<AppInfo>): String {
        val sb = StringBuilder()
        sb.appendLine("My App List")
        sb.appendLine("Generated: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}")
        sb.appendLine("Device: ${android.os.Build.MODEL}")
        sb.appendLine("Total apps: ${apps.size}")
        sb.appendLine("─".repeat(60))
        apps.forEachIndexed { index, app ->
            sb.appendLine("${index + 1}. ${app.appName}")
            sb.appendLine("   Package: ${app.packageName}")
            sb.appendLine("   Version: ${app.versionName ?: "-"}")
            sb.appendLine("   Installed: ${app.installDateFormatted}")
            sb.appendLine("   Size: ${app.apkSizeFormatted}")
            sb.appendLine()
        }
        return sb.toString()
    }

    private fun formatJson(apps: List<AppInfo>): String {
        val bundle = BackupBundle(
            apps = apps.map { app ->
                BackupAppEntry(
                    packageName = app.packageName,
                    appName = app.appName,
                    versionName = app.versionName,
                    versionCode = app.versionCode,
                    isSystemApp = app.isSystemApp
                )
            }
        )
        return json.encodeToString(bundle)
    }

    private fun formatHtml(apps: List<AppInfo>): String {
        val sb = StringBuilder()
        sb.appendLine("<!DOCTYPE html>")
        sb.appendLine("<html><head><meta charset=\"utf-8\">")
        sb.appendLine("<title>My App List</title>")
        sb.appendLine("<style>")
        sb.appendLine("body { font-family: -apple-system, BlinkMacSystemFont, sans-serif; max-width: 900px; margin: 0 auto; padding: 20px; background: #f5f5f5; }")
        sb.appendLine("h1 { color: #00695C; }")
        sb.appendLine(".meta { color: #666; margin-bottom: 20px; }")
        sb.appendLine("table { width: 100%; border-collapse: collapse; background: white; border-radius: 8px; overflow: hidden; box-shadow: 0 1px 3px rgba(0,0,0,0.1); }")
        sb.appendLine("th { background: #00695C; color: white; padding: 12px; text-align: left; }")
        sb.appendLine("td { padding: 10px 12px; border-bottom: 1px solid #eee; }")
        sb.appendLine("tr:hover { background: #f0f0f0; }")
        sb.appendLine("</style></head><body>")
        sb.appendLine("<h1>My App List</h1>")
        sb.appendLine("<div class=\"meta\">")
        sb.appendLine("<p>Generated: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}</p>")
        sb.appendLine("<p>Device: ${android.os.Build.MODEL} · Android ${android.os.Build.VERSION.RELEASE}</p>")
        sb.appendLine("<p>Total apps: ${apps.size}</p>")
        sb.appendLine("</div>")
        sb.appendLine("<table>")
        sb.appendLine("<tr><th>#</th><th>App Name</th><th>Package</th><th>Version</th><th>Installed</th><th>Size</th></tr>")
        apps.forEachIndexed { index, app ->
            sb.appendLine("<tr><td>${index + 1}</td><td>${app.appName}</td><td><code>${app.packageName}</code></td><td>${app.versionName ?: "-"}</td><td>${app.installDateFormatted}</td><td>${app.apkSizeFormatted}</td></tr>")
        }
        sb.appendLine("</table></body></html>")
        return sb.toString()
    }

    private fun formatExtension(format: Int): String = when (format) {
        Constants.ExportFormat.MARKDOWN -> "md"
        Constants.ExportFormat.PLAIN_TEXT -> "txt"
        Constants.ExportFormat.JSON -> "json"
        Constants.ExportFormat.HTML -> "html"
        else -> "md"
    }

    private fun formatName(format: Int): String = when (format) {
        Constants.ExportFormat.MARKDOWN -> "Markdown"
        Constants.ExportFormat.PLAIN_TEXT -> "Plain Text"
        Constants.ExportFormat.JSON -> "JSON"
        Constants.ExportFormat.HTML -> "HTML"
        else -> "Markdown"
    }

    fun getBackupFile(record: BackupRecord): File = File(record.filePath)
}
