package com.teamposhan.biomed.ui.components

import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

/**
 * Reads a screen's content aloud.
 *
 * An accessibility feature with a specific user in mind rather than a generic screen reader:
 * many caregivers using this app read slowly or not at all, and several screens — consent,
 * instructions, the report — carry information they are expected to act on. TalkBack exists,
 * but it requires the user to know it exists and to have turned it on.
 *
 * ## Making it not sound like a machine
 *
 * The robotic quality of Android TTS is usually not the synthesiser — it is three defaults
 * that nobody changes:
 *
 *  1. **The wrong locale.** This previously asked for `Locale.UK`, so Indian users heard a
 *     British voice reading Indian names and place words. `en-IN` exists on essentially every
 *     device with Google TTS installed and is a different voice entirely.
 *  2. **The wrong voice within the locale.** A locale usually has several voices of
 *     differing quality, and the engine's default is often the smallest embedded one.
 *     [pickVoice] asks for the highest-quality voice that does **not** need a network, which
 *     matters twice over here: it sounds markedly better, and it still works in a village
 *     with no signal.
 *  3. **One long breath.** Handing the engine an entire page produces a flat, unbroken
 *     stream. Splitting on sentences and queueing a short silence between them gives the
 *     phrasing a person expects.
 *
 * A neural Indic voice — Piper or an AI4Bharat model through ONNX Runtime — would sound
 * better still, at the cost of roughly 20-60 MB of voice data per language plus the runtime,
 * and a phonemiser for Indic scripts. That is a real option but not a small one, and it is
 * not needed to fix what actually sounds wrong today.
 */
class ReadAloudController(
    private val engine: TextToSpeech?,
    val available: Boolean,
    private val onDone: () -> Unit = {},
) {
    var speaking: Boolean = false
        private set

    /**
     * Speaks [text], one sentence at a time with a short gap between them.
     *
     * The gaps are what carry the phrasing. Without them a paragraph arrives as a single
     * run-on utterance, which is most of the "computer voice" impression.
     */
    fun speak(text: String) {
        val tts = engine ?: return
        if (text.isBlank()) return
        tts.stop()

        val sentences = splitSentences(text)
        sentences.forEachIndexed { i, sentence ->
            val mode = if (i == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
            tts.speak(sentence, mode, null, "$UTTERANCE_ID-$i")
            if (i < sentences.lastIndex) {
                tts.playSilentUtterance(PAUSE_MS, TextToSpeech.QUEUE_ADD, "$UTTERANCE_ID-gap-$i")
            }
        }
        speaking = true
    }

    fun stop() {
        engine?.stop()
        speaking = false
    }

    internal fun markStopped() {
        speaking = false
        onDone()
    }

    private companion object {
        const val UTTERANCE_ID = "poshan-read-aloud"

        /** Long enough to hear as a full stop, short enough not to sound like a fault. */
        const val PAUSE_MS = 260L

        /**
         * Sentence split that keeps the terminator.
         *
         * Deliberately crude: abbreviations and decimals will occasionally split early. That
         * costs a small extra pause, whereas a clever splitter that runs two sentences
         * together costs the phrasing this exists to produce.
         */
        fun splitSentences(text: String): List<String> =
            text.split(Regex("(?<=[.!?।])\\s+"))
                .map { it.trim() }
                .filter { it.isNotEmpty() }
    }
}

val LocalReadAloud = compositionLocalOf<ReadAloudController?> { null }

/**
 * The best offline voice for [locale], or null to leave the engine's default alone.
 *
 * Network voices are excluded outright. This app is used where there is no signal, and a
 * voice that silently fails offline is worse than a plainer one that always works.
 */
private fun pickVoice(tts: TextToSpeech, locale: Locale): Voice? = runCatching {
    tts.voices
        ?.filter { voice ->
            voice.locale.language == locale.language &&
                (voice.locale.country.isEmpty() || voice.locale.country == locale.country) &&
                !voice.isNetworkConnectionRequired &&
                !voice.features.orEmpty().contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED)
        }
        // Higher quality first; among equals, prefer an exact country match.
        ?.maxWithOrNull(
            compareBy<Voice> { it.quality }
                .thenBy { if (it.locale.country == locale.country) 1 else 0 }
        )
}.getOrNull()

