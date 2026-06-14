package com.pocketscan.app.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pocketscan.app.PocketScanApplication
import com.pocketscan.app.ui.theme.Shapes

/**
 * A rounded square thumbnail that shows the document's first-page bitmap when available,
 * or falls back to a Material document icon on a tonal background.
 */
@Composable
fun DocumentThumbnail(
    thumbnailPath: String?,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
) {
    val context = LocalContext.current
    val container = (context.applicationContext as PocketScanApplication).container
    val bitmap = remember(thumbnailPath) {
        thumbnailPath?.let { path ->
            runCatching {
                BitmapFactory.decodeFile(container.fileManager.resolveThumbnail(path).absolutePath)
            }.getOrNull()
        }
    }
    Box(
        modifier = modifier
            .size(size)
            .clip(Shapes.card)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(size),
            )
        } else {
            Icon(
                Icons.Filled.Description,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
