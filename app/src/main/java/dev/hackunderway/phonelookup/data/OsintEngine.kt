package dev.hackunderway.phonelookup.data

import dev.hackunderway.phonelookup.data.sources.ALL_SOURCES
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch

/**
 * Runs every source at once and streams results back as each one lands, so the
 * screen fills in progressively instead of waiting for the slowest API.
 */
object OsintEngine {

    fun analyzeOffline(input: String, region: String): PhoneInfo =
        PhoneAnalyzer.analyze(input, region)

    fun sourceIds(): List<SourceId> = ALL_SOURCES.map { it.id }

    fun runSources(info: PhoneInfo, keys: ApiKeys): Flow<SourceResult> = channelFlow {
        ALL_SOURCES.forEach { source ->
            launch {
                val result = try {
                    source.run(info, keys)
                } catch (e: Exception) {
                    SourceResult(
                        id = source.id,
                        status = SourceStatus.ERROR,
                        message = e.message?.take(140) ?: "Unexpected failure."
                    )
                }
                send(result)
            }
        }
    }
}
