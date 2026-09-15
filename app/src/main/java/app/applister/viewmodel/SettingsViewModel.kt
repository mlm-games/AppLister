package app.applister.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.applister.AppGraph
import app.applister.data.repository.AppSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = AppGraph.settings

    val settings: StateFlow<AppSettings> = repo.flow
        .catch { e ->
            e.printStackTrace()
            emit(AppSettings())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    fun updateSetting(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch {
            try {
                repo.update(transform)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setField(name: String, value: Any) {
        viewModelScope.launch {
            try {
                repo.set(name, value)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
