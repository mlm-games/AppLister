package app.applister

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import app.applister.data.repository.AppSettings
import app.applister.data.repository.AppSettingsSchema
import app.applister.data.db.AppDatabase
import app.applister.data.repository.AppListRepository
import app.applister.data.repository.BackupRepository
import io.github.mlmgames.settings.core.SettingsRepository
import io.github.mlmgames.settings.core.datastore.createSettingsDataStore

object AppGraph {
    @Volatile
    private var instance: Instance? = null
    private val lock = Any()

    private class Instance(ctx: Context) {
        val appContext: Context = ctx.applicationContext

        private val settingsDataStore: DataStore<Preferences> =
            createSettingsDataStore(appContext, "app_settings")

        val settings: SettingsRepository<AppSettings> =
            SettingsRepository(settingsDataStore, AppSettingsSchema)

        val db: AppDatabase = AppDatabase.build(appContext)

        val appListRepo: AppListRepository = AppListRepository(appContext, db)

        val backupRepo: BackupRepository = BackupRepository(appContext, db, appListRepo)
    }

    fun init(context: Context) {
        if (instance == null) {
            synchronized(lock) {
                if (instance == null) instance = Instance(context)
            }
        }
    }

    private fun ck() = instance ?: error("AppGraph.init(context) not called")

    val settings: SettingsRepository<AppSettings> get() = ck().settings
    val schema get() = AppSettingsSchema
    val db get() = ck().db
    val appListRepo get() = ck().appListRepo
    val backupRepo get() = ck().backupRepo
}
