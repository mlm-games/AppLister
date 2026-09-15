package app.applister.helper

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import app.applister.data.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

object ShareUtils {

        suspend fun shareTextFile(
        context: Context,
        fileName: String,
        content: String,
        mimeType: String = "text/plain"
    ): ShareResult = withContext(Dispatchers.IO) {
        try {
            pruneStaleShares(context)
            val safeName = fileName.substringAfterLast('/').takeIf { it.isNotBlank() } ?: "applister.txt"
            val unique = "${safeName.substringBeforeLast('.')}_${UUID.randomUUID().toString().take(8)}.${safeName.substringAfterLast('.', "txt")}"
            val file = File(File(context.cacheDir, "shared"), unique)
            file.parentFile?.mkdirs()
            file.writeText(content)
            shareFileInternal(context, file, mimeType)
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
            ShareResult.Failed("Cannot share file")
        } catch (e: Exception) {
            e.printStackTrace()
            ShareResult.Failed(e.message ?: "Share failed")
        }
    }

    fun shareFile(context: Context, file: File, mimeType: String = "application/octet-stream"): ShareResult {
        return try {
            shareFileInternal(context, file, mimeType)
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
            ShareResult.Failed("File cannot be shared")
        } catch (e: Exception) {
            e.printStackTrace()
            ShareResult.Failed(e.message ?: "Share failed")
        }
    }

    private fun shareFileInternal(context: Context, file: File, mimeType: String): ShareResult {
        val uri = try {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: IllegalArgumentException) {
            throw e
        }
        val send = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newRawUri("shared file", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(send, "Share App List").apply {
            clipData = ClipData.newRawUri("shared file", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(chooser)
            ShareResult.Shared
        } catch (e: android.content.ActivityNotFoundException) {
            ShareResult.Failed("No app can share this file")
        }
    }

    fun shareText(context: Context, text: String): ShareResult {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            clipData = ClipData.newPlainText("shared text", text)
        }
        val chooser = Intent.createChooser(send, "Share").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(chooser)
            ShareResult.Shared
        } catch (e: android.content.ActivityNotFoundException) {
            ShareResult.Failed("No app can share")
        }
    }

    private fun pruneStaleShares(context: Context) {
        try {
            val dir = File(context.cacheDir, "shared")
            if (!dir.isDirectory) return
            val cutoff = System.currentTimeMillis() - 24 * 60 * 60 * 1000L
            dir.listFiles()?.forEach { f ->
                try {
                    if (f.isFile && f.lastModified() < cutoff) f.delete()
                } catch (_: Exception) { }
            }
        } catch (_: Exception) { }
    }

    fun getMimeType(format: Int): String = Constants.ExportFormat.mimeType(format)

    fun getExtension(format: Int): String = Constants.ExportFormat.extension(format)
}

sealed interface ShareResult {
    data object Shared : ShareResult
    data class Failed(val reason: String) : ShareResult
}
