package com.pocketscan.app.ui.home

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pocketscan.app.PocketScanApplication
import com.pocketscan.app.R
import com.pocketscan.app.domain.Document
import com.pocketscan.app.ui.components.DeleteDialog
import com.pocketscan.app.ui.components.DocumentActionSheet
import com.pocketscan.app.ui.components.DocumentRow
import com.pocketscan.app.ui.components.EmptyScansState
import com.pocketscan.app.ui.components.RenameDialog
import com.pocketscan.app.ui.components.SaveScanDialog
import com.pocketscan.app.ui.theme.Shapes
import com.pocketscan.app.ui.theme.Spacing
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onOpenScan: (Long) -> Unit,
    onOpenPrivacy: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val saving by viewModel.saving.collectAsStateWithLifecycle()
    val pendingScan by viewModel.pendingScan.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val container = (context.applicationContext as PocketScanApplication).container

    var sheetDocument by remember { mutableStateOf<Document?>(null) }
    var renameDocument by remember { mutableStateOf<Document?>(null) }
    var deleteDocument by remember { mutableStateOf<Document?>(null) }

    val scannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val parsed = container.scannerManager.parseResult(result.data)
            val pdfUri = parsed?.pdf?.uri
            if (pdfUri != null) viewModel.onScanResult(pdfUri)
            else viewModel.onScannerError(context.getString(R.string.error_scan_failed))
        } else if (result.resultCode == Activity.RESULT_CANCELED) {
            viewModel.onScannerError(context.getString(R.string.scan_canceled))
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { e ->
            when (e) {
                is HomeEvent.ShowSnackbar -> snackbarHostState.showSnackbar(e.message)
            }
        }
    }

    fun launchScanner() {
        val activity = context as? Activity ?: return
        container.scannerManager.startScanIntent(activity)
            .addOnSuccessListener { sender ->
                scannerLauncher.launch(container.scannerManager.toRequest(sender))
            }
            .addOnFailureListener {
                scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.error_scanner_unavailable)) }
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                    )
                },
                actions = {
                    IconButton(onClick = onOpenPrivacy) {
                        Icon(
                            Icons.Outlined.Shield,
                            contentDescription = stringResource(R.string.privacy),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        floatingActionButton = {
            if (state is HomeUiState.Success) {
                ExtendedFloatingActionButton(
                    onClick = { launchScanner() },
                    icon = { Icon(Icons.Filled.DocumentScanner, contentDescription = null) },
                    text = { Text(stringResource(R.string.scan_document)) },
                    shape = Shapes.pill,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 2.dp),
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.surface,
    ) { padding ->
        when (val s = state) {
            HomeUiState.Loading -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
            HomeUiState.Empty -> EmptyScansState(
                onScan = { launchScanner() },
                modifier = Modifier.padding(padding),
            )
            is HomeUiState.Success -> DocumentList(
                state = s,
                padding = padding,
                onOpen = onOpenScan,
                onOverflow = { sheetDocument = it },
            )
        }
        if (saving) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Text(stringResource(R.string.saving), modifier = Modifier.padding(top = Spacing.m))
                }
            }
        }
    }

    pendingScan?.let { pending ->
        SaveScanDialog(
            suggestedName = pending.suggestedName,
            onSave = { name ->
                viewModel.confirmSavePendingScan(name)
                scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.snack_scan_saved)) }
            },
            onDiscard = { viewModel.discardPendingScan() },
        )
    }

    sheetDocument?.let { doc ->
        DocumentActionSheet(
            document = doc,
            onDismiss = { sheetDocument = null },
            onOpen = { onOpenScan(doc.id) },
            onShare = {
                val uri = container.fileManager.shareUri(doc.pdfPath)
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
            onRename = { renameDocument = doc },
            onDelete = { deleteDocument = doc },
        )
    }

    renameDocument?.let { doc ->
        RenameDialog(
            initial = doc.name,
            onDismiss = { renameDocument = null },
            onConfirm = { name ->
                renameDocument = null
                viewModel.renameDocument(doc.id, name) {
                    scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.snack_renamed)) }
                }
            },
        )
    }

    deleteDocument?.let { doc ->
        DeleteDialog(
            onDismiss = { deleteDocument = null },
            onConfirm = {
                deleteDocument = null
                viewModel.deleteDocument(doc.id) {
                    scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.snack_deleted)) }
                }
            },
        )
    }
}

@Composable
private fun DocumentList(
    state: HomeUiState.Success,
    padding: PaddingValues,
    onOpen: (Long) -> Unit,
    onOverflow: (Document) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(
            start = Spacing.screenHorizontal,
            end = Spacing.screenHorizontal,
            top = Spacing.s,
            bottom = 96.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(Spacing.s),
    ) {
        item {
            Text(
                stringResource(R.string.recent_scans),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = Spacing.xs, top = Spacing.s, bottom = Spacing.s),
            )
        }
        items(state.documents, key = { it.id }) { doc ->
            DocumentRow(
                document = doc,
                onClick = { onOpen(doc.id) },
                onOverflow = { onOverflow(doc) },
            )
        }
    }
}
