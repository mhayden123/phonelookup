package dev.hackunderway.phonelookup.data.sources

import dev.hackunderway.phonelookup.data.ApiKeys
import dev.hackunderway.phonelookup.data.Fact
import dev.hackunderway.phonelookup.data.Finding
import dev.hackunderway.phonelookup.data.Http
import dev.hackunderway.phonelookup.data.PhoneInfo
import dev.hackunderway.phonelookup.data.SourceId
import dev.hackunderway.phonelookup.data.SourceResult
import dev.hackunderway.phonelookup.data.SourceStatus
import dev.hackunderway.phonelookup.data.searchVariants
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** One OSINT lookup source. */
interface OsintSource {
    val id: SourceId
    suspend fun run(info: PhoneInfo, keys: ApiKeys): SourceResult
}

internal fun SourceId.needsKey() =
    SourceResult(this, SourceStatus.NEEDS_KEY, message = "Add your $keyName in Settings to enable this source.")

internal fun SourceId.empty(message: String = "No results.") =
    SourceResult(this, SourceStatus.EMPTY, message = message)

internal fun SourceId.error(message: String) =
    SourceResult(this, SourceStatus.ERROR, message = message)

internal fun SourceId.httpError(code: Int) = when (code) {
    401, 403 -> error("Access denied (HTTP $code). Check the key, or the service is blocking this request.")
    429 -> error("Rate limited (HTTP 429). Try again later.")
    else -> error("Request failed (HTTP $code).")
}

/** Carrier / line-type lookup. Free tier: 100 requests a month. */
object NumverifySource : OsintSource {
    override val id = SourceId.NUMVERIFY

    override suspend fun run(info: PhoneInfo, keys: ApiKeys): SourceResult {
        if (keys.numverify.isBlank()) return id.needsKey()

        fun url(scheme: String): HttpUrl = "$scheme://apilayer.net/api/validate".toHttpUrl()
            .newBuilder()
            .addQueryParameter("access_key", keys.numverify)
            .addQueryParameter("number", info.dialable)
            .addQueryParameter("country_code", info.region)
            .build()

        return try {
            var response = Http.get(url("https"))
            var json = JSONObject(response.body)

            // Numverify's free plan is HTTP-only; retry in the clear when it says so.
            if (json.optJSONObject("error")?.optInt("code") == 105) {
                response = Http.get(url("http"))
                json = JSONObject(response.body)
            }

            if (response.code != 200) return id.httpError(response.code)

            json.optJSONObject("error")?.let { err ->
                return id.error(err.optString("info").ifBlank { "Numverify error ${err.optInt("code")}." })
            }
            if (!json.optBoolean("valid", false)) {
                return id.empty("Numverify considers this number invalid.")
            }

            val facts = listOfNotNull(
                json.optString("country_name").ifBlank { null }?.let { Fact("Country", it) },
                json.optString("location").ifBlank { null }?.let { Fact("Location", it) },
                json.optString("carrier").ifBlank { null }?.let { Fact("Carrier", it) },
                json.optString("line_type").ifBlank { null }?.let { Fact("Line type", it) }
            )
            if (facts.isEmpty()) id.empty("Valid, but no extra detail returned.")
            else SourceResult(id, SourceStatus.OK, facts = facts)
        } catch (e: Exception) {
            id.error(e.friendly())
        }
    }
}

/** Infostealer-breach intelligence. Free, no key. */
object HudsonRockSource : OsintSource {
    override val id = SourceId.HUDSON_ROCK

