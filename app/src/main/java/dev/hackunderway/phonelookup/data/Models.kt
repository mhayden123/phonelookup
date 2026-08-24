package dev.hackunderway.phonelookup.data

/** A single label/value pair shown in a result card. */
data class Fact(val label: String, val value: String)

/** A single hit from a search source. */
data class Finding(
    val title: String,
    val subtitle: String? = null,
    val snippet: String? = null,
    val url: String? = null
)

enum class SourceStatus { LOADING, OK, EMPTY, NEEDS_KEY, ERROR }

enum class SourceId(val label: String, val keyName: String?) {
    NUMVERIFY("Numverify", "Numverify key"),
    HUDSON_ROCK("Hudson Rock", null),
    GOOGLE("Google (SerpAPI)", "SerpAPI key"),
    DUCKDUCKGO("DuckDuckGo", null),
    REDDIT("Reddit", null),
    GITHUB("GitHub code", "GitHub token")
}

data class SourceResult(
    val id: SourceId,
    val status: SourceStatus,
    val facts: List<Fact> = emptyList(),
    val findings: List<Finding> = emptyList(),
    val message: String? = null
) {
    /** Number of concrete hits, used for the summary line. */
    val hitCount: Int get() = findings.size
}

/** Everything libphonenumber can tell us offline. */
data class PhoneInfo(
    val input: String,
    val region: String,
    val valid: Boolean,
    val possible: Boolean,
    val e164: String? = null,
    val international: String? = null,
    val national: String? = null,
    val rfc3966: String? = null,
    val countryCode: Int? = null,
    val nationalNumber: String? = null,
    val regionCode: String? = null,
    val location: String? = null,
    val carrier: String? = null,
    val lineType: String? = null,
    val timezones: List<String> = emptyList(),
    val error: String? = null
) {
    /** Best available dialable form. */
    val dialable: String get() = e164 ?: input
}

data class LookupReport(
    val phoneInfo: PhoneInfo,
    val sources: List<SourceResult>,
    val timestampMillis: Long
) {
    val totalFindings: Int get() = sources.sumOf { it.hitCount }
}
