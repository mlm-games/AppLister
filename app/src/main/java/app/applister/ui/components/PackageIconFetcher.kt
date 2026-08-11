package app.applister.ui.components

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import coil3.ImageLoader
import coil3.asImage
import coil3.decode.DataSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.ImageFetchResult
import coil3.key.Keyer
import coil3.request.Options

class PackageIconFetcher(
    private val data: PackageIcon,
    private val context: Context
) : Fetcher {
    override suspend fun fetch(): FetchResult? {
        val pm = context.packageManager
        val drawable = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getApplicationIcon(data.packageName)
            } else {
                @Suppress("DEPRECATION")
                pm.getApplicationIcon(data.packageName)
            }
        } catch (_: PackageManager.NameNotFoundException) {
            return null
        }

        val size = (context.resources.displayMetrics.density * 88).toInt().coerceAtLeast(48)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).also { bmp ->
            val canvas = Canvas(bmp)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
        }
        return ImageFetchResult(
            image = bitmap.asImage(),
            isSampled = false,
            dataSource = DataSource.DISK
        )
    }

    class Factory : Fetcher.Factory<PackageIcon> {
        override fun create(data: PackageIcon, options: Options, imageLoader: ImageLoader): Fetcher? {
            return PackageIconFetcher(data, options.context)
        }
    }
}

class PackageIconKeyer : Keyer<PackageIcon> {
    override fun key(data: PackageIcon, options: Options): String? = "pkg-icon:${data.packageName}"
}