    override suspend fun run(info: PhoneInfo, keys: ApiKeys): SourceResult {
        val number = info.e164 ?: run {
            val digits = info.input.filter { it.isDigit() }
            if (digits.isBlank()) return id.empty("No usable number.") else "+$digits"
        }

        val url = "https://cavalier.hudsonrock.com/api/json/v2/osint-tools/search-by-username".toHttpUrl()
            .newBuilder()
            .addQueryParameter("username", number)
            .build()

        return try {
            val response = Http.get(url)
            if (response.code == 404) return id.empty("Not found in the infostealer database.")
            if (response.code != 200) return id.httpError(response.code)

            val json = JSONObject(response.body)
            val stealers = json.optJSONArray("stealers")
            if (stealers == null || stealers.length() == 0) {
                return id.empty(json.optString("message").ifBlank { "No infostealer records found." })
            }

            val findings = (0 until stealers.length()).map { i ->
                val s = stealers.getJSONObject(i)
                val logins = s.optJSONArray("top_logins")
                val loginPreview = if (logins != null && logins.length() > 0) {
                    (0 until minOf(logins.length(), 5)).joinToString(", ") { logins.optString(it) }
                } else null

                Finding(
                    title = s.optString("stealer_family").ifBlank { "Unknown stealer" },
                    subtitle = listOfNotNull(
                        s.optString("date_compromised").take(10).ifBlank { null },
                        s.optString("computer_name").ifBlank { null },
                        s.optString("operating_system").ifBlank { null }
                    ).joinToString(" · ").ifBlank { null },
                    snippet = loginPreview?.let { "Exposed logins: $it" }
                )
            }

            SourceResult(
                id = id,
                status = SourceStatus.OK,
                facts = listOf(
                    Fact("Status", "Compromised"),
                    Fact("Corporate services", json.optInt("total_corporate_services", 0).toString()),
                    Fact("Personal services", json.optInt("total_user_services", 0).toString())
                ),
                findings = findings,
                message = json.optString("message").ifBlank { null }
            )
        } catch (e: Exception) {
            id.error(e.friendly())
        }
    }
}

/** DuckDuckGo Instant Answer API. Free, no key. */
object DuckDuckGoSource : OsintSource {
    override val id = SourceId.DUCKDUCKGO

    override suspend fun run(info: PhoneInfo, keys: ApiKeys): SourceResult {
        val url = "https://api.duckduckgo.com/".toHttpUrl().newBuilder()
            .addQueryParameter("q", "\"${info.dialable}\"")
            .addQueryParameter("format", "json")
            .addQueryParameter("no_html", "1")
            .build()

        return try {
            val response = Http.get(url)
            if (response.code != 200) return id.httpError(response.code)

            val json = JSONObject(response.body)
            val findings = mutableListOf<Finding>()

            val abstract = json.optString("AbstractText")
            if (abstract.isNotBlank()) {
                findings += Finding(
                    title = json.optString("Heading").ifBlank { "Abstract" },
                    snippet = abstract.take(400),
                    url = json.optString("AbstractURL").ifBlank { null }
                )
            }

            json.optJSONArray("RelatedTopics")?.let { topics ->
                for (i in 0 until minOf(topics.length(), 10)) {
                    val topic = topics.optJSONObject(i) ?: continue
                    val text = topic.optString("Text")
                    if (text.isBlank()) continue
                    findings += Finding(
                        title = text.take(120),
                        url = topic.optString("FirstURL").ifBlank { null }
                    )
                }
            }

            if (findings.isEmpty()) id.empty("No instant answer for this number.")
            else SourceResult(id, SourceStatus.OK, findings = findings)
        } catch (e: Exception) {
            id.error(e.friendly())
        }
    }
}

/** Public Reddit search. Free, no key. */
object RedditSource : OsintSource {
    override val id = SourceId.REDDIT

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    override suspend fun run(info: PhoneInfo, keys: ApiKeys): SourceResult {
        val url = "https://www.reddit.com/r/all/search.json".toHttpUrl().newBuilder()
            .addQueryParameter("q", "\"${info.dialable}\"")
            .addQueryParameter("limit", "20")
            .build()

        return try {
            val response = Http.get(url)
            if (response.code != 200) return id.httpError(response.code)

            val children = JSONObject(response.body)
                .optJSONObject("data")
                ?.optJSONArray("children")
                ?: return id.empty()

            val findings = (0 until children.length()).mapNotNull { i ->
                val post = children.optJSONObject(i)?.optJSONObject("data") ?: return@mapNotNull null
                val created = post.optLong("created_utc", 0L)
                Finding(
                    title = post.optString("title").ifBlank { "(untitled post)" },
                    subtitle = listOfNotNull(
                        post.optString("subreddit").ifBlank { null }?.let { "r/$it" },
                        "${post.optInt("score", 0)} points",
                        created.takeIf { it > 0 }?.let { dateFormat.format(Date(it * 1000)) }
                    ).joinToString(" · "),
                    url = post.optString("permalink").ifBlank { null }?.let { "https://reddit.com$it" }
                )
            }

            if (findings.isEmpty()) id.empty() else SourceResult(id, SourceStatus.OK, findings = findings)
        } catch (e: Exception) {
            id.error(e.friendly())
        }
    }
}

