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
import coil3.size.Dimension
import coil3.size.Size
import coil3.size.isOriginal

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
        } catch (_: SecurityException) {
            return null
        } catch (_: Exception) {
            return null
        }

        val density = context.resources.displayMetrics.density
        val targetPx = data.targetPx ?: (density * DEFAULT_DP).toInt().coerceAtLeast(MIN_PX)
        val size = targetPx.coerceIn(MIN_PX, MAX_PX)
        val bitmap = try {
            Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).also { bmp ->
                val canvas = Canvas(bmp)
                drawable.setBounds(0, 0, canvas.width, canvas.height)
                drawable.draw(canvas)
            }
        } catch (_: OutOfMemoryError) {
            return null
        } catch (_: Exception) {
            return null
        }
        return ImageFetchResult(
            image = bitmap.asImage(),
            isSampled = false,
            dataSource = DataSource.MEMORY
        )
    }

    class Factory : Fetcher.Factory<PackageIcon> {
        override fun create(data: PackageIcon, options: Options, imageLoader: ImageLoader): Fetcher? {
            val targetPx = options.size.let { size ->
                if (size.isOriginal) {
                    null
                } else {
                    val w = (size.width as? Dimension.Pixels)?.px
                    val h = (size.height as? Dimension.Pixels)?.px
                    val px = when {
                        w != null && h != null -> minOf(w, h)
                        w != null -> w
                        h != null -> h
                        else -> null
                    }
                    px?.coerceIn(MIN_PX, MAX_PX)
                }
            }
            return PackageIconFetcher(
                data.copy(targetPx = targetPx),
                options.context.applicationContext
            )
        }
    }

    companion object {
        private const val DEFAULT_DP = 48
        private const val MIN_PX = 48
        private const val MAX_PX = 512
    }
}

class PackageIconKeyer : Keyer<PackageIcon> {
    override fun key(data: PackageIcon, options: Options): String {
        val ctx = options.context.applicationContext
        val (version, updated) = try {
            val pm = ctx.packageManager
            val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(
                    data.packageName,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(data.packageName, 0)
            }
            val v = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                info.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                info.versionCode.toLong()
            }
            v to info.lastUpdateTime
        } catch (_: Exception) {
            0L to 0L
        }
        return "pkg-icon:${data.packageName}:v$version:u$updated"
    }
}
