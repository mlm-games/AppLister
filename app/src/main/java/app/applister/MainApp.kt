package app.applister

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import app.applister.ui.components.PackageIconFetcher
import app.applister.ui.components.PackageIconKeyer

class MainApp : Application(), SingletonImageLoader.Factory {
    override fun onCreate() {
        super.onCreate()
        AppGraph.init(applicationContext)
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        return ImageLoader.Builder(context)
            .components {
                add(PackageIconFetcher.Factory())
                add(PackageIconKeyer())
            }
            .build()
    }
}
