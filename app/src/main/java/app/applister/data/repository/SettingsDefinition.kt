package app.applister.data.repository

import app.applister.data.Constants
import app.applister.data.model.AppStore
import app.applister.data.model.FilterMode
import app.applister.data.model.SortMode
import io.github.mlmgames.settings.core.annotations.CategoryDefinition
import io.github.mlmgames.settings.core.annotations.Setting
import io.github.mlmgames.settings.core.types.Dropdown
import io.github.mlmgames.settings.core.types.Toggle

@CategoryDefinition(order = 0)
object General

@CategoryDefinition(order = 1)
object Appearance

@CategoryDefinition(order = 2)
object Backup

data class AppSettings(
    @Setting(
        title = "Default Sort",
        description = "Default sorting order for app list",
        category = General::class,
        type = Dropdown::class,
        options = ["Name (A→Z)", "Name (Z→A)", "Install Date (Newest)", "Install Date (Oldest)",
            "Updated (Newest)", "Updated (Oldest)", "Size (Largest)", "Size (Smallest)", "Package Name"]
    )
    val defaultSort: Int = 0,

    @Setting(
        title = "Default Filter",
        description = "Which apps to show by default",
        category = General::class,
        type = Dropdown::class,
        options = ["All Apps", "User Apps", "System Apps"]
    )
    val defaultFilter: Int = 0,

    @Setting(
        title = "Preferred app store",
        description = "Store to open when getting apps",
        category = General::class,
        type = Dropdown::class,
        options = ["Google Play", "F-Droid", "Amazon Appstore", "Galaxy Store", "AppGallery"]
    )
    val preferredStore: Int = 0,

    @Setting(
        title = "Show package name",
        description = "Show package name under app name in the list",
        category = General::class,
        type = Toggle::class
    )
    val showPackageName: Boolean = true,

    @Setting(
        title = "Show app size",
        description = "Show APK size in the list",
        category = General::class,
        type = Toggle::class
    )
    val showAppSize: Boolean = false,

    @Setting(
        title = "Theme",
        category = Appearance::class,
        type = Dropdown::class,
        options = ["System", "Light", "Dark"]
    )
    val themeMode: Int = 0,

    @Setting(
        title = "Aurora theme",
        description = "Use custom Aurora color scheme instead of system dynamic colors",
        category = Appearance::class,
        type = Toggle::class
    )
    val useAuroraTheme: Boolean = true,

    @Setting(
        title = "Auto Backup",
        description = "Automatically backup app list when opening the app",
        category = Backup::class,
        type = Toggle::class
    )
    val autoBackup: Boolean = false,

    @Setting(
        title = "Auto Backup Format",
        description = "File format for automatic backups",
        category = Backup::class,
        type = Dropdown::class,
        options = ["Markdown", "Plain Text", "JSON", "HTML"]
    )
    val autoBackupFormat: Int = Constants.ExportFormat.JSON,

    @Setting(
        title = "Default Export Format",
        description = "Default format when exporting manually",
        category = General::class,
        type = Dropdown::class,
        options = ["Markdown", "Plain Text", "JSON", "HTML"]
    )
    val defaultExportFormat: Int = Constants.ExportFormat.JSON
)

object AppSettingsValidator {
    fun validate() {
        check(SortMode.entries.size == 9) {
            "SortMode.entries size changed (${SortMode.entries.size}); " +
                "AppSettings.defaultSort options must be updated + migration added"
        }
        check(FilterMode.entries.size == 3) {
            "FilterMode.entries size changed; AppSettings.defaultFilter options must match"
        }
        check(AppStore.entries.size == 5) {
            "AppStore.entries size changed; AppSettings.preferredStore options must match"
        }
        check(Constants.ExportFormat.MARKDOWN == 0 && Constants.ExportFormat.PLAIN_TEXT == 1 &&
            Constants.ExportFormat.JSON == 2 && Constants.ExportFormat.HTML == 3) {
            "ExportFormat indices changed; autoBackupFormat/defaultExportFormat options must match"
        }
        check(SortMode.entries.first() == SortMode.NAME_ASC &&
            SortMode.entries.last() == SortMode.PACKAGE_NAME) {
            "SortMode order changed; stored defaultSort indices would remap"
        }
        check(FilterMode.entries.first() == FilterMode.ALL &&
            FilterMode.entries.last() == FilterMode.SYSTEM) {
            "FilterMode order changed; stored defaultFilter indices would remap"
        }
        check(AppStore.entries.first() == AppStore.PLAY_STORE &&
            AppStore.entries.last() == AppStore.HUAWEI) {
            "AppStore order changed; stored preferredStore indices would remap"
        }
    }
}
