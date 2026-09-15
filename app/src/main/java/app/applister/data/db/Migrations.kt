package app.applister.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "DELETE FROM backup_records WHERE id NOT IN " +
                "(SELECT MIN(id) FROM backup_records GROUP BY fileName)"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `backup_records_new` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`fileName` TEXT NOT NULL, " +
                "`filePath` TEXT NOT NULL, " +
                "`createdAt` INTEGER NOT NULL, " +
                "`appCount` INTEGER NOT NULL, " +
                "`format` TEXT NOT NULL, " +
                "`isAutoBackup` INTEGER NOT NULL)"
        )
        db.execSQL(
            "INSERT INTO `backup_records_new` " +
                "(`id`, `fileName`, `filePath`, `createdAt`, `appCount`, `format`, `isAutoBackup`) " +
                "SELECT `id`, `fileName`, `filePath`, `createdAt`, `appCount`, `format`, `isAutoBackup` " +
                "FROM `backup_records`"
        )
        db.execSQL("DROP TABLE `backup_records`")
        db.execSQL("ALTER TABLE `backup_records_new` RENAME TO `backup_records`")
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_backup_records_fileName` " +
                "ON `backup_records` (`fileName`)"
        )
    }
}
