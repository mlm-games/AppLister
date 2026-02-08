package app.applister.data.repository

import io.github.mlmgames.settings.core.annotations.CategoryDefinition
import io.github.mlmgames.settings.core.annotations.Persisted
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

    @Persisted
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
    val autoBackupFormat: Int = 2,

    @Setting(
        title = "Default Export Format",
        description = "Default format when exporting manually",
        category = General::class,
        type = Dropdown::class,
        options = ["Markdown", "Plain Text", "JSON", "HTML"]
    )
    val defaultExportFormat: Int = 2
)
