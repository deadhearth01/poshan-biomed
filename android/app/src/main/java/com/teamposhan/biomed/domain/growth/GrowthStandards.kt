package com.teamposhan.biomed.domain.growth

import com.teamposhan.biomed.data.model.GrowthZ
import com.teamposhan.biomed.data.model.Sex
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sqrt

/** One row of a WHO z-score table: month, Box-Cox power, median, coefficient of variation. */
typealias Lms = WhoLms.Lms

/**
 * WHO Child Growth Standards — z-score calculation on the published LMS model.
 *
 * Every indicator is stored as (L, M, S) per month and evaluated with the Box-Cox form
 * the WHO uses:
 *
 *     z = ((value / M)^L - 1) / (L * S)        (L != 0)
 *     z = ln(value / M) / S                    (L == 0)
 *
 * and inverted for charting:
 *
 *     value = M * (1 + L * S * z)^(1/L)        (L != 0)
 *     value = M * exp(S * z)                   (L == 0)
 *
 * `S` is the *coefficient of variation*, not an absolute SD — the two were mixed up in an
 * earlier revision, which made weight z-scores too small by a factor of M and flattened
 * the length SD curves on the chart.
 *
 * Every indicator reads from [WhoLms], which carries the published per-month L/M/S rows
 * transcribed from the WHO z-score tables — weight-for-age, BMI-for-age, length/height-for-age
 * and arm circumference-for-age (MUAC).
 */
object GrowthStandards {


    // ---------------------------------------------------------------------
    // Indicators
    // ---------------------------------------------------------------------

    enum class Metric { WEIGHT_FOR_AGE, LENGTH_FOR_AGE, BMI_FOR_AGE, MUAC_FOR_AGE }

    /**
     * Everything the chart needs to draw an indicator the way the WHO sheet does:
     * axis title, unit, and the y-window per age band (the WHO publishes a separate
     * birth-to-2 and 2-to-5 sheet, with different y-ranges).
     */
    data class MetricInfo(
        val title: String,
        val axisTitle: String,
        val unit: String,
        val decimals: Int,
        /** y-window and gridline step for birth-to-2 years. */
        val youngRange: ClosedFloatingPointRange<Float>,
        val youngStep: Float,
        /** y-window and gridline step for 2-to-5 years. */
        val olderRange: ClosedFloatingPointRange<Float>,
        val olderStep: Float,
        val firstMonth: Float = 0f,
    )

    fun info(metric: Metric, ageMonths: Float = 0f): MetricInfo = when (metric) {
        Metric.WEIGHT_FOR_AGE -> MetricInfo(
            title = "Weight-for-age",
            axisTitle = "Weight (kg)",
            unit = "kg",
            decimals = 1,
            youngRange = 1f..18f, youngStep = 1f,
            olderRange = 8f..30f, olderStep = 2f,
        )
        Metric.LENGTH_FOR_AGE -> MetricInfo(
            title = if (ageMonths < 24f) "Length-for-age" else "Height-for-age",
            axisTitle = if (ageMonths < 24f) "Length (cm)" else "Height (cm)",
            unit = "cm",
            decimals = 1,
            youngRange = 44f..95f, youngStep = 5f,
            olderRange = 76f..125f, olderStep = 5f,
        )
        Metric.BMI_FOR_AGE -> MetricInfo(
            title = "BMI-for-age",
            axisTitle = "BMI (kg/m²)",
            unit = "kg/m²",
            decimals = 1,
            youngRange = 9f..23f, youngStep = 1f,
            olderRange = 11f..21f, olderStep = 1f,
        )
        // The WHO arm-circumference sheet starts at 3 months and is drawn in centimetres,
        // while the app records MUAC in millimetres for the 115/125 mm field cut-offs.
        Metric.MUAC_FOR_AGE -> MetricInfo(
            title = "Arm circumference-for-age",
            axisTitle = "Arm circumference (cm)",
            unit = "cm",
            decimals = 1,
            youngRange = 10f..19f, youngStep = 1f,
            olderRange = 11f..22f, olderStep = 1f,
            firstMonth = 3f,
        )
    }

