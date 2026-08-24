package dev.hackunderway.phonelookup.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.KeyOff
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.hackunderway.phonelookup.data.SourceResult
import dev.hackunderway.phonelookup.data.SourceStatus

@Composable
fun SourceCard(
    result: SourceResult,
    onOpenUrl: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // null = the user has not decided, so fall back to the auto rule. Tracking the
    // override separately is what lets an auto-opened card still be collapsed.
    var userExpanded by remember(result.id) { mutableStateOf<Boolean?>(null) }
    val hasDetail = result.facts.isNotEmpty() || result.findings.isNotEmpty()

    // Auto-open the card that actually found something worth reading.
    val autoExpand = result.status == SourceStatus.OK && result.findings.size <= 3
    val effectivelyExpanded = userExpanded ?: autoExpand

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = hasDetail) { userExpanded = !effectivelyExpanded }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusIcon(result.status)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = result.id.label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = summaryLine(result),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (result.hitCount > 0) {
                Badge { Text(result.hitCount.toString()) }
                Spacer(Modifier.width(8.dp))
            }
            if (hasDetail) {
                Icon(
                    imageVector = if (effectivelyExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (effectivelyExpanded) "Collapse" else "Expand"
                )
            }
        }

        AnimatedVisibility(visible = effectivelyExpanded && hasDetail) {
            Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                if (result.facts.isNotEmpty()) {
                    result.facts.forEach { fact ->
                        Row(modifier = Modifier.padding(vertical = 3.dp)) {
                            Text(
                                text = fact.label,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(130.dp)
                            )
                            Text(text = fact.value, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    if (result.findings.isNotEmpty()) {
                        Spacer(Modifier.size(8.dp))
                        HorizontalDivider()
                    }
                }

                result.findings.forEachIndexed { index, finding ->
                    if (index > 0) HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = finding.url != null) {
                                finding.url?.let(onOpenUrl)
                            }
                            .padding(vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.Top) {
                            Text(
                                text = finding.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )
                            if (finding.url != null) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = "Open link",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        finding.subtitle?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        finding.snippet?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusIcon(status: SourceStatus) {
    when (status) {
        SourceStatus.LOADING -> CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            strokeWidth = 2.dp
        )
        SourceStatus.OK -> StatusGlyph(Icons.Default.CheckCircle, MaterialTheme.colorScheme.primary)
        SourceStatus.EMPTY -> StatusGlyph(
            Icons.Default.RemoveCircleOutline,
            MaterialTheme.colorScheme.onSurfaceVariant
        )
        SourceStatus.NEEDS_KEY -> StatusGlyph(
            Icons.Default.KeyOff,
            MaterialTheme.colorScheme.onSurfaceVariant
        )
        SourceStatus.ERROR -> StatusGlyph(Icons.Default.ErrorOutline, MaterialTheme.colorScheme.error)
    }
}

@Composable
private fun StatusGlyph(icon: ImageVector, tint: Color) {
    Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
}

private fun summaryLine(result: SourceResult): String = when (result.status) {
    SourceStatus.LOADING -> "Searching…"
    SourceStatus.OK -> when {
        result.findings.isNotEmpty() ->
            "${result.findings.size} result${if (result.findings.size == 1) "" else "s"}"
        result.facts.isNotEmpty() -> "Details available"
        else -> "Done"
    }
    SourceStatus.EMPTY -> result.message ?: "Nothing found"
    SourceStatus.NEEDS_KEY -> result.message ?: "API key required"
    SourceStatus.ERROR -> result.message ?: "Failed"
}
