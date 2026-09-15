package app.applister.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.GetApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.applister.R
import app.applister.data.model.AppStore
import app.applister.data.model.RestoreResult
import app.applister.data.model.RestoredApp
import app.applister.data.model.StoreOpenResult
import app.applister.data.model.VersionStatus
import app.applister.viewmodel.SettingsViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun RestoreSummaryDialog(
    result: RestoreResult,
    settingsVM: SettingsViewModel,
    onDismiss: () -> Unit,
    onStoreError: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val settings by settingsVM.settings.collectAsStateWithLifecycle()
    val preferredStore = remember(settings.preferredStore) { AppStore.fromIndex(settings.preferredStore) }
    val openStoreFailedFormat = stringResource(R.string.open_store_failed)

    fun openStore(packageName: String) {
        when (val openResult = preferredStore.openApp(context, packageName)) {
            is StoreOpenResult.Success -> {  }
            is StoreOpenResult.NoAppFound -> {
                onStoreError(preferredStore.getMissingStoreMessage())
            }
            is StoreOpenResult.Error -> {
                onStoreError(openStoreFailedFormat.format(openResult.message))
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.restore_summary))
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                item {
                    Text(
                        text = stringResource(R.string.total_apps_backup, result.totalApps),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = stringResource(R.string.installed_count, result.foundApps.size),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = stringResource(R.string.not_installed_count, result.missingApps.size),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    if (result.skippedInvalidEntries > 0) {
                        Text(
                            text = stringResource(R.string.skipped_invalid_entries, result.skippedInvalidEntries),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                if (result.missingApps.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.missing_apps),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    items(result.missingApps, key = { it.packageName }) { app ->
                        MissingAppItem(
                            app = app,
                            onOpenStore = { openStore(it) }
                        )
                    }
                }

                if (result.foundApps.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.installed_apps),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    items(result.foundApps, key = { it.packageName }) { app ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Column {
                                Text(
                                    text = app.appName,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = app.packageName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                VersionNote(app)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (result.missingApps.isNotEmpty()) {
                TextButton(onClick = {
                    result.missingApps.firstOrNull()?.let { openStore(it.packageName) }
                }) {
                    Text(stringResource(R.string.install_missing_apps))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        }
    )
}

@Composable
private fun VersionNote(app: RestoredApp) {
    val note = when (app.versionStatus) {
        VersionStatus.OUTDATED -> stringResource(R.string.version_outdated)
        VersionStatus.NEWER_THAN_BACKUP -> stringResource(R.string.version_newer)
        VersionStatus.VERSION_DIFFERS -> app.versionInBackup?.let {
            stringResource(R.string.was_version, it)
        }
        else -> app.versionInBackup?.let { stringResource(R.string.was_version, it) }
    }
    if (note != null) {
        Text(
            text = note,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MissingAppItem(
    app: RestoredApp,
    onOpenStore: (String) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.appName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (app.versionInBackup != null) {
                    Text(
                        text = stringResource(R.string.was_version, app.versionInBackup),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = { onOpenStore(app.packageName) }) {
                Icon(
                    Icons.Default.GetApp,
                    contentDescription = stringResource(R.string.install_from_play_store),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
