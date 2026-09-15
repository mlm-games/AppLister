package app.applister.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.applister.R
import app.applister.data.Constants
import app.applister.data.model.AppInfo
import app.applister.data.model.FilterMode
import app.applister.data.model.RestoreResult
import app.applister.data.model.SortMode
import app.applister.data.repository.AppListRepository
import app.applister.data.repository.AppSettings
import app.applister.data.repository.BackupRepository
import io.github.mlmgames.settings.core.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

data class SnackbarMessage(
    val id: Long,
    val messageResId: Int,
    val args: Array<String> = emptyArray()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as SnackbarMessage
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}

typealias SnackbarState = SnackbarMessage

class AppListViewModel(
    private val appListRepo: AppListRepository,
    private val backupRepo: BackupRepository,
    private val settingsRepo: SettingsRepository<AppSettings>
) : ViewModel() {

    private var snackbarSeq = 0L
    private val _allApps = MutableStateFlow<List<AppInfo>>(emptyList())
    private val _query = MutableStateFlow("")
    private val _debouncedQuery = MutableStateFlow("")
    private val _sortMode = MutableStateFlow(SortMode.NAME_ASC)
    private val _filterMode = MutableStateFlow(FilterMode.ALL)
    private val _isLoading = MutableStateFlow(true)
    private val _loadError = MutableStateFlow<String?>(null)
    private val _selectionMode = MutableStateFlow(false)
    private val _selectedPackages = MutableStateFlow<Set<String>>(emptySet())
    private val _restoreResult = MutableStateFlow<RestoreResult?>(null)
    private val _backupInProgress = MutableStateFlow(false)
    private val _restoreInProgress = MutableStateFlow(false)
    private val _exportInProgress = MutableStateFlow(false)

    private val _snackbarEvents = MutableSharedFlow<SnackbarMessage>(extraBufferCapacity = 16)
    val snackbarEvents: SharedFlow<SnackbarMessage> = _snackbarEvents.asSharedFlow()

    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    val loadError: StateFlow<String?> = _loadError.asStateFlow()
    val query: StateFlow<String> = _query.asStateFlow()
    val sortMode: StateFlow<SortMode> = _sortMode.asStateFlow()
    val filterMode: StateFlow<FilterMode> = _filterMode.asStateFlow()
    val selectionMode: StateFlow<Boolean> = _selectionMode.asStateFlow()
    val selectedPackages: StateFlow<Set<String>> = _selectedPackages.asStateFlow()
    val restoreResult: StateFlow<RestoreResult?> = _restoreResult.asStateFlow()
    val backupInProgress: StateFlow<Boolean> = _backupInProgress.asStateFlow()
    val restoreInProgress: StateFlow<Boolean> = _restoreInProgress.asStateFlow()
    val exportInProgress: StateFlow<Boolean> = _exportInProgress.asStateFlow()

    val snackbarState: StateFlow<SnackbarState?> = _snackbarEvents
        .map { it as SnackbarState? }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

        val apps: StateFlow<List<AppInfo>> = combine(
        _allApps, _debouncedQuery, _sortMode, _filterMode, _selectedPackages
    ) { all, q, sort, filter, selected ->
        var result = appListRepo.filterApps(all, filter)
        result = appListRepo.searchApps(result, q)
        result = appListRepo.sortApps(result, sort)
        if (selected.isEmpty()) {
            result
        } else {
            result.map { app -> app.copy(isSelected = selected.contains(app.packageName)) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val appCounts: StateFlow<Triple<Int, Int, Int>> = _allApps
        .map { all ->
            Triple(
                all.size,
                all.count { !it.isSystemApp },
                all.count { it.isSystemApp }
            )
        }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Triple(0, 0, 0))

    private var loadJob: Job? = null

    private val backupMutex = Mutex()

    private var autoBackupDoneForLaunch = false

    init {
        @OptIn(kotlinx.coroutines.FlowPreview::class)
        viewModelScope.launch {
            _query.debounce(250).distinctUntilChanged().collect { _debouncedQuery.value = it }
        }
        viewModelScope.launch {
            try {
                val settings = settingsRepo.flow.first()
                _sortMode.value = SortMode.fromIndex(settings.defaultSort)
                _filterMode.value = FilterMode.fromIndex(settings.defaultFilter)
            } catch (e: Exception) {
                e.printStackTrace()
                emitSnackbarSync(R.string.failed_load_apps, e.message ?: "")
            }
            loadAppsInternal()
            try {
                val settings = settingsRepo.flow.first()
                if (settings.autoBackup && !autoBackupDoneForLaunch) {
                    autoBackupDoneForLaunch = true
                    performAutoBackup(settings.autoBackupFormat)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun loadApps() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch { loadAppsInternal() }
    }

    private suspend fun loadAppsInternal() {
        _isLoading.value = true
        _loadError.value = null
        try {
            _allApps.value = appListRepo.getInstalledApps()
            val alive = _allApps.value.asSequence().map { it.packageName }.toSet()
            _selectedPackages.update { sel -> sel.intersect(alive) }
            if (_selectedPackages.value.isEmpty()) _selectionMode.value = false
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            e.printStackTrace()
            _loadError.value = e.message ?: ""
            emitSnackbarSync(R.string.failed_load_apps, e.message ?: "")
        } finally {
            _isLoading.value = false
        }
    }

    fun retryLoad() = loadApps()

    fun setQuery(q: String) {
        _query.value = q
    }

    fun setSortMode(mode: SortMode) {
        _sortMode.value = mode
        // Persist as default so Settings and next launch agree (fire-and-forget ok).
        viewModelScope.launch {
            try {
                settingsRepo.update { it.copy(defaultSort = mode.ordinal) }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setFilterMode(mode: FilterMode) {
        _filterMode.value = mode
        viewModelScope.launch {
            try {
                settingsRepo.update { it.copy(defaultFilter = mode.ordinal) }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun enterSelectionMode() {
        _selectionMode.value = true
    }

    fun exitSelectionMode() {
        _selectionMode.value = false
        _selectedPackages.value = emptySet()
    }

    fun toggleSelection(packageName: String) {
        _selectedPackages.update { current ->
            if (current.contains(packageName)) current - packageName else current + packageName
        }
        if (_selectedPackages.value.isEmpty()) {
            _selectionMode.value = false
        } else {
            _selectionMode.value = true
        }
    }

        fun selectAll() {
        _selectedPackages.value = apps.value.map { it.packageName }.toSet()
        _selectionMode.value = _selectedPackages.value.isNotEmpty()
    }

    fun deselectAll() {
        _selectedPackages.value = emptySet()
        _selectionMode.value = false
    }

    fun getSelectedApps(): List<AppInfo> {
        val selected = _selectedPackages.value
        if (selected.isEmpty()) return emptyList()
        return _allApps.value.filter { selected.contains(it.packageName) }
    }

        private fun resolveAppsForAction(): List<AppInfo> {
        return if (_selectionMode.value && _selectedPackages.value.isNotEmpty()) {
            getSelectedApps()
        } else {
            _allApps.value
        }
    }

    fun createBackup(format: Int) {
        viewModelScope.launch {
            backupMutex.withLock {
                if (_backupInProgress.value) return@withLock
                _backupInProgress.value = true
                try {
                    val appsToBackup = resolveAppsForAction()
                    if (appsToBackup.isEmpty()) {
                        emitSnackbarSync(R.string.backup_failed)
                        return@withLock
                    }
                    val safeFormat = Constants.ExportFormat.coerce(format)
                    val record = backupRepo.createBackup(appsToBackup, safeFormat, isAuto = false)
                    if (record != null) {
                        emitSnackbarSync(R.string.backup_created, record.fileName)
                        exitSelectionMode()
                    } else {
                        emitSnackbarSync(R.string.backup_failed)
                    }
                } catch (e: Exception) {
                    emitSnackbarSync(R.string.backup_failed_msg, e.message ?: "")
                } finally {
                    _backupInProgress.value = false
                }
            }
        }
    }

    private suspend fun performAutoBackup(format: Int) {
        backupMutex.withLock {
            if (_backupInProgress.value) return
            _backupInProgress.value = true
            try {
                val allApps = _allApps.value
                if (allApps.isNotEmpty()) {
                    backupRepo.createBackup(
                        allApps,
                        Constants.ExportFormat.coerce(format),
                        isAuto = true
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _backupInProgress.value = false
            }
        }
    }

    fun restoreFromJson(jsonContent: String) {
        if (_restoreInProgress.value) return
        viewModelScope.launch {
            _restoreInProgress.value = true
            try {
                val result = backupRepo.restoreFromJson(jsonContent)
                _restoreResult.value = result
                if (result.isDecodeError) {
                    emitSnackbarSync(R.string.restore_failed, "invalid backup file")
                }
            } catch (e: Exception) {
                emitSnackbarSync(R.string.restore_failed, e.message ?: "")
            } finally {
                _restoreInProgress.value = false
            }
        }
    }

    fun dismissRestoreResult() {
        _restoreResult.value = null
    }

    fun dismissSnackbar() {
    }

        suspend fun getExportContent(format: Int): String = withContext(Dispatchers.Default) {
        _exportInProgress.value = true
        try {
            val appsToExport = resolveAppsForAction()
            if (appsToExport.isEmpty()) {
                emitSnackbar(R.string.backup_failed)
                throw IllegalStateException("Nothing to export")
            }
            backupRepo.formatApps(appsToExport, Constants.ExportFormat.coerce(format))
        } finally {
            _exportInProgress.value = false
        }
    }

    private suspend fun emitSnackbar(messageResId: Int, vararg args: String) {
        _snackbarEvents.emit(SnackbarMessage(++snackbarSeq, messageResId, args.map { it }.toTypedArray()))
    }

    private fun emitSnackbarSync(messageResId: Int, vararg args: String) {
        _snackbarEvents.tryEmit(SnackbarMessage(++snackbarSeq, messageResId, args.map { it }.toTypedArray()))
    }

    private fun emitSnackbar(messageResId: Int) {
        emitSnackbarSync(messageResId)
    }
}
