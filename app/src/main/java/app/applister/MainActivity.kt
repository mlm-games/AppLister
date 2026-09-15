package app.applister

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import app.applister.data.repository.AppSettingsValidator
import app.applister.ui.theme.MainTheme
import app.applister.ui.util.NavGraph
import app.applister.ui.util.Screen
import app.applister.viewmodel.AppListViewModel
import app.applister.viewmodel.SettingsViewModel

class MainActivity : ComponentActivity() {
    private inline fun <reified T : ViewModel> vm(crossinline create: () -> T) =
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <VM : ViewModel> create(modelClass: Class<VM>): VM = create() as VM
        }

    private val settingsVM: SettingsViewModel by viewModels {
        vm { SettingsViewModel(application) }
    }

    private val appListVM: AppListViewModel by viewModels {
        vm { AppListViewModel(AppGraph.appListRepo, AppGraph.backupRepo, AppGraph.settings) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        AppGraph.init(applicationContext)
        AppSettingsValidator.validate()

        setContent {
            val settings by settingsVM.settings.collectAsStateWithLifecycle()
            val dark = when (settings.themeMode) {
                0 -> isSystemInDarkTheme()
                1 -> false
                2 -> true
                else -> isSystemInDarkTheme()
            }

            MainTheme(
                darkTheme = dark,
                useAuroraTheme = settings.useAuroraTheme,
                dynamicColor = !settings.useAuroraTheme
            ) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val backStack = rememberNavBackStack(Screen.Home)

                    NavGraph(
                        backStack = backStack,
                        decorators = listOf(
                            rememberSaveableStateHolderNavEntryDecorator(),
                            rememberViewModelStoreNavEntryDecorator()
                        ),
                        appListVM = appListVM,
                        settingsVM = settingsVM
                    )
                }
            }
        }
    }
}
