package dev.hackunderway.phonelookup

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.hackunderway.phonelookup.ui.LookupScreen
import dev.hackunderway.phonelookup.ui.LookupViewModel
import dev.hackunderway.phonelookup.ui.SettingsScreen
import dev.hackunderway.phonelookup.ui.theme.PhoneLookupTheme

class MainActivity : ComponentActivity() {

    private val viewModel: LookupViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleSharedNumber(intent)

        setContent {
            PhoneLookupTheme {
                AppRoot(viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleSharedNumber(intent)
    }

    /** Accept a number shared in from another app. */
    private fun handleSharedNumber(intent: Intent?) {
        if (intent?.action != Intent.ACTION_SEND) return
        val shared = intent.getStringExtra(Intent.EXTRA_TEXT)?.trim().orEmpty()
        if (shared.isNotBlank()) viewModel.onNumberChange(shared)
    }
}

@Composable
private fun AppRoot(viewModel: LookupViewModel) {
    var showSettings by remember { mutableStateOf(false) }

    BackHandler(enabled = showSettings) { showSettings = false }

    if (showSettings) {
        SettingsScreen(onBack = { showSettings = false })
    } else {
        LookupScreen(viewModel = viewModel, onOpenSettings = { showSettings = true })
    }
}
