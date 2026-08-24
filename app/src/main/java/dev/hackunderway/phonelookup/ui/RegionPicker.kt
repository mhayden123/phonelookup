package dev.hackunderway.phonelookup.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.hackunderway.phonelookup.data.RegionOption
import java.util.Locale

@Composable
fun RegionPickerDialog(
    regions: List<RegionOption>,
    selected: RegionOption?,
    onPick: (RegionOption) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }

    val filtered = remember(query, regions) {
        val needle = query.trim().lowercase(Locale.ROOT)
        if (needle.isEmpty()) regions
        else regions.filter {
            it.name.lowercase(Locale.ROOT).contains(needle) ||
                it.code.lowercase(Locale.ROOT).contains(needle) ||
                it.countryCode.toString().contains(needle)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        title = { Text("Country") },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Search country or code") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                    items(filtered, key = { it.code }) { region ->
                        val isSelected = region.code == selected?.code
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPick(region) }
                                .padding(vertical = 12.dp)
                        ) {
                            Text(
                                text = region.display,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
    )
}
