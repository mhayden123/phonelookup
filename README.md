# PhoneLookup

An Android port of [SearchPhone by HackUnderway](https://github.com/HackUnderway/SearchPhone) —
the phone-number OSINT tool — rebuilt as a native app you can run from your phone
instead of a terminal.

## What it does

Type a number, pick a country, and the app runs every source at once, filling in
results as each one answers.

**Always works, no setup, no network:**

- Validity and "possible number" checks
- E.164 / international / national / RFC3966 formats
- Country, region and location
- Carrier
- Line type (mobile, fixed line, VoIP, toll free, …)
- Time zones

This comes from Google's libphonenumber, bundled into the app.

**Free sources, no key:**

- **Hudson Rock** — whether the number shows up in infostealer malware logs
- **DuckDuckGo** — instant answers and related topics
- **Reddit** — public posts mentioning the number

**Needs a free API key** (Settings → paste key):

| Source | What it adds | Free tier |
|---|---|---|
| [Numverify](https://numverify.com/product) | Carrier, line type, location | 100 lookups/month |
| [SerpAPI](https://serpapi.com/users/sign_up) | Google results mentioning the number | 250 searches/month |
| [GitHub](https://github.com/settings/tokens) | Code containing the number | 5,000 requests/hour |

Sources without a key are skipped and say so — the app still works without any of them.

**Also:** share a JSON report out to any app, tap a result to open it, call/SMS/copy
the number, and a recent-lookups list you can tap to re-run.

## Installing it

Grab `phonelookup.apk` from the **Build APK** workflow's artifacts
([Actions tab](../../actions)), copy it to your phone, and open it. Android will ask
you to allow installing from this source — that is expected for an app that does not
come from the Play Store.

To publish it as a release you can download directly on your phone, run the
**Build APK** workflow manually from the Actions tab with **publish_release**
checked. Note this repo is public, so that release is publicly downloadable.

## Building it yourself

```bash
./gradlew assembleDebug
# app/build/outputs/apk/debug/app-debug.apk
```

Requires JDK 17 and the Android SDK (platform 35). Opening the folder in Android
Studio works too.

The APK is signed with the checked-in debug key in `keystore/`, so every build
installs over the previous one. It uses the standard, publicly known Android debug
credentials — it is not a secret, and it is not for the Play Store.

## Notes

- API keys live in the app's private storage and are only ever sent to the service
  they belong to.
- Numverify's free plan refuses HTTPS, so that one host — and only that host — is
  allowed to use plain HTTP (see `network_security_config.xml`).
- Look up numbers you have a legitimate reason to investigate, and follow the laws
  that apply where you are.

Credit for the original tool and its source list goes to
[HackUnderway](https://github.com/HackUnderway/SearchPhone).
