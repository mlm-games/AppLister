package app.applister.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.applister.R
import app.applister.data.repository.AppSettings
import app.applister.ui.components.AppTopBar
import app.applister.viewmodel.SettingsViewModel
import io.github.mlmgames.settings.ui.dialogs.DropdownSettingDialog

@Composable
fun SettingsScreen(
    vm: SettingsViewModel,
    onBack: () -> Unit
) {
    val settings by vm.settings.collectAsState()
    val cfg = LocalConfiguration.current
    val gridCells = remember(cfg.screenWidthDp) { GridCells.Adaptive(minSize = 420.dp) }

    var showDefaultSortDialog by remember { mutableStateOf(false) }
    var showDefaultFilterDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showAutoBackupFormatDialog by remember { mutableStateOf(false) }
    var showExportFormatDialog by remember { mutableStateOf(false) }

    val sortOptions = listOf(
        stringResource(R.string.sort_name_asc), stringResource(R.string.sort_name_desc),
        stringResource(R.string.sort_install_date_newest), stringResource(R.string.sort_install_date_oldest),
        stringResource(R.string.sort_updated_newest), stringResource(R.string.sort_updated_oldest),
        stringResource(R.string.sort_size_largest), stringResource(R.string.sort_size_smallest),
        stringResource(R.string.sort_package_name)
    )
    val filterOptions = listOf(
        stringResource(R.string.filter_all), stringResource(R.string.filter_user),
        stringResource(R.string.filter_system)
    )
    val themeOptions = listOf(
        stringResource(R.string.theme_system), stringResource(R.string.theme_light),
        stringResource(R.string.theme_dark)
    )
    val formatOptions = listOf(
        stringResource(R.string.format_markdown), stringResource(R.string.format_plain_text),
        stringResource(R.string.format_json), stringResource(R.string.format_html)
    )

    SettingsScaffold(
        title = stringResource(R.string.settings),
        onBack = onBack
    ) {
        LazyVerticalGrid(
            columns = gridCells,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = stringResource(R.string.general),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }

            item {
                SettingsItem(
                    title = stringResource(R.string.default_sort),
                    subtitle = sortOptions[settings.defaultSort],
                    onClick = { showDefaultSortDialog = true }
                )
            }

            item {
                SettingsItem(
                    title = stringResource(R.string.default_filter),
                    subtitle = filterOptions[settings.defaultFilter],
                    onClick = { showDefaultFilterDialog = true }
                )
            }

            item {
                SettingsToggleItem(
                    title = stringResource(R.string.show_package_name),
                    subtitle = stringResource(R.string.show_package_name_desc),
                    checked = settings.showPackageName,
                    onCheckedChange = {
                        vm.updateSetting { it.copy(showPackageName = !it.showPackageName) }
                    }
                )
            }

            item {
                SettingsToggleItem(
                    title = stringResource(R.string.show_app_size),
                    subtitle = stringResource(R.string.show_app_size_desc),
                    checked = settings.showAppSize,
                    onCheckedChange = {
                        vm.updateSetting { it.copy(showAppSize = !it.showAppSize) }
                    }
                )
            }

            item {
                SettingsItem(
                    title = stringResource(R.string.default_export_format),
                    subtitle = formatOptions[settings.defaultExportFormat],
                    onClick = { showExportFormatDialog = true }
                )
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = stringResource(R.string.appearance),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                )
            }

            item {
                SettingsItem(
                    title = stringResource(R.string.theme),
                    subtitle = themeOptions[settings.themeMode],
                    onClick = { showThemeDialog = true }
                )
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = stringResource(R.string.backup),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                )
            }

            item {
                SettingsToggleItem(
                    title = stringResource(R.string.auto_backup_setting),
                    subtitle = stringResource(R.string.auto_backup_setting_desc),
                    checked = settings.autoBackup,
                    onCheckedChange = {
                        vm.updateSetting { it.copy(autoBackup = !it.autoBackup) }
                    }
                )
            }

            item {
                SettingsItem(
                    title = stringResource(R.string.auto_backup_format),
                    subtitle = formatOptions[settings.autoBackupFormat],
                    onClick = { showAutoBackupFormatDialog = true }
                )
            }
        }
    }

    if (showDefaultSortDialog) {
        DropdownSettingDialog(
            title = stringResource(R.string.default_sort),
            options = sortOptions,
            selectedIndex = settings.defaultSort,
            onDismiss = { showDefaultSortDialog = false },
            onOptionSelected = { i ->
                vm.updateSetting { it.copy(defaultSort = i) }
                showDefaultSortDialog = false
            }
        )
    }

    if (showDefaultFilterDialog) {
        DropdownSettingDialog(
            title = stringResource(R.string.default_filter),
            options = filterOptions,
            selectedIndex = settings.defaultFilter,
            onDismiss = { showDefaultFilterDialog = false },
            onOptionSelected = { i ->
                vm.updateSetting { it.copy(defaultFilter = i) }
                showDefaultFilterDialog = false
            }
        )
    }

    if (showThemeDialog) {
        DropdownSettingDialog(
            title = stringResource(R.string.theme),
            options = themeOptions,
            selectedIndex = settings.themeMode,
            onDismiss = { showThemeDialog = false },
            onOptionSelected = { i ->
                vm.updateSetting { it.copy(themeMode = i) }
                showThemeDialog = false
            }
        )
    }

    if (showExportFormatDialog) {
        DropdownSettingDialog(
            title = stringResource(R.string.default_export_format),
            options = formatOptions,
            selectedIndex = settings.defaultExportFormat,
            onDismiss = { showExportFormatDialog = false },
            onOptionSelected = { i ->
                vm.updateSetting { it.copy(defaultExportFormat = i) }
                showExportFormatDialog = false
            }
        )
    }

    if (showAutoBackupFormatDialog) {
        DropdownSettingDialog(
            title = stringResource(R.string.auto_backup_format),
            options = formatOptions,
            selectedIndex = settings.autoBackupFormat,
            onDismiss = { showAutoBackupFormatDialog = false },
            onOptionSelected = { i ->
                vm.updateSetting { it.copy(autoBackupFormat = i) }
                showAutoBackupFormatDialog = false
            }
        )
    }
}

@Composable
private fun SettingsItem(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    androidx.compose.material3.ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle, style = MaterialTheme.typography.bodySmall) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    )
}

@Composable
private fun SettingsToggleItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    androidx.compose.material3.ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle, style = MaterialTheme.typography.bodySmall) },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScaffold(
    title: String,
    onBack: () -> Unit,
    actions: @Composable (RowScope.() -> Unit) = {},
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp
            ) {
                AppTopBar(
                    title = {
                        Text(
                            title,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = stringResource(R.string.back)
                            )
                        }
                    },
                    actions = actions
                )
            }
        }
    ) { paddingValues ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            color = MaterialTheme.colorScheme.background
        ) {
            content(paddingValues)
        }
    }
}
