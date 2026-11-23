package com.mediadock.app.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mediadock.app.core.datastore.WorkspaceRules
import com.mediadock.app.core.network.ApiConstants

@Composable
internal fun ServerConnectionDialog(
    serverUrl: String,
    diagnostics: DiagnosticsViewModel,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var address by rememberSaveable(serverUrl) { mutableStateOf(serverUrl) }
    val check by diagnostics.connection.collectAsState()
    val edited = address.trim().trimEnd('/') != serverUrl.trimEnd('/')
    val valid = WorkspaceRules.validUrl(address)
    val currentCheck = check?.takeIf { it.serverUrl == serverUrl && !edited }
    val checking = currentCheck != null && currentCheck.result == null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Server connection") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Server URL") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = address.isNotBlank() && !valid,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(
                        onClick = { address = ApiConstants.DEFAULT_BASE_URL },
                        enabled = address.trim().trimEnd('/') != ApiConstants.DEFAULT_BASE_URL.trimEnd('/')
                    ) { Text("Use default") }
                    TextButton(
                        onClick = { diagnostics.check(serverUrl) },
                        enabled = !edited && !checking
                    ) { Text(if (checking) "Checking…" else "Test connection") }
                }
                if (checking) LinearProgressIndicator(Modifier.fillMaxWidth())
                currentCheck?.result?.let { result ->
                    Text(result, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (edited) onSave(address.trim())
                    onDismiss()
                },
                enabled = !edited || valid
            ) { Text(if (edited) "Save" else "Done") }
        },
        dismissButton = {
            if (edited) TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
