package com.teamposhan.biomed.domain.growth

import com.teamposhan.biomed.data.model.DomainStatus
import com.teamposhan.biomed.data.model.GrowthFinding
import com.teamposhan.biomed.data.model.GrowthZ
import com.teamposhan.biomed.data.model.MuacBand

/**
 * WHO z-score and MUAC interpretation, following the standard cut-offs used by India's
 * ICDS / Poshan Abhiyaan community screening.
 *
 * Weight-for-age  : < -3 SD severe underweight, < -2 SD underweight, > +2 SD high weight
 * Length/height   : < -3 SD severe stunting, < -2 SD stunting, -2..-1 SD monitor, > +2 SD tall
 * BMI-for-age     : > +1 SD overweight (~85th), > +1.645 SD obesity (~95th)
 * MUAC (6-59 mo)  : < 115 mm SAM, 115-125 mm MAM, >= 125 mm normal
 */
object GrowthInterpretation {

    const val MUAC_SAM_MM = 115f
    const val MUAC_MAM_MM = 125f
    const val MUAC_MIN_AGE_MONTHS = 6f
    const val MUAC_MAX_AGE_MONTHS = 59f

    fun muacBand(muacMm: Float?, ageMonths: Float): MuacBand {
        if (muacMm == null) return MuacBand.NOT_APPLICABLE
        if (ageMonths < MUAC_MIN_AGE_MONTHS || ageMonths > MUAC_MAX_AGE_MONTHS) return MuacBand.NOT_APPLICABLE
        return when {
            muacMm < MUAC_SAM_MM -> MuacBand.SAM
            muacMm < MUAC_MAM_MM -> MuacBand.MAM
            else -> MuacBand.NORMAL
        }
    }

    fun muacLabel(band: MuacBand): String = when (band) {
        MuacBand.SAM -> "Severe acute malnutrition (SAM)"
        MuacBand.MAM -> "Moderate acute malnutrition (MAM)"
        MuacBand.NORMAL -> "Normal"
        MuacBand.NOT_APPLICABLE -> "Not applicable for this age"
    }

    fun weightForAge(z: Float): GrowthFinding = when {
        z < -3f -> GrowthFinding(
            "Severely underweight",
            "Weight-for-age is below -3 SD. This is severe malnutrition and needs urgent assessment.",
            DomainStatus.DELAYED,
        )
        z < -2f -> GrowthFinding(
            "Underweight",
            "Weight-for-age is below -2 SD — moderate undernutrition. Review feeding and follow up in 2–4 weeks.",
            DomainStatus.CONCERN,
        )
        z > 2f -> GrowthFinding(
            "High weight-for-age",
            "Above +2 SD. Possible overweight — review portion size and sugar intake.",
            DomainStatus.CONCERN,
        )
        else -> GrowthFinding("Weight normal", "Weight-for-age is within the normal range (-2 to +2 SD).", DomainStatus.ON_TRACK)
    }

    fun lengthForAge(z: Float, ageMonths: Float): GrowthFinding {
        val what = if (ageMonths < 24f) "Length" else "Height"
        return when {
            z < -3f -> GrowthFinding(
                "Severe stunting",
                "$what-for-age is below -3 SD. Severe long-term growth restriction — refer for urgent nutritional and medical care.",
                DomainStatus.DELAYED,
            )
            z < -2f -> GrowthFinding(
                "Stunting",
                "$what-for-age is below -2 SD, which defines stunting (chronic undernutrition or repeated illness). Refer for nutrition assessment.",
                DomainStatus.CONCERN,
            )
            z < -1f -> GrowthFinding(
                "Mildly low $what",
                "Between -1 and -2 SD. Not stunted, but worth monitoring at the next visit.",
                DomainStatus.CONCERN,
            )
            z > 2f -> GrowthFinding(
                "Tall for age",
                "Above +2 SD — uncommon (about 2.3% of children). Usually normal, occasionally worth a clinical look.",
                DomainStatus.ON_TRACK,
            )
            else -> GrowthFinding("$what normal", "$what-for-age is within the normal range.", DomainStatus.ON_TRACK)
        }
    }

    fun bmiForAge(z: Float): GrowthFinding = when {
        z > 1.645f -> GrowthFinding(
            "Obesity",
            "BMI-for-age is above the 95th percentile (+1.645 SD). Advise nutrition counselling and activity.",
            DomainStatus.CONCERN,
        )
        z > 1f -> GrowthFinding(
            "Overweight",
            "BMI-for-age is above +1 SD (about the 85th percentile). Monitor weight gain and diet.",
            DomainStatus.CONCERN,
        )
        z < -3f -> GrowthFinding(
            "Severe wasting",
            "BMI-for-age is below -3 SD — severe acute malnutrition. Urgent referral.",
            DomainStatus.DELAYED,
        )
        z < -2f -> GrowthFinding(
            "Wasting",
            "BMI-for-age is below -2 SD — moderate acute malnutrition. Refer for nutrition assessment.",
            DomainStatus.CONCERN,
        )
        else -> GrowthFinding("BMI normal", "BMI-for-age is within the normal range.", DomainStatus.ON_TRACK)
    }

    fun muacFinding(muacMm: Float?, ageMonths: Float): GrowthFinding {
        return when (muacBand(muacMm, ageMonths)) {
            MuacBand.SAM -> GrowthFinding(
                "SAM by MUAC",
                "MUAC is under 115 mm. This is severe acute malnutrition — refer to the nearest Nutrition Rehabilitation Centre today.",
                DomainStatus.DELAYED,
            )
            MuacBand.MAM -> GrowthFinding(
                "MAM by MUAC",
                "MUAC is between 115 and 125 mm — moderate acute malnutrition. Enrol for supplementary nutrition and reassess in 2 weeks.",
                DomainStatus.CONCERN,
            )
            MuacBand.NORMAL -> GrowthFinding("MUAC normal", "MUAC is 125 mm or more.", DomainStatus.ON_TRACK)
            MuacBand.NOT_APPLICABLE -> GrowthFinding(
                "MUAC not applicable",
                "MUAC screening applies to children between 6 and 59 months.",
                DomainStatus.NOT_ASSESSED,
            )
        }
    }

    /** All findings for a completed measurement, worst first. */
    fun findings(z: GrowthZ?, muacMm: Float?, ageMonths: Float): List<GrowthFinding> {
        val list = mutableListOf<GrowthFinding>()
        z?.weightForAge?.let { list += weightForAge(it) }
        z?.heightForAge?.let { list += lengthForAge(it, ageMonths) }
        z?.bmiForAge?.let { list += bmiForAge(it) }
        if (muacMm != null) list += muacFinding(muacMm, ageMonths)
        return list.sortedByDescending { it.severity.ordinal }
    }
}
