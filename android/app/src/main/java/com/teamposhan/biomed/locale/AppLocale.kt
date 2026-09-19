package com.teamposhan.biomed.locale

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

/**
 * Per-app language using the official AndroidX API.
 *
 * `AppCompatDelegate.setApplicationLocales` applies the locale to the whole app,
 * recreates the running activities and — with the `autoStoreLocales` service declared in
 * the manifest — persists the choice on Android 12 and below. On Android 13+ the system
 * stores it and also exposes the choice in Settings → Apps → Poshan → Language.
 */
object AppLocale {

    const val ENGLISH = "en"
    const val HINDI = "hi"
    const val TELUGU = "te"

    val supported: List<Pair<String, String>> = listOf(
        ENGLISH to "English",
        HINDI to "हिन्दी",
        TELUGU to "తెలుగు",
    )

    fun apply(code: String) {
        val tag = code.ifBlank { ENGLISH }
        val current = AppCompatDelegate.getApplicationLocales().toLanguageTags()
        if (current != tag) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
        }
    }

    /** The tag actually in effect, falling back to the system language then English. */
    fun current(): String {
        val app = AppCompatDelegate.getApplicationLocales().toLanguageTags()
        if (app.isNotBlank()) return app.substringBefore(",")
        return ENGLISH
    }
}
