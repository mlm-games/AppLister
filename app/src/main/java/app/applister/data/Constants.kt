package app.applister.data

object Constants {
    const val BACKUP_DIR = "backups"
    const val BACKUP_PREFIX = "applister-backup"

    const val MAX_AUTO_BACKUPS = 30

    const val BACKUP_SCHEMA_VERSION = 1

    object ExportFormat {
        const val MARKDOWN = 0
        const val PLAIN_TEXT = 1
        const val JSON = 2
        const val HTML = 3

        val ALL = intArrayOf(MARKDOWN, PLAIN_TEXT, JSON, HTML)

        fun coerce(format: Int): Int = if (format in ALL) format else MARKDOWN

        fun isRestorable(format: Int): Boolean = coerce(format) == JSON

        fun extension(format: Int): String = when (coerce(format)) {
            MARKDOWN -> "md"
            PLAIN_TEXT -> "txt"
            JSON -> "json"
            HTML -> "html"
            else -> "md"
        }

        fun displayName(format: Int): String = when (coerce(format)) {
            MARKDOWN -> "Markdown"
            PLAIN_TEXT -> "Plain Text"
            JSON -> "JSON"
            HTML -> "HTML"
            else -> "Markdown"
        }

        fun mimeType(format: Int): String = when (coerce(format)) {
            MARKDOWN -> "text/markdown"
            PLAIN_TEXT -> "text/plain"
            JSON -> "application/json"
            HTML -> "text/html"
            else -> "text/plain"
        }
    }
}
