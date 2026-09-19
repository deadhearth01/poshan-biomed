package com.teamposhan.biomed.ui.haptics

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Semantic haptics. Uses rich VibrationEffect primitives on API 31+ when the device
 * supports them, and gracefully falls back to predefined effects / short vibration.
 *
 * Paired with Material 3 Expressive motion so every tap, selection and confirmation has
 * a physical counterpart.
 */
class AppHaptics(context: Context) {

    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        manager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    /** Light tick — tab changes, toggles, list scrolling. */
    fun tick() = predefined(VibrationEffect.EFFECT_TICK)

    /** Crisp click — selecting a response card. */
    fun click() = predefined(VibrationEffect.EFFECT_CLICK)

    /** Positive confirmation — answer saved, category completed. */
    fun confirm() = predefined(VibrationEffect.EFFECT_DOUBLE_CLICK)

    /** Warning / negative feedback — red flag surfaced. */
    fun reject() = predefined(VibrationEffect.EFFECT_HEAVY_CLICK)

    /** Celebratory success — assessment submitted. */
    fun success() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val supported = vibrator.arePrimitivesSupported(
                VibrationEffect.Composition.PRIMITIVE_SLOW_RISE,
                VibrationEffect.Composition.PRIMITIVE_CLICK,
            )
            if (supported.all { it }) {
                vibrator.vibrate(
                    VibrationEffect.startComposition()
                        .addPrimitive(VibrationEffect.Composition.PRIMITIVE_SLOW_RISE, 1f)
                        .addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 1f, 40)
                        .compose()
                )
                return
            }
        }
        predefined(VibrationEffect.EFFECT_CLICK)
    }

    // ------------------------------------------------------------------
    // Dynamic haptics
    //
    // A fixed buzz on every tap becomes noise, and noise gets ignored. These vary the
    // feedback with what is actually happening, so the hand learns the difference between
    // "recorded" and "that is the last one" without the eye having to check.
    // ------------------------------------------------------------------

    /** True when the device can compose primitives rather than only replay canned effects. */
    private val supportsComposition: Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && vibrator.hasVibrator()

    private fun canCompose(vararg primitives: Int): Boolean =
        supportsComposition && runCatching {
            vibrator.arePrimitivesSupported(*primitives).all { it }
        }.getOrDefault(false)

    /**
     * Selection feedback whose intensity rises with progress through a sequence.
     *
     * Answering question 2 of 14 should not feel identical to answering 13 of 14. The
     * amplitude climbs from a light tick to a firm one, so a caregiver can feel the end of a
     * category approaching.
     */
    fun progressTick(progress: Float) {
        val p = progress.coerceIn(0f, 1f)
        if (canCompose(VibrationEffect.Composition.PRIMITIVE_TICK)) {
            val scale = 0.35f + 0.5f * p
            runCatching {
                vibrator.vibrate(
                    VibrationEffect.startComposition()
                        .addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, scale)
                        .compose()
                )
            }
            return
        }
        tick()
    }

    /**
     * Confirmation weighted by how significant the answer was.
     *
     * A routine "yes" gets a single light click; an answer that raised a flag gets a
     * heavier double pulse. The caregiver feels that something mattered before they read
     * anything.
     */
    fun answerRecorded(flagged: Boolean) {
        if (canCompose(
                VibrationEffect.Composition.PRIMITIVE_CLICK,
                VibrationEffect.Composition.PRIMITIVE_TICK,
            )
        ) {
            val composition = VibrationEffect.startComposition()
            if (flagged) {
                composition
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.7f)
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.45f, 90)
            } else {
                composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.5f)
            }
            runCatching { vibrator.vibrate(composition.compose()) }
            return
        }
        if (flagged) reject() else click()
    }

    /**
     * Feedback scaled to a clinical severity band, 0 = normal to 3 = severe.
     *
     * Used when a result appears. Reading a z-score takes a moment; feeling that this one is
     * different does not.
     */
    fun severity(band: Int) {
        val level = band.coerceIn(0, 3)
        if (level == 0) {
            tick()
            return
        }
        if (canCompose(VibrationEffect.Composition.PRIMITIVE_CLICK)) {
            val composition = VibrationEffect.startComposition()
            repeat(level) { i ->
                composition.addPrimitive(
                    VibrationEffect.Composition.PRIMITIVE_CLICK,
                    0.4f + 0.2f * level,
                    if (i == 0) 0 else 80,
                )
            }
            runCatching { vibrator.vibrate(composition.compose()) }
            return
        }
        if (level >= 2) reject() else confirm()
    }

    /**
     * A rising swell, for the moment a long action completes — a session finishing, a sync
     * succeeding. Distinct from [success] in that it scales with how much was accomplished.
     */
    fun completion(fraction: Float) {
        val f = fraction.coerceIn(0f, 1f)
        if (canCompose(
                VibrationEffect.Composition.PRIMITIVE_SLOW_RISE,
                VibrationEffect.Composition.PRIMITIVE_CLICK,
            )
        ) {
            runCatching {
                vibrator.vibrate(
                    VibrationEffect.startComposition()
                        .addPrimitive(VibrationEffect.Composition.PRIMITIVE_SLOW_RISE, 0.4f + 0.6f * f)
                        .addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.6f + 0.4f * f, 40)
                        .compose()
                )
            }
            return
        }
        success()
    }

    /**
     * A soft downward pulse for a dismissal or a step backwards — quieter than a selection,
     * so going back never feels like a mistake.
     */
    fun back() {
        if (canCompose(VibrationEffect.Composition.PRIMITIVE_LOW_TICK)) {
            runCatching {
                vibrator.vibrate(
                    VibrationEffect.startComposition()
                        .addPrimitive(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, 0.5f)
                        .compose()
                )
            }
            return
        }
        tick()
    }

    private fun predefined(effectId: Int) {
        if (!vibrator.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            runCatching { vibrator.vibrate(VibrationEffect.createPredefined(effectId)) }
        } else {
            @Suppress("DEPRECATION")
            runCatching { vibrator.vibrate(30) }
        }
    }
}

@Composable
fun rememberAppHaptics(): AppHaptics {
    val context = LocalContext.current
    return remember(context) { AppHaptics(context) }
}
