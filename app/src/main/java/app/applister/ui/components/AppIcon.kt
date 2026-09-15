package app.applister.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.applister.R
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade

@Composable
fun AppIcon(
    packageName: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp
) {
    val appContext = LocalContext.current.applicationContext
    val tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
    val basePainter = painterResource(R.drawable.ic_app_placeholder)
    val placeholder = remember(basePainter, tint) {
        TintedPainter(basePainter, tint)
    }
    val model = remember(packageName, size, appContext) {
        val px = with(appContext.resources.displayMetrics) {
            (size.value * density).toInt().coerceAtLeast(1)
        }
        ImageRequest.Builder(appContext)
            .data(PackageIcon(packageName))
            .size(px, px)
            .crossfade(true)
            .build()
    }

    AsyncImage(
        model = model,
        contentDescription = stringResource(R.string.app_icon_desc, packageName),
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(8.dp)),
        placeholder = placeholder,
        error = placeholder,
        fallback = placeholder
    )
}

data class PackageIcon(
    val packageName: String,
    val targetPx: Int? = null
)

private class TintedPainter(
    private val delegate: Painter,
    private val tint: Color
) : Painter() {
    override val intrinsicSize: Size
        get() = delegate.intrinsicSize

    override fun DrawScope.onDraw() {
        with(delegate) {
            draw(size = size, colorFilter = ColorFilter.tint(tint))
        }
    }
}
