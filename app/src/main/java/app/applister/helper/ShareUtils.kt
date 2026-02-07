package app.applister.helper

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

object ShareUtils {

    fun shareTextFile(context: Context, fileName: String, content: String, mimeType: String = "text/plain") {
        val file = File(context.cacheDir, fileName)
        file.writeText(content)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share App List"))
    }

    fun shareFile(context: Context, file: File, mimeType: String = "application/octet-stream") {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Backup"))
    }

    fun shareText(context: Context, text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "Share"))
    }

    fun getMimeType(format: Int): String = when (format) {
        0 -> "text/markdown"
        1 -> "text/plain"
        2 -> "application/json"
        3 -> "text/html"
        else -> "text/plain"
    }

    fun getExtension(format: Int): String = when (format) {
        0 -> "md"
        1 -> "txt"
        2 -> "json"
        3 -> "html"
        else -> "txt"
    }
}
