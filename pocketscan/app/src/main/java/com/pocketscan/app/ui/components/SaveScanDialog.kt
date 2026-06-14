package com.pocketscan.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pocketscan.app.R

/**
 * Shown after a successful scan. Lets the user confirm the file name before saving
 * to local app-private storage, or discard the scan without persisting.
 */
@Composable
fun SaveScanDialog(
    suggestedName: String,
    onSave: (String) -> Unit,
    onDiscard: () -> Unit,
) {
    var value by remember { mutableStateOf(suggestedName) }
    val trimmed = value.trim()
    val emptyErr = trimmed.isEmpty()
    val invalidErr = !emptyErr && trimmed.contains(Regex("""[\\/:*?"<>|]"""))

    AlertDialog(
        onDismissRequest = { /* require explicit Save or Discard */ },
        icon = { Icon(Icons.Filled.Save, contentDescription = null) },
        title = { Text(stringResource(R.string.save_scan_title)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 4.dp),
            ) {
                Text(stringResource(R.string.save_scan_body))
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    singleLine = true,
                    isError = emptyErr || invalidErr,
                    label = { Text(stringResource(R.string.rename_hint)) },
                    supportingText = {
                        when {
                            emptyErr -> Text(stringResource(R.string.name_error_empty))
                            invalidErr -> Text(stringResource(R.string.name_error_invalid))
                        }
                    },
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(trimmed) },
                enabled = !emptyErr && !invalidErr,
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = onDiscard) {
                Text(stringResource(R.string.discard))
            }
        },
    )
}
