package app.applister.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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
        error = {
            Icon(
                imageVector = Icons.Default.Android,
                contentDescription = null,
                modifier = Modifier.size(size),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        placeholder = {
            Icon(
                imageVector = Icons.Default.Android,
                contentDescription = null,
                modifier = Modifier.size(size),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    )
}

data class PackageIcon(val packageName: String)