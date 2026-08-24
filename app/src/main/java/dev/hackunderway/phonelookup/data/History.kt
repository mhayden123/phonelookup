package dev.hackunderway.phonelookup.data

import android.content.Context
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject

data class HistoryEntry(
    val number: String,
    val region: String,
    val label: String,
    val timestampMillis: Long
)

/** The last few lookups, so you can re-run one with a tap. */
class History(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("phonelookup_history", Context.MODE_PRIVATE)

    fun entries(): List<HistoryEntry> = try {
        val array = JSONArray(prefs.getString(KEY, "[]").orEmpty())
        (0 until array.length()).mapNotNull { i ->
            val o = array.optJSONObject(i) ?: return@mapNotNull null
            HistoryEntry(
                number = o.optString("number"),
                region = o.optString("region"),
                label = o.optString("label"),
                timestampMillis = o.optLong("ts")
            )
        }.filter { it.number.isNotBlank() }
    } catch (e: Exception) {
        emptyList()
    }

    fun add(entry: HistoryEntry) {
        val kept = (listOf(entry) + entries().filterNot { it.number == entry.number })
            .take(MAX_ENTRIES)
        val array = JSONArray()
        kept.forEach {
            array.put(
                JSONObject()
                    .put("number", it.number)
                    .put("region", it.region)
                    .put("label", it.label)
                    .put("ts", it.timestampMillis)
            )
        }
        prefs.edit { putString(KEY, array.toString()) }
    }

    fun clear() = prefs.edit { remove(KEY) }

    private companion object {
        const val KEY = "entries"
        const val MAX_ENTRIES = 25
    }
}
