@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.ui.ExperimentalComposeUiApi::class
)

package dev.hackunderway.phonelookup.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.hackunderway.phonelookup.data.PhoneInfo
import dev.hackunderway.phonelookup.report.ReportExporter
import kotlinx.coroutines.launch

@Composable
fun LookupScreen(
    viewModel: LookupViewModel,
    onOpenSettings: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val clipboard = LocalClipboardManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val snackbarHost = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showRegionPicker by remember { mutableStateOf(false) }

    if (showRegionPicker) {
        RegionPickerDialog(
            regions = state.regions,
            selected = state.region,
            onPick = {
                viewModel.onRegionChange(it)
                showRegionPicker = false
            },
            onDismiss = { showRegionPicker = false }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            TopAppBar(
                title = { Text("PhoneLookup") },
                actions = {
                    if (state.hasResults) {
                        IconButton(onClick = {
                            val report = viewModel.buildReport()
                            if (report == null) return@IconButton
                            runCatching {
                                context.startActivity(ReportExporter.shareIntent(context, report))
                            }.onFailure {
                                scope.launch { snackbarHost.showSnackbar("Could not share the report.") }
                            }
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "Share report")
                        }
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SearchCard(
                    number = state.number,
                    regionLabel = state.region?.display ?: "Choose country",
                    canSearch = state.canSearch,
                    running = state.running,
                    onNumberChange = viewModel::onNumberChange,
                    onPickRegion = { showRegionPicker = true },
                    onSearch = {
                        keyboard?.hide()
                        viewModel.analyze()
                    },
                    onCancel = viewModel::cancel
                )
            }

            state.phoneInfo?.let { info ->
                item {
                    PhoneInfoCard(
                        info = info,
                        onCall = {
                            runCatching {
                                context.startActivity(
                                    Intent(Intent.ACTION_DIAL, Uri.parse("tel:${info.dialable}"))
                                )
                            }
                        },
                        onSms = {
                            runCatching {
                                context.startActivity(
                                    Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${info.dialable}"))
                                )
                            }
                        },
                        onCopy = {
                            clipboard.setText(AnnotatedString(info.dialable))
                            scope.launch { snackbarHost.showSnackbar("Copied ${info.dialable}") }
                        }
                    )
                }

                item {
                    ProgressRow(
                        done = state.doneCount,
                        total = state.sources.size,
                        totalFindings = state.totalFindings
                    )
                }

                items(state.sources.values.toList(), key = { it.id.name }) { result ->
                    SourceCard(result = result, onOpenUrl = { url ->
                        runCatching { uriHandler.openUri(url) }
                    })
                }

                if (state.missingKeys.isNotEmpty()) {
                    item {
                        MissingKeysHint(count = state.missingKeys.size, onOpenSettings = onOpenSettings)
                    }
                }
            }

            if (!state.hasResults && state.history.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Recent", style = MaterialTheme.typography.titleSmall)
                        TextButton(onClick = viewModel::clearHistory) { Text("Clear") }
                    }
                }
                items(state.history, key = { it.number + it.timestampMillis }) { entry ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ),
                        onClick = { viewModel.rerun(entry) }
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Text(entry.label, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                entry.region,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchCard(
    number: String,
    regionLabel: String,
    canSearch: Boolean,
    running: Boolean,
    onNumberChange: (String) -> Unit,
    onPickRegion: () -> Unit,
    onSearch: () -> Unit,
    onCancel: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = number,
                onValueChange = onNumberChange,
                label = { Text("Phone number") },
                placeholder = { Text("+51 987 654 321") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(onSearch = { if (canSearch) onSearch() }),
                trailingIcon = {
                    if (number.isNotEmpty()) {
                        IconButton(onClick = { onNumberChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedButton(onClick = onPickRegion, modifier = Modifier.fillMaxWidth()) {
                Text(regionLabel)
            }

            if (running) {
                OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
                    Text("Cancel")
                }
            } else {
                Button(
                    onClick = onSearch,
                    enabled = canSearch,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Look up")
                }
            }
        }
    }
}

@Composable
private fun PhoneInfoCard(
    info: PhoneInfo,
    onCall: () -> Unit,
    onSms: () -> Unit,
    onCopy: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = info.international ?: info.input,
                style = MaterialTheme.typography.headlineSmall,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = when {
                    info.valid -> "Valid number"
                    info.possible -> "Not valid, but a possible number"
                    else -> info.error ?: "Not a valid number"
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (info.valid) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.error
            )

            Spacer(Modifier.height(12.dp))

            InfoRow("Location", info.location)
            InfoRow("Carrier", info.carrier)
            InfoRow("Line type", info.lineType)
            InfoRow("Country code", info.countryCode?.let { "+$it" })
            InfoRow("National", info.national)
            InfoRow("E.164", info.e164)
            InfoRow("Time zones", info.timezones.takeIf { it.isNotEmpty() }?.joinToString(", "))

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AssistChip(
                    onClick = onCall,
                    label = { Text("Call") },
                    leadingIcon = { Icon(Icons.Default.Call, null, Modifier.size(16.dp)) }
                )
                AssistChip(
                    onClick = onSms,
                    label = { Text("SMS") },
                    leadingIcon = { Icon(Icons.Default.Sms, null, Modifier.size(16.dp)) }
                )
                AssistChip(
                    onClick = onCopy,
                    label = { Text("Copy") },
                    leadingIcon = { Icon(Icons.Default.ContentCopy, null, Modifier.size(16.dp)) }
                )
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String?) {
    if (value.isNullOrBlank()) return
    Row(modifier = Modifier.padding(vertical = 3.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(110.dp)
        )
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ProgressRow(done: Int, total: Int, totalFindings: Int) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (done < total) "Searching sources ($done/$total)" else "$total sources searched",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "$totalFindings finding${if (totalFindings == 1) "" else "s"}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
        if (done < total && total > 0) {
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { done.toFloat() / total.toFloat() },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun MissingKeysHint(count: Int, onOpenSettings: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        ),
        onClick = onOpenSettings
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "$count source${if (count == 1) "" else "s"} skipped",
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                "Add free API keys in Settings to search those too.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