/**
 * Creates a TTS engine bound to the composition's lifetime and the active locale.
 *
 * Re-created when the language changes: a single engine whose locale is mutated mid-session
 * tends to keep the old voice on some devices.
 */
@Composable
fun rememberReadAloud(): ReadAloudController {
    val context = LocalContext.current
    val locales = LocalConfiguration.current.locales
    val language = if (locales.isEmpty) "en" else locales[0].language

    var controller by remember(language) {
        mutableStateOf(ReadAloudController(engine = null, available = false))
    }

    DisposableEffect(language) {
        var engine: TextToSpeech? = null
        engine = TextToSpeech(context) { status ->
            val tts = engine
            if (status == TextToSpeech.SUCCESS && tts != null) {
                val locale = when (language) {
                    "hi" -> Locale("hi", "IN")
                    "te" -> Locale("te", "IN")
                    // Indian English, not British. The previous Locale.UK gave every user
                    // in the field a voice from the wrong country.
                    else -> Locale("en", "IN")
                }
                var result = tts.setLanguage(locale)
                if (result == TextToSpeech.LANG_MISSING_DATA ||
                    result == TextToSpeech.LANG_NOT_SUPPORTED
                ) {
                    // Fall back within the language before giving up: a device with generic
                    // English but no en-IN should still speak.
                    result = tts.setLanguage(Locale(locale.language))
                }
                val ok = result != TextToSpeech.LANG_MISSING_DATA &&
                    result != TextToSpeech.LANG_NOT_SUPPORTED

                if (ok) {
                    pickVoice(tts, locale)?.let { runCatching { tts.voice = it } }
                    // Slightly slower and very slightly lower than default: instructional
                    // content, read by someone following along with the screen.
                    tts.setSpeechRate(0.92f)
                    tts.setPitch(0.96f)
                }

                controller = ReadAloudController(engine = tts, available = ok)

                tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) = Unit
                    override fun onDone(utteranceId: String?) {
                        // Only the queue's tail ends the session; the per-sentence
                        // utterances in between must not reset the button.
                        if (utteranceId?.contains("-gap-") == false) controller.markStopped()
                    }

                    @Deprecated("Required by the platform interface")
                    override fun onError(utteranceId: String?) {
                        controller.markStopped()
                    }
                })
            }
        }
        onDispose {
            engine?.stop()
            engine?.shutdown()
        }
    }

    return controller
}

/**
 * Speaker button for an app bar.
 *
 * [textProvider] is a lambda rather than a string so the caller can assemble the page's
 * spoken form lazily — reading a whole screen aloud means composing text that does not exist
 * as a single value anywhere on it.
 */
@Composable
fun ReadAloudAction(
    textProvider: () -> String,
    modifier: Modifier = Modifier,
) {
    val controller = LocalReadAloud.current ?: rememberReadAloud()
    var speaking by remember { mutableStateOf(false) }
    val haptics = com.teamposhan.biomed.ui.haptics.rememberAppHaptics()

    // Stop speaking when the screen goes away; otherwise the voice follows the user.
    DisposableEffect(Unit) {
        onDispose { controller.stop() }
    }

    IconButton(
        onClick = {
            haptics.tick()
            if (speaking) {
                controller.stop()
                speaking = false
            } else {
                controller.speak(textProvider())
                speaking = true
            }
        },
        modifier = modifier,
        enabled = controller.available,
    ) {
        Icon(
            if (speaking) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
            contentDescription = if (speaking) "Stop reading" else "Read this page aloud",
            tint = if (controller.available) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.outlineVariant
            },
        )
    }
}
