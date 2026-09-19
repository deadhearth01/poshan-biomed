package com.teamposhan.biomed.domain.growth

import com.teamposhan.biomed.data.model.GrowthMeasurement
import com.teamposhan.biomed.data.model.Sex
import com.teamposhan.biomed.domain.schedule.AssessmentSchedule
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * A fabricated growth trajectory, for checking that the charts plot correctly without
 * having to enter a year of real visits by hand.
 *
 * Deliberately *not* a flat healthy child: the sample drifts downward across the periodic
 * visits so the plot exercises every band the report can render — the normal range, the
 * amber -2 SD crossing, the red -3 SD zone, the faltering trend note and the MUAC cut-offs.
 *
 * Values are derived from the real WHO LMS tables by picking a z for each visit and
 * inverting the curve, so a sampled point sits exactly where its z-score says it should.
 * Nothing here is ever written to the store — it exists only while the report's
 * "Sample data" switch is on.
 */
object SampleGrowthData {

    /** Where the trajectory starts and ends, per indicator. */
    private fun zPath(metric: GrowthStandards.Metric): Pair<Float, Float> = when (metric) {
        // Born on the median, falls into moderate underweight — the classic faltering shape.
        GrowthStandards.Metric.WEIGHT_FOR_AGE -> 0.3f to -2.4f
        // Stunting accumulates slowly.
        GrowthStandards.Metric.LENGTH_FOR_AGE -> -0.2f to -2.1f
        // Wasting: milder, so the BMI chart sits in the amber band rather than the red.
        GrowthStandards.Metric.BMI_FOR_AGE -> 0.1f to -1.7f
        // Ends just inside MAM by MUAC.
        GrowthStandards.Metric.MUAC_FOR_AGE -> 0.0f to -1.9f
    }

    /** The visit ages a child of this age would have been seen at, plus today. */
    fun visitMonths(ageMonths: Float): List<Float> {
        val scheduled = AssessmentSchedule.visits
            .map { it.first.toFloat() }
            .filter { it <= ageMonths }
        return (scheduled + ageMonths).distinct().sorted()
    }

    /**
     * The sample trajectory for one indicator.
     *
     * @param seed keeps the wobble stable for a given child, so the chart does not
     *   reshuffle itself on every recomposition.
     */
    fun history(
        metric: GrowthStandards.Metric,
        sex: Sex,
        ageMonths: Float,
        seed: Int = 0,
    ): List<Pair<Float, Float>> {
        val months = visitMonths(ageMonths).filter { it >= GrowthStandards.firstMonth(metric) }
        if (months.isEmpty()) return emptyList()

        val (startZ, endZ) = zPath(metric)
        val random = Random(seed * 31 + metric.ordinal)
        val span = (months.last() - months.first()).coerceAtLeast(1f)

        return months.map { month ->
            val t = (month - months.first()) / span
            // Measurement noise, so the line looks measured rather than computed.
            val wobble = (random.nextFloat() - 0.5f) * 0.18f
            val z = startZ + (endZ - startZ) * t + wobble
            val row = GrowthStandards.lmsAt(metric, sex, month)
            month to GrowthStandards.valueAt(row, z)
        }
    }

    /**
     * The latest sampled visit as a [GrowthMeasurement], so the findings, z-scores and
     * MUAC banding in the report read from the same numbers the charts are plotting.
     */
    fun latestMeasurement(sex: Sex, ageMonths: Float, seed: Int = 0): GrowthMeasurement {
        val weight = history(GrowthStandards.Metric.WEIGHT_FOR_AGE, sex, ageMonths, seed).lastOrNull()?.second
        val height = history(GrowthStandards.Metric.LENGTH_FOR_AGE, sex, ageMonths, seed).lastOrNull()?.second
        val muacCm = history(GrowthStandards.Metric.MUAC_FOR_AGE, sex, ageMonths, seed).lastOrNull()?.second
        return GrowthMeasurement(
            weightKg = weight?.let { (it * 10f).roundToInt() / 10f },
            heightCm = height?.let { (it * 10f).roundToInt() / 10f },
            muacMm = muacCm?.let { (it * 10f).roundToInt().toFloat() },
        )
    }

    /** A demo age for a child with no recorded birth date — old enough to show a full run. */
    const val FALLBACK_AGE_MONTHS = 18f
}
