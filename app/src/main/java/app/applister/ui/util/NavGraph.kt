package app.applister.ui.util

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import app.applister.AppGraph
import app.applister.ui.screens.AppDetailScreen
import app.applister.ui.screens.AppListScreen
import app.applister.ui.screens.BackupListScreen
import app.applister.ui.screens.SettingsScreen
import app.applister.viewmodel.AppDetailViewModel
import app.applister.viewmodel.AppListViewModel
import app.applister.viewmodel.BackupListViewModel
import app.applister.viewmodel.SettingsViewModel
import kotlinx.serialization.Serializable

fun NavBackStack<NavKey>.popSafe(): Boolean {
    if (size <= 1) return false
    removeAt(lastIndex)
    return true
}

@Composable
fun NavGraph(
    backStack: NavBackStack<NavKey>,
    decorators: List<NavEntryDecorator<Any>>,
    appListVM: AppListViewModel,
    settingsVM: SettingsViewModel
) {
    NavDisplay(
        backStack = backStack,
        onBack = { backStack.popSafe() },
        entryDecorators = decorators,
        entryProvider = entryProvider {

            entry<Screen.Home> {
                AppListScreen(
                    vm = appListVM,
                    settingsVM = settingsVM,
                    onOpenSettings = { backStack.add(Screen.Settings) },
                    onOpenBackups = { backStack.add(Screen.Backups) },
                    onOpenApp = { pkg -> backStack.add(Screen.AppDetail(pkg)) }
                )
            }

            entry<Screen.AppDetail> { args ->
                val detailVM: AppDetailViewModel = viewModel(key = "detail_${args.packageName}") {
                    AppDetailViewModel()
                }
                AppDetailScreen(
                    packageName = args.packageName,
                    vm = detailVM,
                    settingsVM = settingsVM,
                    onBack = { backStack.popSafe() }
                )
            }

            entry<Screen.Backups> {
                val backupListVM: BackupListViewModel = viewModel {
                    BackupListViewModel(AppGraph.backupRepo)
                }
                BackupListScreen(
                    vm = backupListVM,
                    appListVM = appListVM,
                    settingsVM = settingsVM,
                    onBack = { backStack.popSafe() }
                )
            }

            entry<Screen.Settings> {
                SettingsScreen(
                    vm = settingsVM,
                    onBack = { backStack.popSafe() }
                )
            }
        }
    )
}

@Serializable
sealed interface Screen : NavKey {
    @Serializable
    data object Home : Screen

    @Serializable
    data class AppDetail(val packageName: String) : Screen

    @Serializable
    data object Backups : Screen

    @Serializable
    data object Settings : Screen
}
