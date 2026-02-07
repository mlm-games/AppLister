package app.applister.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.applister.data.db.BackupRecord
import app.applister.data.repository.BackupRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class BackupListViewModel(
    private val backupRepo: BackupRepository
) : ViewModel() {

    val backups: StateFlow<List<BackupRecord>> = backupRepo.allBackups()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteBackup(record: BackupRecord) {
        viewModelScope.launch {
            backupRepo.deleteBackup(record)
        }
    }

    fun getBackupFile(record: BackupRecord): File = backupRepo.getBackupFile(record)

    fun readBackupContent(record: BackupRecord): String? {
        return try {
            val file = getBackupFile(record)
            if (file.exists()) file.readText() else null
        } catch (_: Exception) {
            null
        }
    }
}
