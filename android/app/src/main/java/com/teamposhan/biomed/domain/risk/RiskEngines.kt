package com.teamposhan.biomed.domain.risk

import com.teamposhan.biomed.data.model.AttentionResult
import com.teamposhan.biomed.data.model.CategoryResult
import com.teamposhan.biomed.data.model.CompositeRisk
import com.teamposhan.biomed.data.model.DevelopmentalQuotient
import com.teamposhan.biomed.data.model.DomainStatus
import com.teamposhan.biomed.data.model.GrowthZ
import com.teamposhan.biomed.data.model.RiskBand
import com.teamposhan.biomed.data.model.VitaminDResult
import com.teamposhan.biomed.domain.milestones.RedFlag
import com.teamposhan.biomed.domain.scoring.Scoring

// ---------------------------------------------------------------------------
// Vitamin D risk (non-invasive, questionnaire based — NOT a blood test)
// ---------------------------------------------------------------------------

data class VitaminDInput(
    val ageMonths: Float = 0f,
    val exclusivelyBreastfedNoSupplement: Boolean = false,
    val noSunExposure: Boolean = false,
    val darkSkin: Boolean = false,
    val fullyCoveredDress: Boolean = false,
    val urbanHighPollution: Boolean = false,
    val bornPremature: Boolean = false,
    val maternalDeficiency: Boolean = false,
    val lowDietaryIntake: Boolean = false,
    val winterSeason: Boolean = false,
)

object VitaminDEngine {

    fun assess(input: VitaminDInput): VitaminDResult {
        var score = 0
        val factors = mutableListOf<String>()

        if (input.exclusivelyBreastfedNoSupplement) { score += 2; factors += "Exclusive breastfeeding without vitamin D supplement" }
        if (input.noSunExposure) { score += 2; factors += "Limited sun exposure" }
        if (input.darkSkin) { score += 1; factors += "Darker skin pigmentation" }
        if (input.fullyCoveredDress) { score += 1; factors += "Skin largely covered outdoors" }
        if (input.urbanHighPollution) { score += 1; factors += "Urban / high air pollution" }
        if (input.bornPremature) { score += 1; factors += "Born prematurely" }
        if (input.maternalDeficiency) { score += 2; factors += "Maternal vitamin D deficiency" }
        if (input.lowDietaryIntake) { score += 1; factors += "Low dietary intake of fortified foods / fish / eggs" }
        if (input.winterSeason) { score += 1; factors += "Winter season / low sunlight" }

        val band = when {
            score >= 8 -> RiskBand.PRIORITY
            score >= 4 -> RiskBand.REVIEW
            score >= 2 -> RiskBand.MONITOR
            else -> RiskBand.ON_TRACK
        }
        return VitaminDResult(score = score, band = band, factors = factors)
    }

    fun riskFraction(result: VitaminDResult?): Float = when (result?.band) {
        RiskBand.PRIORITY -> 1f
        RiskBand.REVIEW -> 0.75f
        RiskBand.MONITOR -> 0.4f
        RiskBand.ON_TRACK -> 0f
        null -> 0f
    }
}

// ---------------------------------------------------------------------------
// Composite risk engine — one band + referral list for the whole screening
// ---------------------------------------------------------------------------

object CompositeRiskEngine {

    private const val W_DEV = 0.40f
    private const val W_GROWTH = 0.20f
    private const val W_ATTENTION = 0.20f
    private const val W_VITD = 0.10f
    private const val W_IMMUN = 0.10f