    private fun table(metric: Metric, sex: Sex): List<Lms> {
        val female = sex == Sex.FEMALE
        return when (metric) {
            Metric.WEIGHT_FOR_AGE -> if (female) WhoLms.weightGirls else WhoLms.weightBoys
            Metric.LENGTH_FOR_AGE -> if (female) WhoLms.heightGirls else WhoLms.heightBoys
            Metric.BMI_FOR_AGE -> if (female) WhoLms.bmiGirls else WhoLms.bmiBoys
            Metric.MUAC_FOR_AGE -> if (female) WhoLms.muacGirls else WhoLms.muacBoys
        }
    }

    /** The youngest age the WHO publishes this indicator for — MUAC only starts at 3 months. */
    fun firstMonth(metric: Metric): Float = table(metric, Sex.MALE).firstOrNull()?.month ?: 0f

    // ---------------------------------------------------------------------
    // LMS maths
    // ---------------------------------------------------------------------

    /** The interpolated LMS row at an arbitrary age. */
    fun lmsAt(metric: Metric, sex: Sex, ageMonths: Float): Lms {
        val rows = table(metric, sex)
        if (rows.isEmpty()) return Lms(ageMonths, 1f, 0f, 1f)
        if (ageMonths <= rows.first().month) return rows.first().copy(month = ageMonths)
        if (ageMonths >= rows.last().month) return rows.last().copy(month = ageMonths)
        val upper = rows.first { it.month >= ageMonths }
        val lower = rows.last { it.month < ageMonths }
        val t = (ageMonths - lower.month) / (upper.month - lower.month)
        return Lms(
            month = ageMonths,
            l = lower.l + (upper.l - lower.l) * t,
            m = lower.m + (upper.m - lower.m) * t,
            s = lower.s + (upper.s - lower.s) * t,
        )
    }

    /** z = ((v/M)^L - 1) / (L·S), or ln(v/M)/S when L is zero. */
    fun zFrom(row: Lms, value: Float): Float {
        if (row.m <= 0f || row.s <= 0f || value <= 0f) return 0f
        return if (abs(row.l) < 1e-6f) {
            (ln(value / row.m) / row.s)
        } else {
            (((value / row.m).pow(row.l) - 1f) / (row.l * row.s))
        }
    }

    /** The measurement that sits exactly on a given z curve. */
    fun valueAt(row: Lms, z: Float): Float {
        if (row.m <= 0f) return 0f
        return if (abs(row.l) < 1e-6f) {
            row.m * exp(row.s * z)
        } else {
            val inner = 1f + row.l * row.s * z
            if (inner <= 0f) 0f else row.m * inner.pow(1f / row.l)
        }
    }

    fun zFor(metric: Metric, sex: Sex, ageMonths: Float, value: Float): Float =
        zFrom(lmsAt(metric, sex, ageMonths), value)

    /** The whole z-curve for charting: one row per month across the requested span. */
    fun curve(metric: Metric, sex: Sex, fromMonth: Float = 0f, toMonth: Float = 60f): List<Lms> {
        val step = 1f
        val out = ArrayList<Lms>()
        // Never extrapolate below the youngest published month: MUAC has no 0-3 month curve.
        var m = maxOf(fromMonth, firstMonth(metric))
        while (m <= toMonth + 0.01f) {
            out += lmsAt(metric, sex, m)
            m += step
        }
        return out
    }

    /** Percentile for a z-score, via the Zelen & Severo normal CDF approximation. */
    fun percentile(z: Float): Float {
        val x = z.toDouble()
        val t = 1.0 / (1.0 + 0.2316419 * abs(x))
        val d = 0.3989422804014327 * exp(-x * x / 2.0)
        var p = d * t * (0.319381530 + t * (-0.356563782 + t * (1.781477937 + t * (-1.821255978 + t * 1.330274429))))
        if (x > 0) p = 1.0 - p
        return (p * 100.0).toFloat()
    }

