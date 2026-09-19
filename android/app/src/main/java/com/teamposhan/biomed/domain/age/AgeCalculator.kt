package com.teamposhan.biomed.domain.age

import com.teamposhan.biomed.data.model.ChildProfile
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Age handling for screening.
 *
 * Chronological age drives everything. For children born prematurely we also expose a
 * corrected age (age adjusted for the number of weeks early) which is the correct basis for
 * developmental screening under 24 months.
 */
object AgeCalculator {

    /** Reference gestation the correction is measured against. */
    const val FULL_TERM_WEEKS = 40

    /**
     * Below this gestation a child counts as preterm and the correction applies.
     *
     * Distinct from [FULL_TERM_WEEKS], which is only the arithmetic reference. The gate was
     * previously 40, which meant a baby born at 38 or 39 weeks — clinically term — had two
     * weeks subtracted from their age and could be scored against an easier question set.
     * 37 completed weeks is the standard definition of term.
     */
    const val PRETERM_THRESHOLD_WEEKS = 37

    /** Above this, correction is no longer applied regardless of prematurity. */
    const val CORRECTION_CEILING_MONTHS = 24f

    fun chronologicalMonths(dob: LocalDate, at: LocalDate = LocalDate.now()): Float {
        val days = ChronoUnit.DAYS.between(dob, at).toFloat()
        return days / 30.4375f
    }

    fun chronologicalMonths(child: ChildProfile, at: LocalDate = LocalDate.now()): Float =
        chronologicalMonths(LocalDate.ofEpochDay(child.dateOfBirthEpochDay), at)

    /**
     * Corrected age = chronological age − weeks of prematurity.
     * Only applied when the child was born before 37 weeks and is under 24 months.
     */
    fun correctedMonths(child: ChildProfile, at: LocalDate = LocalDate.now()): Float? {
        if (!child.bornPremature) return null
        val ga = child.gestationalAgeWeeks ?: return null
        if (ga >= PRETERM_THRESHOLD_WEEKS) return null
        val chrono = chronologicalMonths(child, at)
        if (chrono >= CORRECTION_CEILING_MONTHS) return null
        val weeksEarly = (FULL_TERM_WEEKS - ga).toFloat()
        val corrected = chrono - (weeksEarly * 7f / 30.4375f)
        return corrected.coerceAtLeast(0f)
    }

    /**
     * Why the correction is or is not being applied, for the child form to show.
     *
     * The prematurity switch previously looked broken because it *is* inert on its own: with
     * no gestational age entered there is nothing to subtract, so toggling it changed
     * nothing and said nothing. This returns the reason so the form can explain itself
     * instead of silently ignoring the input.
     */
    sealed interface CorrectionState {
        /** Not premature, or the switch is off. */
        data object NotApplicable : CorrectionState

        /** Switch on but no gestational age entered — the switch alone does nothing. */
        data object NeedsGestationalAge : CorrectionState

        /** Entered gestation is term; no correction is clinically warranted. */
        data class TermGestation(val weeks: Int) : CorrectionState

        /** Past the age at which correction stops being applied. */
        data object PastCeiling : CorrectionState

        /** Correction is active. */
        data class Applied(
            val weeksEarly: Int,
            val chronologicalMonths: Float,
            val correctedMonths: Float,
        ) : CorrectionState
    }

    fun correctionState(
        bornPremature: Boolean,
        gestationalAgeWeeks: Int?,
        dob: LocalDate,
        at: LocalDate = LocalDate.now(),
    ): CorrectionState {
        if (!bornPremature) return CorrectionState.NotApplicable
        val ga = gestationalAgeWeeks ?: return CorrectionState.NeedsGestationalAge
        if (ga >= PRETERM_THRESHOLD_WEEKS) return CorrectionState.TermGestation(ga)
        val chrono = chronologicalMonths(dob, at)
        if (chrono >= CORRECTION_CEILING_MONTHS) return CorrectionState.PastCeiling
        val weeksEarly = FULL_TERM_WEEKS - ga
        return CorrectionState.Applied(
            weeksEarly = weeksEarly,
            chronologicalMonths = chrono,
            correctedMonths = (chrono - weeksEarly * 7f / 30.4375f).coerceAtLeast(0f),
        )
    }

    /** Age that should be used to select the question set. */
    fun assessmentMonths(child: ChildProfile, at: LocalDate = LocalDate.now()): Float =
        correctedMonths(child, at) ?: chronologicalMonths(child, at)

    fun format(months: Float): String {
        if (months < 0) return "—"
        val whole = months.toInt()
        return if (whole < 24) {
            "$whole mo"
        } else {
            val years = whole / 12
            val rem = whole % 12
            if (rem == 0) "$years yr" else "$years yr $rem mo"
        }
    }
}
