package app.applister.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.Launch
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.applister.AppGraph
import app.applister.R
import app.applister.data.model.AppInfo
import app.applister.data.model.AppStore
import app.applister.data.model.StoreOpenResult
import app.applister.helper.registerPackageChanges
import app.applister.ui.components.AppIcon
import app.applister.ui.components.AppTopBar
import app.applister.viewmodel.AppDetailViewModel
import app.applister.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AppDetailScreen(
    packageName: String,
    vm: AppDetailViewModel,
    settingsVM: SettingsViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val settings by settingsVM.settings.collectAsStateWithLifecycle()

    fun show(message: String) {
        scope.launch {
            try {
                snackbarHostState.showSnackbar(message)
            } catch (_: Exception) { }
        }
    }

    var refreshTick by remember(packageName) { mutableStateOf(0) }
    androidx.compose.runtime.DisposableEffect(context, packageName) {
        val receiver = context.registerPackageChanges { refreshTick++ }
        onDispose {
            try { context.unregisterReceiver(receiver) } catch (_: Exception) {}
        }
    }
    val appInfo: AppInfo? = remember(packageName, refreshTick) {
        try {
            AppGraph.appListRepo.getAppInfo(packageName)
        } catch (_: Exception) {
            null
        }
    }

    LaunchedEffect(appInfo) {
        if (refreshTick > 0 && appInfo == null) onBack()
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = { Text(stringResource(R.string.app_details)) },
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
        if (appInfo == null) {
            Column(
                modifier = Modifier
                    .padding(pad)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    stringResource(R.string.app_not_found),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(pad)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        AppIcon(packageName = appInfo.packageName, size = 64.dp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = appInfo.appName,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            if (appInfo.isSystemApp) {
                                Text(
                                    text = stringResource(R.string.system_app),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                        }
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        DetailRow(stringResource(R.string.package_name), appInfo.packageName, mono = true)
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 6.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        )
                        DetailRow(stringResource(R.string.version), stringResource(R.string.version_format, appInfo.versionName ?: "?", appInfo.versionCode))
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 6.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        )
                        DetailRow(stringResource(R.string.installed), formatTimestamp(appInfo.installTimeMillis))
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 6.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        )
                        DetailRow(stringResource(R.string.last_updated), formatTimestamp(appInfo.updateTimeMillis))
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 6.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        )
                        DetailRow(stringResource(R.string.apk_size), appInfo.apkSizeFormatted)
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.actions),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilledTonalButton(
                                onClick = {
                                    if (!vm.launchApp(context, appInfo.packageName)) {
                                        show(context.getString(R.string.open_store_failed, appInfo.appName))
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Launch,
                                    contentDescription = null,
                                    modifier = Modifier.padding(end = 6.dp)
                                )
                                Text(stringResource(R.string.open))
                            }
                            FilledTonalButton(
                                onClick = {
                                    val store = AppStore.fromIndex(settings.preferredStore)
                                    when (val result = vm.openAppStore(context, appInfo.packageName, store)) {
                                        is StoreOpenResult.Success -> { /* No action needed */ }
                                        is StoreOpenResult.NoAppFound -> {
                                            scope.launch {
                                                try {
                                                    val response = snackbarHostState.showSnackbar(
                                                        message = store.getMissingStoreMessage(),
                                                        actionLabel = context.getString(R.string.help)
                                                    )
                                                    if (response == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                                                        snackbarHostState.showSnackbar(store.getGuidanceMessage())
                                                    }
                                                } catch (_: Exception) { }
                                            }
                                        }
                                        is StoreOpenResult.Error -> {
                                            show(context.getString(R.string.open_store_failed, result.message))
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    Icons.Default.ShoppingCart,
                                    contentDescription = null,
                                    modifier = Modifier.padding(end = 6.dp)
                                )
                                Text(stringResource(R.string.store))
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val store = AppStore.fromIndex(settings.preferredStore)
                                    when (val r = vm.shareApp(context, appInfo.packageName, appInfo.appName, store)) {
                                        is AppDetailViewModel.DetailActionResult.Done -> Unit
                                        is AppDetailViewModel.DetailActionResult.Failed ->
                                            show(context.getString(R.string.share_failed, r.reason))
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    Icons.Default.Share,
                                    contentDescription = null,
                                    modifier = Modifier.padding(end = 6.dp)
                                )
                                Text(stringResource(R.string.share))
                            }
                            OutlinedButton(
                                onClick = {
                                    when (val r = vm.openAppInfo(context, appInfo.packageName)) {
                                        is AppDetailViewModel.DetailActionResult.Done -> Unit
                                        is AppDetailViewModel.DetailActionResult.Failed ->
                                            show(context.getString(R.string.open_store_failed, r.reason))
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = null,
                                    modifier = Modifier.padding(end = 6.dp)
                                )
                                Text(stringResource(R.string.info))
                            }
                        }

                        if (!appInfo.isSystemApp) {
                            OutlinedButton(
                                onClick = {
                                    when (val r = vm.uninstallApp(context, appInfo.packageName)) {
                                        is AppDetailViewModel.DetailActionResult.Done -> Unit
                                        is AppDetailViewModel.DetailActionResult.Failed ->
                                            show(context.getString(R.string.open_store_failed, r.reason))
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    modifier = Modifier.padding(end = 6.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Text(stringResource(R.string.uninstall), color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String, mono: Boolean = false) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = if (mono) FontFamily.Monospace else FontFamily.Default,
            fontWeight = FontWeight.Medium
        )
    }
}

private val timestampFormat: ThreadLocal<SimpleDateFormat> = ThreadLocal.withInitial {
    SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
}

private fun formatTimestamp(millis: Long): String {
    return try {
        timestampFormat.get()?.format(Date(millis)) ?: Date(millis).toString()
    } catch (_: Exception) {
        Date(millis).toString()
    }
}
