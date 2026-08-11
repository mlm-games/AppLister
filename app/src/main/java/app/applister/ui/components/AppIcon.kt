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
    val context = LocalContext.current
    val tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
    val basePainter = painterResource(R.drawable.ic_app_placeholder)
    val placeholder = remember(basePainter, tint) {
        TintedPainter(basePainter, tint)
    }
    val model = remember(packageName) {
        ImageRequest.Builder(context)
            .data(PackageIcon(packageName))
            .crossfade(true)
            .build()
    }

    AsyncImage(
        model = model,
        contentDescription = null,
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(8.dp)),
        placeholder = placeholder,
        error = placeholder,
        fallback = placeholder
    )
}

data class PackageIcon(val packageName: String)

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