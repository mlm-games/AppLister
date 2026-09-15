package app.applister.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [BackupRecord::class], version = 2, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun backupDao(): BackupDao

    companion object {
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "applister.db")
                .addMigrations(MIGRATION_1_2)
                .build()
    }
}