    /** BMI from the raw measurements, or null when either is missing. */
    fun bmi(weightKg: Float?, heightCm: Float?): Float? {
        if (weightKg == null || heightCm == null || heightCm <= 0f) return null
        val m = heightCm / 100f
        return weightKg / (m * m)
    }

    fun z(
        sex: Sex,
        ageMonths: Float,
        weightKg: Float?,
        heightCm: Float?,
        muacMm: Float? = null,
    ): GrowthZ {
        val heightZ = heightCm?.let { zFor(Metric.LENGTH_FOR_AGE, sex, ageMonths, it) }
        val weightZ = weightKg?.let { zFor(Metric.WEIGHT_FOR_AGE, sex, ageMonths, it) }
        val bmiZ = bmi(weightKg, heightCm)?.let { zFor(Metric.BMI_FOR_AGE, sex, ageMonths, it) }
        val muacZ = muacMm
            ?.takeIf { ageMonths >= firstMonth(Metric.MUAC_FOR_AGE) }
            ?.let { zFor(Metric.MUAC_FOR_AGE, sex, ageMonths, it / 10f) }
        return GrowthZ(
            weightForAge = weightZ,
            heightForAge = heightZ,
            bmiForAge = bmiZ,
            muacForAge = muacZ,
        )
    }

    /** The measured value for an indicator out of a recorded measurement. */
    fun measuredValue(
        metric: Metric,
        weightKg: Float?,
        heightCm: Float?,
        muacMm: Float? = null,
    ): Float? = when (metric) {
        Metric.WEIGHT_FOR_AGE -> weightKg
        Metric.LENGTH_FOR_AGE -> heightCm
        Metric.BMI_FOR_AGE -> bmi(weightKg, heightCm)
        Metric.MUAC_FOR_AGE -> muacMm?.let { it / 10f }
    }

    /** Length is measured lying down (< 24 months); height standing (24–60 months). */
    fun measurementLabel(ageMonths: Float): String =
        if (ageMonths < 24f) "Length" else "Height"

    fun measurementHint(ageMonths: Float): String =
        if (ageMonths < 24f) {
            "Measured lying down with an infantometer, heels against the footboard."
        } else {
            "Measured standing without shoes, heels together, looking straight ahead."
        }

    fun interpret(z: Float?): String? {
        if (z == null) return null
        return when {
            z <= -3f -> "Severely below normal"
            z <= -2f -> "Below normal"
            z < -1f -> "Borderline"
            z <= 1f -> "Normal"
            z <= 2f -> "Above normal"
            else -> "Well above normal"
        }
    }

    fun band(z: Float?): Int = when {
        z == null -> 0
        z <= -3f -> 3
        z <= -2f -> 2
        z < -1f -> 1
        else -> 0
    }

    fun severity(z: Float?): String? = when {
        z == null -> null
        abs(z) >= 3f -> "severe"
        abs(z) >= 2f -> "moderate"
        abs(z) >= 1f -> "mild"
        else -> null
    }

    // ---------------------------------------------------------------------
    // Compatibility shims for call sites written against the old curve API.
    // ---------------------------------------------------------------------

    @Deprecated("Use lmsAt", ReplaceWith("lmsAt(metric, sex, ageMonths)"))
    fun interpolateAt(metric: Metric, sex: Sex, ageMonths: Float): Lms = lmsAt(metric, sex, ageMonths)

    @Deprecated("Use zFor", ReplaceWith("zFor(metric, sex, ageMonths, value)"))
    fun zFromCurve(metric: Metric, sex: Sex, ageMonths: Float, value: Float): Float =
        zFor(metric, sex, ageMonths, value)

    /** Standard-deviation distance in measurement units at a given age (for "x kg below"). */
    fun unitsPerSd(metric: Metric, sex: Sex, ageMonths: Float): Float {
        val row = lmsAt(metric, sex, ageMonths)
        return abs(valueAt(row, 1f) - row.m)
    }

    /** Root-mean-square helper used by the trend readout. */
    fun rms(values: List<Float>): Float =
        if (values.isEmpty()) 0f else sqrt(values.sumOf { (it * it).toDouble() }.toFloat() / values.size)
}
