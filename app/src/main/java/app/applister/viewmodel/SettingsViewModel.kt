package app.applister.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.applister.AppGraph
import app.applister.data.repository.AppSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = AppGraph.settings

    val settings: StateFlow<AppSettings> = repo.flow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    fun updateSetting(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch {
            repo.update(transform)
        }
    }
}
