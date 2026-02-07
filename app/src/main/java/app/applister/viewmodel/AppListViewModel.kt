package app.applister.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.applister.R
import app.applister.data.model.AppInfo
import app.applister.data.model.FilterMode
import app.applister.data.model.RestoreResult
import app.applister.data.model.SortMode
import app.applister.data.repository.AppListRepository
import app.applister.data.repository.AppSettings
import app.applister.data.repository.BackupRepository
import io.github.mlmgames.settings.core.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SnackbarState(
    val messageResId: Int,
    val args: Array<String> = emptyArray()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as SnackbarState
        if (messageResId != other.messageResId) return false
        if (!args.contentEquals(other.args)) return false
        return true
    }
    override fun hashCode(): Int {
        var result = messageResId
        result = 31 * result + args.contentHashCode()
        return result
    }
}

class AppListViewModel(
    private val appListRepo: AppListRepository,
    private val backupRepo: BackupRepository,
    private val settingsRepo: SettingsRepository<AppSettings>
) : ViewModel() {

    private val _allApps = MutableStateFlow<List<AppInfo>>(emptyList())
    private val _query = MutableStateFlow("")
    private val _sortMode = MutableStateFlow(SortMode.NAME_ASC)
    private val _filterMode = MutableStateFlow(FilterMode.ALL)
    private val _isLoading = MutableStateFlow(true)
    private val _selectionMode = MutableStateFlow(false)
    private val _selectedPackages = MutableStateFlow<Set<String>>(emptySet())
    private val _snackbarState = MutableStateFlow<SnackbarState?>(null)
    private val _restoreResult = MutableStateFlow<RestoreResult?>(null)
    private val _backupInProgress = MutableStateFlow(false)

    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    val query: StateFlow<String> = _query.asStateFlow()
    val sortMode: StateFlow<SortMode> = _sortMode.asStateFlow()
    val filterMode: StateFlow<FilterMode> = _filterMode.asStateFlow()
    val selectionMode: StateFlow<Boolean> = _selectionMode.asStateFlow()
    val selectedPackages: StateFlow<Set<String>> = _selectedPackages.asStateFlow()
    val snackbarState: StateFlow<SnackbarState?> = _snackbarState.asStateFlow()
    val restoreResult: StateFlow<RestoreResult?> = _restoreResult.asStateFlow()
    val backupInProgress: StateFlow<Boolean> = _backupInProgress.asStateFlow()

    val apps: StateFlow<List<AppInfo>> = combine(
        _allApps, _query, _sortMode, _filterMode, _selectedPackages
    ) { all, q, sort, filter, selected ->
        var result = appListRepo.filterApps(all, filter)
        result = appListRepo.searchApps(result, q)
        result = appListRepo.sortApps(result, sort)
        result.map { app -> app.copy(isSelected = selected.contains(app.packageName)) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val appCounts: StateFlow<Triple<Int, Int, Int>> = _allApps.combine(_filterMode) { all, _ ->
        Triple(
            all.size,
            all.count { !it.isSystemApp },
            all.count { it.isSystemApp }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Triple(0, 0, 0))

    init {
        viewModelScope.launch {
            val settings = settingsRepo.flow.first()
            _sortMode.value = SortMode.fromIndex(settings.defaultSort)
            _filterMode.value = FilterMode.entries.getOrElse(settings.defaultFilter) { FilterMode.ALL }
            loadApps()

            if (settings.autoBackup) {
                performAutoBackup(settings.autoBackupFormat)
            }
        }
    }

    fun loadApps() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _allApps.value = appListRepo.getInstalledApps()
            } catch (e: Exception) {
                e.printStackTrace()
                _snackbarState.value = SnackbarState(R.string.failed_load_apps, arrayOf(e.message ?: ""))
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun setQuery(q: String) {
        _query.value = q
    }

    fun setSortMode(mode: SortMode) {
        _sortMode.value = mode
    }

    fun setFilterMode(mode: FilterMode) {
        _filterMode.value = mode
    }

    fun enterSelectionMode() {
        _selectionMode.value = true
    }

    fun exitSelectionMode() {
        _selectionMode.value = false
        _selectedPackages.value = emptySet()
    }

    fun toggleSelection(packageName: String) {
        val current = _selectedPackages.value.toMutableSet()
        if (current.contains(packageName)) {
            current.remove(packageName)
        } else {
            current.add(packageName)
        }
        _selectedPackages.value = current
        if (current.isEmpty()) {
            _selectionMode.value = false
        }
    }

    fun selectAll() {
        _selectedPackages.value = apps.value.map { it.packageName }.toSet()
    }

    fun deselectAll() {
        _selectedPackages.value = emptySet()
    }

    fun getSelectedApps(): List<AppInfo> {
        val selected = _selectedPackages.value
        return apps.value.filter { selected.contains(it.packageName) }
    }

    fun createBackup(format: Int) {
        viewModelScope.launch {
            _backupInProgress.value = true
            try {
                val appsToBackup = if (_selectionMode.value && _selectedPackages.value.isNotEmpty()) {
                    getSelectedApps()
                } else {
                    apps.value
                }
                val record = backupRepo.createBackup(appsToBackup, format, isAuto = false)
                if (record != null) {
                    _snackbarState.value = SnackbarState(R.string.backup_created, arrayOf(record.fileName))
                    exitSelectionMode()
                } else {
                    _snackbarState.value = SnackbarState(R.string.backup_failed)
                }
            } catch (e: Exception) {
                _snackbarState.value = SnackbarState(R.string.backup_failed_msg, arrayOf(e.message ?: ""))
            } finally {
                _backupInProgress.value = false
            }
        }
    }

    private fun performAutoBackup(format: Int) {
        viewModelScope.launch {
            try {
                val allApps = _allApps.value
                if (allApps.isNotEmpty()) {
                    backupRepo.createBackup(allApps, format, isAuto = true)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun restoreFromJson(jsonContent: String) {
        viewModelScope.launch {
            try {
                val result = backupRepo.restoreFromJson(jsonContent)
                _restoreResult.value = result
            } catch (e: Exception) {
                _snackbarState.value = SnackbarState(R.string.restore_failed, arrayOf(e.message ?: ""))
            }
        }
    }

    fun dismissRestoreResult() {
        _restoreResult.value = null
    }

    fun dismissSnackbar() {
        _snackbarState.value = null
    }

    fun getExportContent(format: Int): String {
        val appsToExport = if (_selectionMode.value && _selectedPackages.value.isNotEmpty()) {
            getSelectedApps()
        } else {
            apps.value
        }
        return backupRepo.formatApps(appsToExport, format)
    }
}
