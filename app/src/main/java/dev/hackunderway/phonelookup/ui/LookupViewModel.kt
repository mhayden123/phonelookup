package dev.hackunderway.phonelookup.ui

import android.app.Application
import android.content.Context
import android.telephony.TelephonyManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.hackunderway.phonelookup.data.ApiKeys
import dev.hackunderway.phonelookup.data.History
import dev.hackunderway.phonelookup.data.HistoryEntry
import dev.hackunderway.phonelookup.data.LookupReport
import dev.hackunderway.phonelookup.data.OsintEngine
import dev.hackunderway.phonelookup.data.PhoneAnalyzer
import dev.hackunderway.phonelookup.data.PhoneInfo
import dev.hackunderway.phonelookup.data.RegionOption
import dev.hackunderway.phonelookup.data.Settings
import dev.hackunderway.phonelookup.data.SourceId
import dev.hackunderway.phonelookup.data.SourceResult
import dev.hackunderway.phonelookup.data.SourceStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

data class LookupUiState(
    val number: String = "",
    val region: RegionOption? = null,
    val regions: List<RegionOption> = emptyList(),
    val phoneInfo: PhoneInfo? = null,
    val sources: Map<SourceId, SourceResult> = emptyMap(),
    val running: Boolean = false,
    val history: List<HistoryEntry> = emptyList(),
    val startedAtMillis: Long = 0L
) {
    val canSearch: Boolean get() = number.filter { it.isDigit() }.length >= 4 && !running
    val hasResults: Boolean get() = phoneInfo != null
    val doneCount: Int get() = sources.values.count { it.status != SourceStatus.LOADING }
    val totalFindings: Int get() = sources.values.sumOf { it.hitCount }
    val missingKeys: List<SourceId>
        get() = sources.values.filter { it.status == SourceStatus.NEEDS_KEY }.map { it.id }
}

class LookupViewModel(application: Application) : AndroidViewModel(application) {

    private val settings = Settings(application)
    private val history = History(application)

    private val _state = MutableStateFlow(LookupUiState())
    val state: StateFlow<LookupUiState> = _state.asStateFlow()

    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            val regions = withContext(Dispatchers.Default) { PhoneAnalyzer.supportedRegions() }
            val preferred = settings.defaultRegion.ifBlank { deviceRegion() }
            _state.update { current ->
                current.copy(
                    regions = regions,
                    region = current.region
                        ?: regions.firstOrNull { it.code == preferred }
                        ?: regions.firstOrNull { it.code == "US" },
                    history = history.entries()
                )
            }
        }
    }

    private fun deviceRegion(): String {
        val context: Context = getApplication<Application>()
        val telephony = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        val fromSim = telephony?.simCountryIso?.takeIf { it.isNotBlank() }
            ?: telephony?.networkCountryIso?.takeIf { it.isNotBlank() }
        return (fromSim ?: Locale.getDefault().country).uppercase(Locale.ROOT)
    }

    fun onNumberChange(value: String) = _state.update { it.copy(number = value) }

    fun onRegionChange(region: RegionOption) {
        settings.defaultRegion = region.code
        _state.update { it.copy(region = region) }
    }

    fun clearResults() {
        searchJob?.cancel()
        _state.update { it.copy(phoneInfo = null, sources = emptyMap(), running = false) }
    }

    fun cancel() {
        searchJob?.cancel()
        _state.update { current ->
            current.copy(
                running = false,
                sources = current.sources.mapValues { (_, result) ->
                    if (result.status == SourceStatus.LOADING) {
                        SourceResult(result.id, SourceStatus.ERROR, message = "Cancelled.")
                    } else {
                        result
                    }
                }
            )
        }
    }

    fun analyze() {
        val current = _state.value
        val region = current.region?.code ?: "US"
        val number = current.number
        if (number.isBlank()) return

        searchJob?.cancel()

        val pending = OsintEngine.sourceIds().associateWith { SourceResult(it, SourceStatus.LOADING) }
        _state.update {
            it.copy(
                phoneInfo = null,
                sources = pending,
                running = true,
                startedAtMillis = System.currentTimeMillis()
            )
        }

        searchJob = viewModelScope.launch {
            // libphonenumber loads its metadata on first use, and the prefs and
            // history are disk-backed; keep all of it off the main thread.
            val info = withContext(Dispatchers.Default) { OsintEngine.analyzeOffline(number, region) }
            _state.update { it.copy(phoneInfo = info) }

            val (keys, recent) = withContext(Dispatchers.IO) {
                history.add(
                    HistoryEntry(
                        number = info.input,
                        region = region,
                        label = info.international ?: info.input,
                        timestampMillis = System.currentTimeMillis()
                    )
                )
                ApiKeys(
                    numverify = settings.numverifyKey,
                    serpApi = settings.serpApiKey,
                    github = settings.githubToken
                ) to history.entries()
            }
            _state.update { it.copy(history = recent) }

            OsintEngine.runSources(info, keys).collect { result ->
                _state.update { it.copy(sources = it.sources + (result.id to result)) }
            }
            _state.update { it.copy(running = false) }
        }
    }

    fun rerun(entry: HistoryEntry) {
        val region = _state.value.regions.firstOrNull { it.code == entry.region }
        _state.update { it.copy(number = entry.number, region = region ?: it.region) }
        analyze()
    }

    fun clearHistory() {
        history.clear()
        _state.update { it.copy(history = emptyList()) }
    }

    fun buildReport(): LookupReport? {
        val info = _state.value.phoneInfo ?: return null
        return LookupReport(
            phoneInfo = info,
            sources = _state.value.sources.values.toList(),
            timestampMillis = System.currentTimeMillis()
        )
    }
}
