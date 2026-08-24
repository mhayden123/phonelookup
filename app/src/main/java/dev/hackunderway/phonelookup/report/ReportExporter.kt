package dev.hackunderway.phonelookup.report

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import dev.hackunderway.phonelookup.data.LookupReport
import dev.hackunderway.phonelookup.data.SourceStatus
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Builds the JSON report the desktop tool writes, and hands it to the share sheet. */
object ReportExporter {

    private val fileStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
    private val isoStamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)

    fun toJson(report: LookupReport): String {
        val info = report.phoneInfo

        val phoneInfo = JSONObject()
            .put("input", info.input)
            .put("valid", info.valid)
            .put("possible", info.possible)
            .put("e164", info.e164 ?: JSONObject.NULL)
            .put("international", info.international ?: JSONObject.NULL)
            .put("national", info.national ?: JSONObject.NULL)
            .put("rfc3966", info.rfc3966 ?: JSONObject.NULL)
            .put("country_code", info.countryCode ?: JSONObject.NULL)
            .put("national_number", info.nationalNumber ?: JSONObject.NULL)
            .put("region_code", info.regionCode ?: JSONObject.NULL)
            .put("location", info.location ?: JSONObject.NULL)
            .put("carrier", info.carrier ?: JSONObject.NULL)
            .put("line_type", info.lineType ?: JSONObject.NULL)
            .put("timezone", JSONArray(info.timezones))

        val sources = JSONObject()
        report.sources.forEach { source ->
            val facts = JSONObject()
            source.facts.forEach { facts.put(it.label, it.value) }

            val findings = JSONArray()
            source.findings.forEach { finding ->
                findings.put(
                    JSONObject()
                        .put("title", finding.title)
                        .put("subtitle", finding.subtitle ?: JSONObject.NULL)
                        .put("snippet", finding.snippet ?: JSONObject.NULL)
                        .put("url", finding.url ?: JSONObject.NULL)
                )
            }

            sources.put(
                source.id.name.lowercase(Locale.US),
                JSONObject()
                    .put("status", source.status.name)
                    .put("message", source.message ?: JSONObject.NULL)
                    .put("facts", facts)
                    .put("findings", findings)
            )
        }

        return JSONObject()
            .put(
                "metadata",
                JSONObject()
                    .put("phone", info.input)
                    .put("region", info.region)
                    .put("timestamp", isoStamp.format(Date(report.timestampMillis)))
                    .put("tool", "PhoneLookup (Android) - SearchPhone OSINT port")
            )
            .put("phone_info", phoneInfo)
            .put("sources", sources)
            .put("total_findings", report.totalFindings)
            .toString(2)
    }

    fun toPlainText(report: LookupReport): String = buildString {
        val info = report.phoneInfo
        appendLine("PhoneLookup report - ${info.input}")
        appendLine("Generated ${isoStamp.format(Date(report.timestampMillis))}")
        appendLine()
        appendLine("NUMBER")
        info.international?.let { appendLine("  International: $it") }
        info.e164?.let { appendLine("  E.164: $it") }
        appendLine("  Valid: ${if (info.valid) "yes" else "no"}")
        info.location?.let { appendLine("  Location: $it") }
        info.carrier?.let { appendLine("  Carrier: $it") }
        info.lineType?.let { appendLine("  Line type: $it") }
        if (info.timezones.isNotEmpty()) appendLine("  Time zones: ${info.timezones.joinToString(", ")}")
        appendLine()

        report.sources.forEach { source ->
            appendLine("${source.id.label.uppercase(Locale.US)} [${source.status.name}]")
            source.message?.takeIf { source.status != SourceStatus.OK }?.let { appendLine("  $it") }
            source.facts.forEach { appendLine("  ${it.label}: ${it.value}") }
            source.findings.forEach { finding ->
                appendLine("  - ${finding.title}")
                finding.subtitle?.let { appendLine("    $it") }
                finding.url?.let { appendLine("    $it") }
            }
            appendLine()
        }
        appendLine("Total findings: ${report.totalFindings}")
    }

    /** Writes the JSON report to cache and returns a shareable content:// Uri. */
    fun writeJsonFile(context: Context, report: LookupReport): Uri {
        val dir = File(context.cacheDir, "reports").apply { mkdirs() }
        // Only keep the most recent handful of reports around.
        dir.listFiles()?.sortedByDescending { it.lastModified() }?.drop(9)?.forEach { it.delete() }

        val cleanNumber = (report.phoneInfo.e164 ?: report.phoneInfo.input)
            .filter { it.isLetterOrDigit() }
        val file = File(dir, "phone_${cleanNumber}_${fileStamp.format(Date(report.timestampMillis))}.json")
        file.writeText(toJson(report))

        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    fun shareIntent(context: Context, report: LookupReport): Intent {
        val uri = writeJsonFile(context, report)
        return Intent.createChooser(
            Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "PhoneLookup report ${report.phoneInfo.input}")
                putExtra(Intent.EXTRA_TEXT, toPlainText(report).take(4000))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            },
            "Share report"
        )
    }
}
