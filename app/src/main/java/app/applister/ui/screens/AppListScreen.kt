package app.applister.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Deselect
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestorePage
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.applister.R
import app.applister.data.model.AppInfo
import app.applister.helper.ShareUtils
import app.applister.helper.ShareResult
import app.applister.helper.registerPackageChanges
import app.applister.ui.components.AboutDialog
import app.applister.ui.components.AppIcon
import app.applister.ui.components.AppTopBar
import app.applister.ui.components.ExportFormatDialog
import app.applister.ui.components.FilterChipRow
import app.applister.ui.components.RestoreSummaryDialog
import app.applister.ui.components.SearchBar
import app.applister.ui.components.SortDialog
import app.applister.viewmodel.AppListViewModel
import app.applister.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppListScreen(
    vm: AppListViewModel,
    settingsVM: SettingsViewModel,
    onOpenSettings: () -> Unit,
    onOpenBackups: () -> Unit,
    onOpenApp: (String) -> Unit
) {
    val apps by vm.apps.collectAsStateWithLifecycle()
    val isLoading by vm.isLoading.collectAsStateWithLifecycle()
    val loadError by vm.loadError.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val sortMode by vm.sortMode.collectAsStateWithLifecycle()
    val filterMode by vm.filterMode.collectAsStateWithLifecycle()
    val appCounts by vm.appCounts.collectAsStateWithLifecycle()
    val selectionMode by vm.selectionMode.collectAsStateWithLifecycle()
    val selectedPackages by vm.selectedPackages.collectAsStateWithLifecycle()
    val restoreResult by vm.restoreResult.collectAsStateWithLifecycle()
    val backupInProgress by vm.backupInProgress.collectAsStateWithLifecycle()
    val restoreInProgress by vm.restoreInProgress.collectAsStateWithLifecycle()
    val exportInProgress by vm.exportInProgress.collectAsStateWithLifecycle()
    val settings by settingsVM.settings.collectAsStateWithLifecycle()

    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()

    var showMenu by remember { mutableStateOf(false) }
    var showSortDialog by rememberSaveable { mutableStateOf(false) }
    var showExportDialog by rememberSaveable { mutableStateOf(false) }
    var showAboutDialog by rememberSaveable { mutableStateOf(false) }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                try {
                    val json = ctx.contentResolver.openInputStream(uri)
                        ?.bufferedReader()?.use { it.readText() }
                    if (!json.isNullOrBlank()) {
                        vm.restoreFromJson(json)
                    } else {
                        snackbarHostState.showSnackbar(ctx.getString(R.string.restore_failed, "empty file"))
                    }
                } catch (t: Throwable) {
                    t.printStackTrace()
                    try {
                        snackbarHostState.showSnackbar(ctx.getString(R.string.restore_failed, t.message ?: ""))
                    } catch (_: Exception) { }
                }
            }
        }
    }

    LaunchedEffect(vm) {
        vm.snackbarEvents.collect { event ->
            @Suppress("LocalContextGetResourceValueCall")
            val message = ctx.getString(event.messageResId, *event.args)
            try {
                snackbarHostState.showSnackbar(message)
            } catch (_: Exception) { }
        }
    }

    DisposableEffect(ctx) {
        val receiver = ctx.registerPackageChanges { vm.loadApps() }
        onDispose {
            try { ctx.unregisterReceiver(receiver) } catch (_: Exception) {}
        }
    }

    Scaffold(
        topBar = {
            if (selectionMode) {
                AppTopBar(
                    title = {
                        Text(stringResource(R.string.selected_count, selectedPackages.size))
                    },
                    navigationIcon = {
                        IconButton(onClick = { vm.exitSelectionMode() }) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.exit_selection))
                        }
                    },
                    actions = {
                        IconButton(onClick = { vm.selectAll() }) {
                            Icon(Icons.Default.SelectAll, contentDescription = stringResource(R.string.select_all))
                        }
                        IconButton(onClick = { vm.deselectAll() }) {
                            Icon(Icons.Default.Deselect, contentDescription = stringResource(R.string.deselect_all))
                        }
                        IconButton(onClick = { showExportDialog = true }) {
                            Icon(Icons.Default.Share, contentDescription = stringResource(R.string.export_selected))
                        }
                    }
                )
            } else {
                AppTopBar(
                    title = { Text(stringResource(R.string.app_name)) },
                    actions = {
                        Box {
                            IconButton(onClick = { showSortDialog = true }) {
                                Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = stringResource(R.string.sort))
                            }
                        }
                        Box {
                            IconButton(onClick = { showMenu = !showMenu }) {
                                Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.menu))
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.select_apps)) },
                                leadingIcon = { Icon(Icons.Default.Checklist, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    vm.enterSelectionMode()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.export_list)) },
                                leadingIcon = { Icon(Icons.Default.FileUpload, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    showExportDialog = true
                                }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.backup_now)) },
                                leadingIcon = { Icon(Icons.Default.Backup, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    vm.createBackup(settings.defaultExportFormat)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.restore_from_json)) },
                                leadingIcon = { Icon(Icons.Default.RestorePage, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    importLauncher.launch(arrayOf("application/json"))
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.backup_history)) },
                                leadingIcon = { Icon(Icons.Default.History, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    onOpenBackups()
                                }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.settings)) },
                                leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    onOpenSettings()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.about)) },
                                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    showAboutDialog = true
                                }
                            )
                            }
                        }
                    }
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (!selectionMode) {
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + slideInVertically { it },
                    exit = fadeOut() + slideOutVertically { it }
                ) {
                    FloatingActionButton(
                        onClick = { vm.loadApps() },
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.refresh),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    ) { pad ->
        Column(
            modifier = Modifier
                .padding(pad)
                .fillMaxSize()
        ) {
            SearchBar(
                query = query,
                onQueryChange = { vm.setQuery(it) }
            )

            FilterChipRow(
                selectedFilter = filterMode,
                counts = appCounts,
                onFilterSelected = { vm.setFilterMode(it) }
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.apps_count, apps.size, stringResource(sortMode.displayNameResId)),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (backupInProgress || restoreInProgress || exportInProgress) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .height(14.dp)
                                .width(14.dp),
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = when {
                                restoreInProgress -> stringResource(R.string.restoring)
                                exportInProgress -> stringResource(R.string.export_list)
                                else -> stringResource(R.string.backing_up)
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            stringResource(R.string.loading_apps),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else if (loadError != null && apps.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            stringResource(R.string.load_failed),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            loadError ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(onClick = { vm.retryLoad() }) {
                            Text(stringResource(R.string.retry))
                        }
                    }
                }
            } else if (apps.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            if (query.isBlank()) stringResource(R.string.no_apps_found)
                            else stringResource(R.string.no_results_for_query, query),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (query.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(onClick = { vm.setQuery("") }) {
                                Text(stringResource(R.string.clear_search))
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(apps, key = { it.packageName }) { app ->
                        AppListItem(
                            app = app,
                            selectionMode = selectionMode,
                            showPackageName = settings.showPackageName,
                            showAppSize = settings.showAppSize,
                            onClick = {
                                if (selectionMode) {
                                    vm.toggleSelection(app.packageName)
                                } else {
                                    onOpenApp(app.packageName)
                                }
                            },
                            onLongClick = {
                                if (!selectionMode) {
                                    vm.enterSelectionMode()
                                }
                                vm.toggleSelection(app.packageName)
                            },
                            modifier = Modifier.animateItem()
                        )
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }

    if (showSortDialog) {
        SortDialog(
            currentSort = sortMode,
            onSortSelected = { vm.setSortMode(it) },
            onDismiss = { showSortDialog = false }
        )
    }

    if (showExportDialog) {
        ExportFormatDialog(
            defaultFormat = settings.defaultExportFormat,
            onFormatSelected = { format ->
                scope.launch {
                    try {
                        val content = vm.getExportContent(format)
                        val ext = ShareUtils.getExtension(format)
                        val mime = ShareUtils.getMimeType(format)
                        when (val r = ShareUtils.shareTextFile(ctx, "applister.$ext", content, mime)) {
                            is ShareResult.Shared -> vm.exitSelectionMode()
                            is ShareResult.Failed -> snackbarHostState.showSnackbar(
                                ctx.getString(R.string.share_failed, r.reason)
                            )
                        }
                    } catch (e: IllegalStateException) {
                        try {
                            snackbarHostState.showSnackbar(ctx.getString(R.string.no_apps_to_export))
                        } catch (_: Exception) { }
                    } catch (t: Throwable) {
                        t.printStackTrace()
                        try {
                            snackbarHostState.showSnackbar(
                                ctx.getString(R.string.export_failed, t.message ?: "")
                            )
                        } catch (_: Exception) { }
                    }
                }
            },
            onDismiss = { showExportDialog = false }
        )
    }

    if (showAboutDialog) {
        AboutDialog(onDismiss = { showAboutDialog = false })
    }

    val currentResult = restoreResult
    if (currentResult != null) {
        RestoreSummaryDialog(
            result = currentResult,
            settingsVM = settingsVM,
            onDismiss = { vm.dismissRestoreResult() },
            onStoreError = { message ->
                scope.launch {
                    try {
                        snackbarHostState.showSnackbar(message)
                    } catch (_: Exception) { }
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppListItem(
    app: AppInfo,
    selectionMode: Boolean,
    showPackageName: Boolean,
    showAppSize: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ListItem(
        headlineContent = {
            Text(
                text = app.appName,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Medium
            )
        },
        supportingContent = {
            Column {
                if (showPackageName) {
                    Text(
                        text = app.packageName,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = app.versionName ?: "",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (showAppSize) {
                        Text(
                            text = app.apkSizeFormatted,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (app.isSystemApp) {
                        Text(
                            text = stringResource(R.string.system),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }
        },
        leadingContent = {
            if (selectionMode) {
                Checkbox(
                    checked = app.isSelected,
                    onCheckedChange = { onClick() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary
                    )
                )
            } else {
                AppIcon(packageName = app.packageName)
            }
        },
        colors = ListItemDefaults.colors(
            containerColor = if (app.isSelected && selectionMode)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.surface
        ),
        modifier = modifier
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 4.dp)
    )
}
