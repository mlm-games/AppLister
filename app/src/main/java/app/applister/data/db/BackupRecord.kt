package app.applister.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "backup_records",
    indices = [Index(value = ["fileName"], unique = true)]
)
data class BackupRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fileName: String,
    val filePath: String,
    val createdAt: Long = System.currentTimeMillis(),
    val appCount: Int,
    val format: String,
    val isAutoBackup: Boolean = false
)