/** GitHub code search. Needs a personal access token. */
object GitHubSource : OsintSource {
    override val id = SourceId.GITHUB

    override suspend fun run(info: PhoneInfo, keys: ApiKeys): SourceResult {
        if (keys.github.isBlank()) return id.needsKey()

        val url = "https://api.github.com/search/code".toHttpUrl().newBuilder()
            .addQueryParameter("q", "\"${info.dialable}\"")
            .addQueryParameter("per_page", "20")
            .build()

        return try {
            val response = Http.get(
                url,
                mapOf(
                    "Authorization" to "Bearer ${keys.github}",
                    "Accept" to "application/vnd.github+json",
                    "X-GitHub-Api-Version" to "2022-11-28"
                )
            )
            if (response.code != 200) return id.httpError(response.code)

            val items = JSONObject(response.body).optJSONArray("items") ?: return id.empty()
            val findings = (0 until minOf(items.length(), 10)).mapNotNull { i ->
                val item = items.optJSONObject(i) ?: return@mapNotNull null
                val repo = item.optJSONObject("repository")
                Finding(
                    title = repo?.optString("full_name")?.ifBlank { null } ?: "Unknown repository",
                    subtitle = listOfNotNull(
                        item.optString("path").ifBlank { null },
                        repo?.optString("language")?.ifBlank { null }
                    ).joinToString(" · ").ifBlank { null },
                    url = item.optString("html_url").ifBlank { null }
                )
            }

            if (findings.isEmpty()) id.empty() else SourceResult(id, SourceStatus.OK, findings = findings)
        } catch (e: Exception) {
            id.error(e.friendly())
        }
    }
}

/** Google results through SerpAPI. Needs a key; free tier is 250 searches a month. */
object SerpApiSource : OsintSource {
    override val id = SourceId.GOOGLE

    override suspend fun run(info: PhoneInfo, keys: ApiKeys): SourceResult {
        if (keys.serpApi.isBlank()) return id.needsKey()

        val variants = info.searchVariants()
        val query = variants.take(4).joinToString(" OR ") { "\"$it\"" }

        val url = "https://serpapi.com/search".toHttpUrl().newBuilder()
            .addQueryParameter("q", query)
            .addQueryParameter("api_key", keys.serpApi)
            .addQueryParameter("num", "20")
            .addQueryParameter("hl", "en")
            .build()

        return try {
            val response = Http.get(url)
            val json = runCatching { JSONObject(response.body) }.getOrNull()
            if (response.code != 200) {
                val detail = json?.optString("error")?.ifBlank { null }
                return id.error(detail ?: "Request failed (HTTP ${response.code}).")
            }
            json ?: return id.error("Unexpected response from SerpAPI.")

            val organic = json.optJSONArray("organic_results") ?: return id.empty()
            val findings = (0 until organic.length()).mapNotNull { i ->
                val item = organic.optJSONObject(i) ?: return@mapNotNull null
                val title = item.optString("title")
                val link = item.optString("link")
                val snippet = item.optString("snippet")

                // Keep only results where the number actually appears, as the
                // original tool does — Google alone matches far too loosely.
                val haystack = "$title $link $snippet"
                if (variants.none { haystack.contains(it) }) return@mapNotNull null

                Finding(
                    title = title.ifBlank { link.ifBlank { "(untitled result)" } },
                    snippet = snippet.ifBlank { null },
                    url = link.ifBlank { null }
                )
            }

            if (findings.isEmpty()) id.empty("No result contained the number verbatim.")
            else SourceResult(id, SourceStatus.OK, findings = findings)
        } catch (e: Exception) {
            id.error(e.friendly())
        }
    }
}

internal fun Exception.friendly(): String = when (this) {
    is java.net.UnknownHostException -> "No network connection."
    is java.net.SocketTimeoutException -> "The service timed out."
    is org.json.JSONException -> "Unexpected response format."
    else -> message?.take(140) ?: this::class.java.simpleName
}

val ALL_SOURCES: List<OsintSource> = listOf(
    NumverifySource,
    HudsonRockSource,
    SerpApiSource,
    DuckDuckGoSource,
    RedditSource,
    GitHubSource
)
