package com.teamposhan.biomed.domain.growth

import com.teamposhan.biomed.data.model.Sex

/**
 * Decides whether the arm tape needs to come out.
 *
 * ## Why gate it at all
 *
 * Measuring mid-upper arm circumference well takes time and a second pair of hands, and it
 * is the measurement most often done badly — arm held up, tape pulled tight, wrong midpoint.
 * Asking for it on every child at every visit spends that effort mostly on children whose
 * growth is plainly fine, and the rushed readings it produces are worse than none.
 *
 * So it is prompted when weight-for-age or length/height-for-age has already fallen below
 * −2 SD, which is where a wasting check earns its keep.
 *
 * ## The caveat, stated because it matters
 *
 * MUAC is **not** merely a confirmation of a low weight-for-age. It is an independent
 * criterion for severe acute malnutrition, and a child can be below 115 mm with an
 * acceptable weight-for-age — a tall, wasted child especially, where weight-for-age is
 * propped up by height. Gating on WFA/LFA therefore trades some sensitivity for a large
 * reduction in effort.
 *
 * That trade is why [Decision.required] is a prompt and not a lock: the field stays
 * available on every eligible child, and a worker who wants to record it always can. What
 * changes is whether the app asks.
 */
object MuacTrigger {

    /** WHO age window for MUAC-based screening. */
    const val MIN_AGE_MONTHS = 6f
    const val MAX_AGE_MONTHS = 59f

    /** Z-score at or below which the arm measurement is prompted. */
    const val TRIGGER_Z = -2f

    sealed interface Decision {
        /** Outside the 6–59 month window; MUAC is not interpretable. */
        data object OutOfAgeRange : Decision

        /** In range, but weight and height are not both entered yet. */
        data object NeedsWeightAndHeight : Decision

        /** Growth is within range, so the arm tape is optional. */
        data class NotNeeded(val worstZ: Float) : Decision

        /** Growth is below −2 SD; the arm measurement is prompted. */
        data class Required(
            val worstZ: Float,
            /** Which measurement triggered it, for the prompt to name. */
            val trigger: String,
        ) : Decision
    }

    fun evaluate(
        sex: Sex,
        ageMonths: Float,
        weightKg: Float?,
        heightCm: Float?,
    ): Decision {
        if (ageMonths < MIN_AGE_MONTHS || ageMonths > MAX_AGE_MONTHS) {
            return Decision.OutOfAgeRange
        }

        val wfa = weightKg?.takeIf { it > 0f }?.let {
            runCatching {
                GrowthStandards.zFor(GrowthStandards.Metric.WEIGHT_FOR_AGE, sex, ageMonths, it)
            }.getOrNull()
        }
        val lfa = heightCm?.takeIf { it > 0f }?.let {
            runCatching {
                GrowthStandards.zFor(GrowthStandards.Metric.LENGTH_FOR_AGE, sex, ageMonths, it)
            }.getOrNull()
        }

        // Either one alone is enough to trigger, so a single entered measurement below the
        // line does not wait for the other before prompting.
        if (wfa == null && lfa == null) return Decision.NeedsWeightAndHeight

        val worst = listOfNotNull(wfa, lfa).min()
        return if (worst <= TRIGGER_Z) {
            Decision.Required(
                worstZ = worst,
                trigger = if (wfa != null && wfa <= TRIGGER_Z) "weight for age" else "height for age",
            )
        } else {
            // Only call it clear once both are in and both are fine; one good measurement
            // says nothing about the other.
            if (wfa == null || lfa == null) Decision.NeedsWeightAndHeight
            else Decision.NotNeeded(worst)
        }
    }
}
