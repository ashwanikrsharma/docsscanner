package com.pocketscan.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pocketscan.app.R
import com.pocketscan.app.domain.Document
import com.pocketscan.app.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentActionSheet(
    document: Document,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(bottom = Spacing.l),
        ) {
            // Identity header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.l, vertical = Spacing.s),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DocumentThumbnail(document.thumbnailPath, size = 48.dp)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = Spacing.m),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = document.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                    )
                    val pageCount = document.pageCount.coerceAtLeast(0)
                    Text(
                        text = "${pluralStringResource(R.plurals.pages, pageCount, pageCount)} · ${formatSize(document.fileSizeBytes)} · ${formatDate(document.createdAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.xs))

            ActionRow(
                icon = Icons.Filled.OpenInNew,
                label = stringResource(R.string.open_externally),
                onClick = { onDismiss(); onOpen() },
            )
            ActionRow(
                icon = Icons.Filled.Share,
                label = stringResource(R.string.share),
                onClick = { onDismiss(); onShare() },
            )
            ActionRow(
                icon = Icons.Filled.Edit,
                label = stringResource(R.string.rename),
                onClick = { onDismiss(); onRename() },
            )
            ActionRow(
                icon = Icons.Filled.Delete,
                label = stringResource(R.string.delete),
                tint = MaterialTheme.colorScheme.error,
                onClick = { onDismiss(); onDelete() },
            )
        }
    }
}

@Composable
private fun ActionRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.onSurface,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = androidx.compose.ui.semantics.Role.Button, onClick = onClick)
            .padding(horizontal = Spacing.l, vertical = Spacing.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = tint,
            modifier = Modifier.padding(start = Spacing.l),
        )
    }
}
