package app.applister.ui.util

import android.app.Activity
import android.os.Build
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView

@Composable
fun SystemUIController(showStatusBar: Boolean) {
    val view = LocalView.current
    val context = LocalContext.current
    val window = (context as? Activity)?.window

    DisposableEffect(showStatusBar, window, view) {
        if (window == null) {
            onDispose { }
        } else {
            // Remember pre-existing state so dispose restores it.
            val prevBehavior = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.insetsController?.systemBarsBehavior
            } else {
                null
            }
            @Suppress("DEPRECATION")
            val prevVisibility = view.systemUiVisibility
            if (showStatusBar) {
                showStatusBar(window, view)
            } else {
                hideStatusBar(window, view)
            }
            onDispose {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        window.insetsController?.let {
                            it.show(WindowInsets.Type.statusBars())
                            if (prevBehavior != null) {
                                it.systemBarsBehavior = prevBehavior
                            }
                        }
                    } else {
                        @Suppress("DEPRECATION")
                        view.systemUiVisibility = prevVisibility
                    }
                } catch (_: Exception) { }
            }
        }
    }
}

private fun showStatusBar(window: android.view.Window, view: View) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.show(WindowInsets.Type.statusBars())
        } else {
            @Suppress("DEPRECATION")
            view.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        }
    } catch (_: Exception) { }
}

private fun hideStatusBar(window: android.view.Window, view: View) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let {
                it.hide(WindowInsets.Type.statusBars())
                it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            view.systemUiVisibility = View.SYSTEM_UI_FLAG_IMMERSIVE or
                    View.SYSTEM_UI_FLAG_FULLSCREEN
        }
    } catch (_: Exception) { }
}
