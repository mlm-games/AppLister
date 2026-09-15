package app.applister.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.applister.data.db.BackupRecord
import app.applister.data.repository.BackupRepository
import app.applister.data.repository.DeleteBackupResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

sealed interface BackupListEvent {
    data class DeleteFailed(val fileName: String) : BackupListEvent
    data class StaleCleaned(val count: Int) : BackupListEvent
    data class ShareFailed(val fileName: String) : BackupListEvent
    data class ReadFailed(val fileName: String, val reason: String) : BackupListEvent
}

class BackupListViewModel(
    private val backupRepo: BackupRepository
) : ViewModel() {

    private var snackbarSeq = 0L

    val backups: StateFlow<List<BackupRecord>> = backupRepo.allBackups()
        .catch { e ->
            e.printStackTrace()
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _events = MutableSharedFlow<BackupListEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<BackupListEvent> = _events.asSharedFlow()

    private val _restoring = MutableStateFlow(false)
    val restoring: StateFlow<Boolean> = _restoring.asStateFlow()

    private var cleanJob: Job? = null
    fun deleteBackup(record: BackupRecord) {
        viewModelScope.launch {
            when (backupRepo.deleteBackup(record)) {
                DeleteBackupResult.Deleted -> Unit // list updates via Room flow
                DeleteBackupResult.FileDeleteFailed ->
                    _events.emit(BackupListEvent.DeleteFailed(record.fileName))
                DeleteBackupResult.DatabaseError ->
                    _events.emit(BackupListEvent.DeleteFailed(record.fileName))
            }
        }
    }

    fun getBackupFile(record: BackupRecord): File = backupRepo.getBackupFile(record)

    fun isFileMissing(record: BackupRecord): Boolean = backupRepo.isFileMissing(record)

    fun isRestorable(record: BackupRecord): Boolean =
        record.format.equals("JSON", ignoreCase = true)

        suspend fun readBackupContent(record: BackupRecord): String =
        backupRepo.readBackupContent(record)

    fun shareMimeType(record: BackupRecord): String =
        if (isRestorable(record)) "application/json"
        else when {
            record.fileName.endsWith(".md", ignoreCase = true) -> "text/markdown"
            record.fileName.endsWith(".html", ignoreCase = true) -> "text/html"
            record.fileName.endsWith(".txt", ignoreCase = true) -> "text/plain"
            else -> "application/octet-stream"
        }

    fun cleanStaleRecords(records: List<BackupRecord>) {
        if (cleanJob?.isActive == true) return
        cleanJob = viewModelScope.launch {
            try {
                val removed = backupRepo.deleteStaleRecords(records)
                if (removed > 0) _events.emit(BackupListEvent.StaleCleaned(removed))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setRestoring(restoring: Boolean) {
        _restoring.value = restoring
    }
}
