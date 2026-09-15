package app.applister.data.repository

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import android.os.Build
import app.applister.data.Constants
import app.applister.data.db.AppDatabase
import app.applister.data.db.BackupRecord
import app.applister.data.model.AppInfo
import app.applister.data.model.BackupAppEntry
import app.applister.data.model.BackupBundle
import app.applister.data.model.RestoreError
import app.applister.data.model.RestoreResult
import app.applister.data.model.RestoredApp
import app.applister.data.model.VersionStatus
import app.applister.data.model.deduplicated
import app.applister.data.model.isValidPackageName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class BackupRepository(
    private val context: Context,
    private val db: AppDatabase,
    private val appListRepo: AppListRepository
) {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    private val backupMutex = Mutex()

        private fun backupDir(): File {
        val base = context.getExternalFilesDir(null) ?: context.filesDir
        val dir = File(base, Constants.BACKUP_DIR)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun allBackups(): Flow<List<BackupRecord>> = db.backupDao().allBackups()

    suspend fun createBackup(
        apps: List<AppInfo>,
        format: Int = Constants.ExportFormat.JSON,
        isAuto: Boolean = false
    ): BackupRecord? = withContext(Dispatchers.IO) {
        backupMutex.withLock {
            try {
                val safeFormat = Constants.ExportFormat.coerce(format)
                val extension = Constants.ExportFormat.extension(safeFormat)
                val prefix = if (isAuto) "${Constants.BACKUP_PREFIX}-auto" else Constants.BACKUP_PREFIX
                val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss-SSS", Locale.US).format(Date())
                val fileName = "${prefix}_${timestamp}_${UUID.randomUUID().toString().take(8)}.$extension"

                val content = formatApps(apps, safeFormat)
                val file = File(backupDir(), fileName)
                file.parentFile?.mkdirs()
                file.writeText(content)

                if (isAuto) {
                    pruneAutoBackupsLocked(pendingInsert = 1)
                }

                val record = BackupRecord(
                    fileName = fileName,
                    filePath = file.absolutePath,
                    appCount = apps.size,
                    format = Constants.ExportFormat.displayName(safeFormat),
                    isAutoBackup = isAuto
                )
                val id = try {
                    db.backupDao().insert(record)
                } catch (e: SQLiteConstraintException) {
                    file.delete()
                    throw e
                } catch (e: Exception) {
                    file.delete()
                    throw e
                }
                record.copy(id = id)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    private suspend fun pruneAutoBackupsLocked(pendingInsert: Int = 0) {
        val autoBackups = db.backupDao().autoBackups()
        val overBy = autoBackups.size + pendingInsert - Constants.MAX_AUTO_BACKUPS
        if (overBy <= 0) return
        val toDelete = autoBackups.takeLast(overBy.coerceAtMost(autoBackups.size))
        if (toDelete.isEmpty()) return
        val deletedIds = mutableListOf<Long>()
        toDelete.forEach { record ->
            val fileDeleted = try {
                val f = File(record.filePath)
                !f.exists() || f.delete()
            } catch (_: Exception) {
                false
            }
            if (fileDeleted) deletedIds.add(record.id)
        }
        if (deletedIds.isNotEmpty()) {
            db.backupDao().deleteByIds(deletedIds)
        }
    }

    suspend fun deleteBackup(record: BackupRecord): DeleteBackupResult = withContext(Dispatchers.IO) {
        val fileDeleted = try {
            val f = File(record.filePath)
            !f.exists() || f.delete()
        } catch (_: Exception) {
            false
        }
        return@withContext try {
            if (fileDeleted) {
                db.backupDao().delete(record)
                DeleteBackupResult.Deleted
            } else {
                DeleteBackupResult.FileDeleteFailed
            }
        } catch (e: Exception) {
            e.printStackTrace()
            DeleteBackupResult.DatabaseError
        }
    }

    suspend fun restoreFromJson(jsonContent: String): RestoreResult = withContext(Dispatchers.IO) {
        val bundle = try {
            json.decodeFromString<BackupBundle>(jsonContent)
        } catch (e: SerializationException) {
            return@withContext RestoreResult(
                totalApps = 0,
                foundApps = emptyList(),
                missingApps = emptyList(),
                error = RestoreError.MALFORMED_JSON
            )
        } catch (e: IllegalArgumentException) {
            return@withContext RestoreResult(
                totalApps = 0,
                foundApps = emptyList(),
                missingApps = emptyList(),
                error = RestoreError.MALFORMED_JSON
            )
        }

        val found = mutableListOf<RestoredApp>()
        val missing = mutableListOf<RestoredApp>()
        var skippedInvalid = 0

        bundle.apps.deduplicated().forEach { entry ->
            if (!entry.packageName.isValidPackageName()) {
                skippedInvalid++
                return@forEach
            }
            val installed = appListRepo.isPackageInstalled(entry.packageName)
            val installedInfo = if (installed) appListRepo.getAppInfo(entry.packageName) else null
            val status = when {
                !installed -> VersionStatus.MISSING
                installedInfo == null -> VersionStatus.INSTALLED
                entry.versionCode > 0 && installedInfo.versionCode != entry.versionCode -> {
                    if (installedInfo.versionCode < entry.versionCode) VersionStatus.OUTDATED
                    else VersionStatus.NEWER_THAN_BACKUP
                }
                entry.versionName != null && installedInfo.versionName != entry.versionName ->
                    VersionStatus.VERSION_DIFFERS
                else -> VersionStatus.INSTALLED
            }
            val restoredApp = RestoredApp(
                packageName = entry.packageName,
                appName = entry.appName,
                versionInBackup = entry.versionName,
                versionStatus = status
            )
            if (installed) found.add(restoredApp) else missing.add(restoredApp)
        }

        RestoreResult(
            totalApps = found.size + missing.size,
            foundApps = found,
            missingApps = missing,
            error = null,
            skippedInvalidEntries = skippedInvalid
        )
    }

    fun formatApps(apps: List<AppInfo>, format: Int): String {
        return when (Constants.ExportFormat.coerce(format)) {
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
        sb.appendLine("Generated: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}")
        sb.appendLine("Device: ${escapeMdCell(Build.MODEL ?: "unknown")}")
        sb.appendLine("Android: ${escapeMdCell(Build.VERSION.RELEASE ?: "unknown")}")
        sb.appendLine("Total apps: ${apps.size}")
        sb.appendLine()
        sb.appendLine("| # | App Name | Package Name | Version | Installed | Size |")
        sb.appendLine("|---|----------|--------------|---------|-----------|------|")
        apps.forEachIndexed { index, app ->
            sb.appendLine(
                "| ${index + 1} | ${escapeMdCell(app.appName)} | " +
                    "${escapeMdCell(app.packageName)} | ${escapeMdCell(app.versionName ?: "-")} | " +
                    "${escapeMdCell(app.installDateFormatted)} | ${escapeMdCell(app.apkSizeFormatted)} |"
            )
        }
        return sb.toString()
    }

    private fun formatPlainText(apps: List<AppInfo>): String {
        val sb = StringBuilder()
        sb.appendLine("My App List")
        sb.appendLine("Generated: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}")
        sb.appendLine("Device: ${oneLine(Build.MODEL ?: "unknown")}")
        sb.appendLine("Total apps: ${apps.size}")
        sb.appendLine("─".repeat(60))
        apps.forEachIndexed { index, app ->
            sb.appendLine("${index + 1}. ${oneLine(app.appName)}")
            sb.appendLine("   Package: ${oneLine(app.packageName)}")
            sb.appendLine("   Version: ${oneLine(app.versionName ?: "-")}")
            sb.appendLine("   Installed: ${oneLine(app.installDateFormatted)}")
            sb.appendLine("   Size: ${oneLine(app.apkSizeFormatted)}")
            sb.appendLine()
        }
        return sb.toString()
    }

    private fun formatJson(apps: List<AppInfo>): String {
        val bundle = BackupBundle(
            schemaVersion = Constants.BACKUP_SCHEMA_VERSION,
            createdAt = System.currentTimeMillis(),
            deviceName = Build.MODEL ?: "unknown",
            androidVersion = Build.VERSION.RELEASE ?: "unknown",
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
        sb.appendLine("<p>Generated: ${escapeHtml(SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date()))}</p>")
        sb.appendLine("<p>Device: ${escapeHtml(Build.MODEL ?: "unknown")} · Android ${escapeHtml(Build.VERSION.RELEASE ?: "unknown")}</p>")
        sb.appendLine("<p>Total apps: ${apps.size}</p>")
        sb.appendLine("</div>")
        sb.appendLine("<table>")
        sb.appendLine("<tr><th>#</th><th>App Name</th><th>Package</th><th>Version</th><th>Installed</th><th>Size</th></tr>")
        apps.forEachIndexed { index, app ->
            sb.appendLine(
                "<tr><td>${index + 1}</td><td>${escapeHtml(app.appName)}</td>" +
                    "<td><code>${escapeHtml(app.packageName)}</code></td>" +
                    "<td>${escapeHtml(app.versionName ?: "-")}</td>" +
                    "<td>${escapeHtml(app.installDateFormatted)}</td>" +
                    "<td>${escapeHtml(app.apkSizeFormatted)}</td></tr>"
            )
        }
        sb.appendLine("</table></body></html>")
        return sb.toString()
    }

    private fun escapeMdCell(s: String): String =
        s.replace("\\", "\\\\").replace("|", "\\|").replace("\n", " ").replace("\r", "")

    private fun oneLine(s: String): String = s.replace("\n", " ").replace("\r", "")

    private fun escapeHtml(s: String): String = buildString(s.length) {
        for (c in s) {
            when (c) {
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                '&' -> append("&amp;")
                '"' -> append("&quot;")
                '\'' -> append("&#39;")
                else -> append(c)
            }
        }
    }

    fun getBackupFile(record: BackupRecord): File = File(record.filePath)

    fun isFileMissing(record: BackupRecord): Boolean = !File(record.filePath).exists()

        suspend fun readBackupContent(record: BackupRecord): String = withContext(Dispatchers.IO) {
        val file = File(record.filePath)
        if (!file.exists()) throw IOException("Backup file not found: ${record.fileName}")
        if (!file.isFile || !file.canRead()) throw IOException("Backup file unreadable: ${record.fileName}")
        if (file.length() == 0L) throw IOException("Backup file is empty: ${record.fileName}")
        file.readText()
    }

    suspend fun allBackupRecords(): List<BackupRecord> = withContext(Dispatchers.IO) {
        db.backupDao().snapshot()
    }

    suspend fun deleteStaleRecords(records: List<BackupRecord>): Int = withContext(Dispatchers.IO) {
        backupMutex.withLock {
            val stale = records.filter { !File(it.filePath).exists() }
            if (stale.isEmpty()) return@withLock 0
            db.backupDao().deleteByIds(stale.map { it.id })
            stale.size
        }
    }
}

enum class DeleteBackupResult {
    Deleted,
    FileDeleteFailed,
    DatabaseError
}
