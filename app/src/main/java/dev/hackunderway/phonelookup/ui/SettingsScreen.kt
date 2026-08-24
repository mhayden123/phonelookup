@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package dev.hackunderway.phonelookup.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import dev.hackunderway.phonelookup.data.Settings

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val settings = remember { Settings(context) }

    var numverify by remember { mutableStateOf(settings.numverifyKey) }
    var serpApi by remember { mutableStateOf(settings.serpApiKey) }
    var github by remember { mutableStateOf(settings.githubToken) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    "Three sources need a free API key. Everything else — number " +
                        "validation, carrier, Hudson Rock, DuckDuckGo and Reddit — works " +
                        "without one.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                KeyField(
                    label = "Numverify key",
                    description = "Carrier and line type. Free tier: 100 lookups a month.",
                    linkLabel = "Get a key at numverify.com",
                    link = "https://numverify.com/product",
                    value = numverify,
                    onValueChange = {
                        numverify = it
                        settings.numverifyKey = it
                    },
                    onOpenLink = { uriHandler.openUri(it) }
                )
            }

            item {
                KeyField(
                    label = "SerpAPI key",
                    description = "Google results. Free tier: 250 searches a month.",
                    linkLabel = "Get a key at serpapi.com",
                    link = "https://serpapi.com/users/sign_up",
                    value = serpApi,
                    onValueChange = {
                        serpApi = it
                        settings.serpApiKey = it
                    },
                    onOpenLink = { uriHandler.openUri(it) }
                )
            }

            item {
                KeyField(
                    label = "GitHub token",
                    description = "Code search. A classic token with no scopes is enough.",
                    linkLabel = "Create a token on github.com",
                    link = "https://github.com/settings/tokens",
                    value = github,
                    onValueChange = {
                        github = it
                        settings.githubToken = it
                    },
                    onOpenLink = { uriHandler.openUri(it) }
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("About", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "An Android port of SearchPhone by HackUnderway. Keys are saved " +
                                "in this app's private storage and are only ever sent to the " +
                                "service they belong to.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(onClick = {
                            uriHandler.openUri("https://github.com/HackUnderway/SearchPhone")
                        }) {
                            Text("Original project")
                        }
                        Text(
                            "Look up numbers you have a legitimate reason to investigate, and " +
                                "follow the laws that apply where you are.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KeyField(
    label: String,
    description: String,
    linkLabel: String,
    link: String,
    value: String,
    onValueChange: (String) -> Unit,
    onOpenLink: (String) -> Unit
) {
    var visible by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            singleLine = true,
            visualTransformation = if (visible) VisualTransformation.None
            else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { visible = !visible }) {
                    Icon(
                        imageVector = if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (visible) "Hide" else "Show"
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, top = 2.dp)
        )
        TextButton(onClick = { onOpenLink(link) }) { Text(linkLabel) }
    }
}
