package dev.hackunderway.phonelookup.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

object Http {

    const val USER_AGENT = "PhoneLookup/1.0 (Android; OSINT lookup)"

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .callTimeout(35, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    data class Result(val code: Int, val body: String)

    suspend fun get(url: HttpUrl, headers: Map<String, String> = emptyMap()): Result =
        withContext(Dispatchers.IO) {
            val builder = Request.Builder().url(url).header("User-Agent", USER_AGENT)
            headers.forEach { (name, value) -> builder.header(name, value) }
            client.newCall(builder.build()).execute().use { response ->
                Result(response.code, response.body?.string().orEmpty())
            }
        }
}

/** Keys the user entered in Settings; blank means "source disabled". */
data class ApiKeys(
    val numverify: String = "",
    val serpApi: String = "",
    val github: String = ""
)

/**
 * The same number written the different ways it might appear in a web page,
 * used both to build search queries and to filter out false-positive hits.
 */
fun PhoneInfo.searchVariants(): List<String> {
    val digits = (e164 ?: input).filter { it.isDigit() }
    return listOfNotNull(
        input,
        e164,
        international,
        national,
        digits,
        digits.takeIf { it.isNotEmpty() }?.let { "+$it" },
        international?.replace(" ", ""),
        national?.filter { it.isDigit() }
    ).map { it.trim() }.filter { it.isNotBlank() }.distinct()
}
