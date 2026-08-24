package dev.hackunderway.phonelookup.data

import android.content.Context
import androidx.core.content.edit

/**
 * API keys the user supplies. Stored in app-private storage: other apps
 * cannot read it, and nothing is ever sent anywhere except to the API the
 * key belongs to.
 */
class Settings(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("phonelookup_settings", Context.MODE_PRIVATE)

    var numverifyKey: String
        get() = prefs.getString(KEY_NUMVERIFY, "").orEmpty()
        set(value) = prefs.edit { putString(KEY_NUMVERIFY, value.trim()) }

    var serpApiKey: String
        get() = prefs.getString(KEY_SERPAPI, "").orEmpty()
        set(value) = prefs.edit { putString(KEY_SERPAPI, value.trim()) }

    var githubToken: String
        get() = prefs.getString(KEY_GITHUB, "").orEmpty()
        set(value) = prefs.edit { putString(KEY_GITHUB, value.trim()) }

    var defaultRegion: String
        get() = prefs.getString(KEY_REGION, "").orEmpty()
        set(value) = prefs.edit { putString(KEY_REGION, value.trim().uppercase()) }

    private companion object {
        const val KEY_NUMVERIFY = "numverify_key"
        const val KEY_SERPAPI = "serpapi_key"
        const val KEY_GITHUB = "github_token"
        const val KEY_REGION = "default_region"
    }
}