    fun compute(
        categories: List<CategoryResult>,
        redFlags: List<RedFlag>,
        dq: DevelopmentalQuotient,
        growthZ: GrowthZ?,
        attention: AttentionResult?,
        vitaminD: VitaminDResult?,
        overdueFraction: Float,
    ): CompositeRisk {
        val reasons = mutableListOf<String>()
        val referrals = mutableListOf<String>()

        // ---- developmental ----
        val delayed = categories.filter { it.status == DomainStatus.DELAYED }
        val concern = categories.filter { it.status == DomainStatus.CONCERN }
        var devRisk = 0f
        if (redFlags.isNotEmpty()) { devRisk = 1f; reasons += "Developmental red flag(s): " + redFlags.joinToString { it.label } }
        else devRisk = when {
            delayed.size >= 2 -> 1f
            delayed.size == 1 -> 0.75f
            concern.size >= 2 -> 0.5f
            concern.size == 1 -> 0.3f
            else -> 0f
        }
        // DQ nudges the score
        when (Scoring.dqBand(dq.dq)) {
            DomainStatus.DELAYED -> devRisk = maxOf(devRisk, 0.9f)
            DomainStatus.CONCERN -> devRisk = maxOf(devRisk, 0.4f)
            else -> {}
        }
        if (dq.dq > 0f) reasons += "DQ ${dq.dq.toInt()}% (dev age ${"%.1f".format(dq.developmentalAgeMonths)} mo)"

        if (delayed.isNotEmpty()) {
            delayed.forEach { c ->
                referrals += referralFor(c)
            }
            reasons += "Delayed domain(s): " + delayed.joinToString { it.category.name.replace('_', ' ').lowercase() }
        } else if (concern.isNotEmpty()) {
            reasons += "Concern in: " + concern.joinToString { it.category.name.replace('_', ' ').lowercase() }
        }

        // ---- growth ----
        var growthRisk = 0f
        growthZ?.worst?.let { z ->
            growthRisk = when {
                z <= -3f -> 1f
                z <= -2f -> 0.8f
                z < -1.5f -> 0.4f
                z > 3f -> 0.5f
                else -> 0f
            }
            if (z <= -2f) {
                reasons += "Growth below -2 SD (z ${"%.1f".format(z)})"
                referrals += "Nutrition / growth assessment at PHC"
            }
        }

        // ---- attention (confidence gated) ----
        var attentionRisk = 0f
        if (attention != null && !attention.inconclusive) {
            attentionRisk = when (attention.band) {
                RiskBand.PRIORITY -> 1f
                RiskBand.REVIEW -> 0.7f
                RiskBand.MONITOR -> 0.4f
                RiskBand.ON_TRACK -> 0f
            }
            if (attentionRisk >= 0.7f) {
                reasons += "Low attention / visual engagement score"
                referrals += "Developmental / vision review"
            }
        }

        // ---- vitamin D ----
        val vitDRisk = VitaminDEngine.riskFraction(vitaminD)
        if (vitaminD != null && vitaminD.band >= RiskBand.REVIEW) {
            reasons += "Vitamin D deficiency risk score ${vitaminD.score}"
            referrals += "Consider serum 25(OH)D test"
        }

        // ---- immunization ----
        val immunRisk = overdueFraction.coerceIn(0f, 1f)
        if (immunRisk > 0f) reasons += "Overdue immunizations"

        val weighted = W_DEV * devRisk + W_GROWTH * growthRisk +
            W_ATTENTION * attentionRisk + W_VITD * vitDRisk + W_IMMUN * immunRisk

        val band = when {
            redFlags.isNotEmpty() -> RiskBand.PRIORITY
            delayed.size >= 2 -> RiskBand.PRIORITY
            devRisk >= 0.9f && vitDRisk >= 0.75f -> RiskBand.PRIORITY
            delayed.size == 1 || growthRisk >= 0.8f || attentionRisk >= 0.7f || vitDRisk >= 0.75f -> RiskBand.REVIEW
            concern.isNotEmpty() || growthRisk >= 0.4f || attentionRisk >= 0.4f || immunRisk > 0.34f -> RiskBand.MONITOR
            else -> RiskBand.ON_TRACK
        }

        return CompositeRisk(
            devScore = devRisk,
            growthScore = growthRisk,
            attentionScore = attentionRisk,
            vitaminDScore = vitDRisk,
            immunizationScore = immunRisk,
            overall = weighted,
            band = band,
            reasons = reasons.distinct(),
            referrals = referrals.distinct(),
        )
    }

    private fun referralFor(category: CategoryResult): String = when (category.category) {
        com.teamposhan.biomed.data.model.AssessmentCategory.GROSS_MOTOR -> "Physiotherapy referral"
        com.teamposhan.biomed.data.model.AssessmentCategory.FINE_MOTOR -> "Developmental review"
        com.teamposhan.biomed.data.model.AssessmentCategory.COMMUNICATION -> "Speech & hearing assessment"
        com.teamposhan.biomed.data.model.AssessmentCategory.SOCIAL_EMOTIONAL -> "Developmental / behavioural review"
        com.teamposhan.biomed.data.model.AssessmentCategory.NUTRITION -> "Nutrition counselling"
    }
}
