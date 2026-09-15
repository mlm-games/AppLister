package app.applister.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BackupDao {
    @Query("SELECT * FROM backup_records ORDER BY createdAt DESC, id DESC")
    fun allBackups(): Flow<List<BackupRecord>>

    @Query("SELECT * FROM backup_records ORDER BY createdAt DESC, id DESC")
    suspend fun snapshot(): List<BackupRecord>

    @Query("SELECT * FROM backup_records WHERE isAutoBackup = 1 ORDER BY createdAt DESC, id DESC")
    suspend fun autoBackups(): List<BackupRecord>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(record: BackupRecord): Long

    @Delete
    suspend fun delete(record: BackupRecord)

    @Query("DELETE FROM backup_records WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("SELECT COUNT(*) FROM backup_records")
    suspend fun count(): Int
}
