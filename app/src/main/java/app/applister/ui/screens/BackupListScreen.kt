package app.applister.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.RestorePage
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.applister.R
import app.applister.data.db.BackupRecord
import app.applister.helper.ShareResult
import app.applister.helper.ShareUtils
import app.applister.ui.components.AppTopBar
import app.applister.viewmodel.AppListViewModel
import app.applister.viewmodel.BackupListEvent
import app.applister.viewmodel.BackupListViewModel
import app.applister.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BackupListScreen(
    vm: BackupListViewModel,
    appListVM: AppListViewModel,
    settingsVM: SettingsViewModel,
    onBack: () -> Unit
) {
    val backups by vm.backups.collectAsStateWithLifecycle()
    val restoring by vm.restoring.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingDelete by remember { mutableStateOf<BackupRecord?>(null) }

    fun show(message: String) {
        scope.launch {
            try {
                snackbarHostState.showSnackbar(message)
            } catch (_: Exception) { }
        }
    }

    LaunchedEffect(vm) {
        vm.events.collect { event ->
            when (event) {
                is BackupListEvent.DeleteFailed ->
                    show(ctx.getString(R.string.delete_failed, event.fileName))
                is BackupListEvent.StaleCleaned ->
                    show(ctx.getString(R.string.stale_cleaned, event.count))
                is BackupListEvent.ShareFailed ->
                    show(ctx.getString(R.string.share_failed, event.fileName))
                is BackupListEvent.ReadFailed ->
                    show(ctx.getString(R.string.backup_file_unreadable, event.reason))
            }
        }
    }

    LaunchedEffect(backups) {
        if (backups.isNotEmpty()) vm.cleanStaleRecords(backups)
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = { Text(stringResource(R.string.backup_history)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { pad ->
        if (backups.isEmpty()) {
            Box(
                modifier = Modifier
                    .padding(pad)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.height(48.dp)
                    )
                    Text(
                        stringResource(R.string.no_backups_yet),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        stringResource(R.string.create_backup_from_menu),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(pad)
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(backups, key = { it.id }) { record ->
                    val missing = vm.isFileMissing(record)
                    val restorable = vm.isRestorable(record)
                    BackupItem(
                        record = record,
                        fileMissing = missing,
                        restorable = restorable,
                        restoring = restoring,
                        onShare = {
                            if (missing) {
                                show(ctx.getString(R.string.missing_backup_file))
                            } else {
                                when (val r = ShareUtils.shareFile(ctx, vm.getBackupFile(record), vm.shareMimeType(record))) {
                                    is ShareResult.Shared -> Unit
                                    is ShareResult.Failed -> show(ctx.getString(R.string.share_failed, r.reason))
                                }
                            }
                        },
                        onRestore = {
                            if (missing) {
                                show(ctx.getString(R.string.missing_backup_file))
                            } else if (!restorable) {
                                show(ctx.getString(R.string.only_json_restorable))
                            } else {
                                scope.launch {
                                    vm.setRestoring(true)
                                    try {
                                        val content = vm.readBackupContent(record)
                                        appListVM.restoreFromJson(content)
                                        onBack()
                                    } catch (t: Throwable) {
                                        t.printStackTrace()
                                        show(ctx.getString(R.string.backup_file_unreadable, t.message ?: record.fileName))
                                    } finally {
                                        vm.setRestoring(false)
                                    }
                                }
                            }
                        },
                        onDelete = { pendingDelete = record }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }

    val toDelete = pendingDelete
    if (toDelete != null) {
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.confirm_delete_title)) },
            text = { Text(stringResource(R.string.confirm_delete_message, toDelete.fileName)) },
            confirmButton = {
                TextButton(onClick = {
                    pendingDelete = null
                    vm.deleteBackup(toDelete)
                }) {
                    Text(
                        stringResource(R.string.delete),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (restoring) {
        androidx.compose.ui.window.Dialog(onDismissRequest = {}) {
            Card {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator()
                    Text(stringResource(R.string.restoring))
                }
            }
        }
    }

}

@Composable
private fun BackupItem(
    record: BackupRecord,
    fileMissing: Boolean,
    restorable: Boolean,
    restoring: Boolean,
    onShare: () -> Unit,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = record.fileName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = formatBackupDate(record.createdAt),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(R.string.apps_format, record.appCount),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = record.format,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (record.isAutoBackup) {
                            Text(
                                text = stringResource(R.string.auto),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                    if (fileMissing) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.missing_backup_file),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onShare, enabled = !restoring) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = stringResource(R.string.share_backup),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                if (restorable) {
                    IconButton(onClick = onRestore, enabled = !restoring && !fileMissing) {
                        Icon(
                            Icons.Default.RestorePage,
                            contentDescription = stringResource(R.string.restore),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
                IconButton(onClick = onDelete, enabled = !restoring) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(R.string.delete_backup),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

private fun formatBackupDate(millis: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(millis))
}
