package app.applister.data

object Constants {
    const val BACKUP_DIR = "backups"
    const val BACKUP_PREFIX = "myapplist-backup"
    const val MAX_AUTO_BACKUPS = 30

    object ExportFormat {
        const val MARKDOWN = 0
        const val PLAIN_TEXT = 1
        const val JSON = 2
        const val HTML = 3
    }
}
