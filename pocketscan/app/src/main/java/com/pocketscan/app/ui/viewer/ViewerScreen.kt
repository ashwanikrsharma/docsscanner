package com.pocketscan.app.ui.viewer

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pocketscan.app.PocketScanApplication
import com.pocketscan.app.R
import com.pocketscan.app.domain.Document
import com.pocketscan.app.ui.components.DeleteDialog
import com.pocketscan.app.ui.components.MetadataRow
import com.pocketscan.app.ui.components.RenameDialog
import com.pocketscan.app.ui.components.formatDate
import com.pocketscan.app.ui.components.formatSize
import com.pocketscan.app.ui.theme.Shapes
import com.pocketscan.app.ui.theme.Spacing
import kotlinx.coroutines.launch

@Composable
fun ViewerScreen(
    viewModel: ViewerViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showRename by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { ev ->
            when (ev) {
                is ViewerEvent.Snackbar -> snackbarHostState.showSnackbar(ev.message)
                ViewerEvent.Close -> onBack()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (val s = state) {
                            is ViewerUiState.Loaded -> s.document.name
                            else -> stringResource(R.string.details)
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.surface,
    ) { padding ->
        when (val s = state) {
            ViewerUiState.Loading, ViewerUiState.Deleted -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                if (s == ViewerUiState.Loading) CircularProgressIndicator()
            }
            is ViewerUiState.Error -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(s.message)
            }
            is ViewerUiState.Loaded -> LoadedContent(
                document = s.document,
                padding = padding,
                onShare = {
                    val uri = viewModel.shareUri() ?: return@LoadedContent
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "application/pdf"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    val chooser = Intent.createChooser(send, null).apply {
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    try { context.startActivity(chooser) }
                    catch (_: ActivityNotFoundException) {
                        scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.snack_unable_to_open)) }
                    }
                },
                onOpenExternal = {
                    val uri = viewModel.shareUri() ?: return@LoadedContent
                    val view = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "application/pdf")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    try { context.startActivity(view) }
                    catch (_: ActivityNotFoundException) {
                        scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.snack_unable_to_open)) }
                    }
                },
                onRename = { showRename = true },
                onDelete = { showDelete = true },
            )
        }
    }

    if (showRename) {
        val current = (state as? ViewerUiState.Loaded)?.document?.name.orEmpty()
        RenameDialog(
            initial = current,
            onDismiss = { showRename = false },
            onConfirm = { name -> showRename = false; viewModel.rename(name) },
        )
    }
    if (showDelete) {
        DeleteDialog(
            onDismiss = { showDelete = false },
            onConfirm = { showDelete = false; viewModel.delete() },
        )
    }
}

@Composable
private fun LoadedContent(
    document: Document,
    padding: PaddingValues,
    onShare: () -> Unit,
    onOpenExternal: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    val context = LocalContext.current
    val container = (context.applicationContext as PocketScanApplication).container
    val thumb = remember(document.thumbnailPath) {
        document.thumbnailPath?.let { path ->
            runCatching {
                BitmapFactory.decodeFile(container.fileManager.resolveThumbnail(path).absolutePath)
            }.getOrNull()
        }
    }
    val locationLabel = remember(document.pdfPath) {
        container.repository.describeLocation(context, document.pdfPath)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.l),
        verticalArrangement = Arrangement.spacedBy(Spacing.l),
    ) {
        // Preview card
        Surface(
            shape = Shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.xl),
                contentAlignment = Alignment.Center,
            ) {
                if (thumb != null) {
                    Image(
                        bitmap = thumb.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .size(240.dp)
                            .clip(Shapes.card)
                            .background(MaterialTheme.colorScheme.surface),
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(140.dp)
                            .clip(Shapes.large)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Filled.Description,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }
        }

        Button(
            onClick = onOpenExternal,
            shape = Shapes.pill,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Filled.OpenInNew, contentDescription = null)
            Text(stringResource(R.string.open_pdf), modifier = Modifier.padding(start = Spacing.s))
        }
        OutlinedButton(
            onClick = onShare,
            shape = Shapes.pill,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Filled.Share, contentDescription = null)
            Text(stringResource(R.string.share), modifier = Modifier.padding(start = Spacing.s))
        }

        Surface(
            shape = Shapes.card,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(horizontal = Spacing.l, vertical = Spacing.s)) {
                MetadataRow(
                    icon = Icons.Outlined.CalendarMonth,
                    label = stringResource(R.string.created),
                    value = formatDate(document.createdAt),
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                MetadataRow(
                    icon = Icons.Filled.CalendarToday,
                    label = stringResource(R.string.updated),
                    value = formatDate(document.updatedAt),
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                val pageCount = document.pageCount.coerceAtLeast(0)
                MetadataRow(
                    icon = Icons.Filled.Description,
                    label = stringResource(R.string.pages_label),
                    value = pluralStringResource(R.plurals.pages, pageCount, pageCount),
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                MetadataRow(
                    icon = Icons.Outlined.Storage,
                    label = stringResource(R.string.size_label),
                    value = formatSize(document.fileSizeBytes),
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                MetadataRow(
                    icon = Icons.Filled.FolderShared,
                    label = stringResource(R.string.location_label),
                    value = locationLabel,
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.m),
            modifier = Modifier.fillMaxWidth(),
        ) {
            OutlinedButton(
                onClick = onRename,
                shape = Shapes.pill,
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Filled.Edit, contentDescription = null)
                Text(stringResource(R.string.rename), modifier = Modifier.padding(start = Spacing.s))
            }
            OutlinedButton(
                onClick = onDelete,
                shape = Shapes.pill,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) {
                Icon(Icons.Filled.Delete, contentDescription = null)
                Text(stringResource(R.string.delete), modifier = Modifier.padding(start = Spacing.s))
            }
        }
    }
}
