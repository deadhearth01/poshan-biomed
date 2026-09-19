package com.teamposhan.biomed.domain.scoring

import com.teamposhan.biomed.data.model.AssessmentAnswer
import com.teamposhan.biomed.data.model.AssessmentCategory
import com.teamposhan.biomed.data.model.CategoryResult
import com.teamposhan.biomed.data.model.DevelopmentalQuotient
import com.teamposhan.biomed.data.model.DomainStatus
import com.teamposhan.biomed.data.model.ResponseType
import com.teamposhan.biomed.domain.milestones.AgeBand
import com.teamposhan.biomed.domain.milestones.MilestoneBank
import com.teamposhan.biomed.domain.milestones.RedFlag
import kotlin.math.ceil

data class ScoringOutput(
    val categories: List<CategoryResult>,
    val redFlags: List<RedFlag>,
    val dq: DevelopmentalQuotient,
)

/**
 * Deterministic, explainable scoring. No black boxes — every status can be traced back to
 * the answers and the AIIMS red-flag chart.
 */
object Scoring {

    private val DEV_CATEGORIES = listOf(
        AssessmentCategory.GROSS_MOTOR,
        AssessmentCategory.FINE_MOTOR,
        AssessmentCategory.COMMUNICATION,
        AssessmentCategory.SOCIAL_EMOTIONAL,
    )

    fun evaluate(assessedAgeMonths: Float, chronologicalMonths: Float, answers: List<AssessmentAnswer>): ScoringOutput {
        val answersById = answers.associateBy { it.questionId }
        val band = AgeBand.forAge(assessedAgeMonths)

        val categories = DEV_CATEGORIES.map { category ->
            // Catch-up screening: a child first seen at 11 months is asked every milestone
            // from birth onwards, not just their current band, because the skills they
            // should already have are exactly what tells us whether they are behind.
            val allQuestions = MilestoneBank.catchUpQuestions(assessedAgeMonths, category)
            val currentBandQuestions = allQuestions.filter { it.band == band }
            val earlierQuestions = allQuestions.filter { it.band != band }

            val answered = allQuestions.count { answersById[it.id]?.response != null }

            // A milestone counts as missed when it is not securely achieved — "No", "Not
            // sure", or a "Maybe" that only earns half credit. The numeric weighting lives
            // on ResponseType so it is stated once, not re-derived here.
            fun isMissed(id: String): Boolean =
                answersById[id]?.response?.isConcern == true

            // "Not sure" is flagged in its own right: a caregiver who cannot say whether
            // their child does an everyday behaviour has effectively not observed it.
            val uncertain = allQuestions.filter {
                answersById[it.id]?.response?.flagsConcern == true
            }

            val currentConcerns = currentBandQuestions.count { isMissed(it.id) }
            val missedEarlier = earlierQuestions.filter { isMissed(it.id) }
            val concerns = currentConcerns + missedEarlier.size

            val flags = triggeredFlagsFor(category, assessedAgeMonths, answersById)
            val threshold = maxOf(2, ceil(currentBandQuestions.size * 0.5f).toInt())
            val status = when {
                flags.isNotEmpty() -> DomainStatus.DELAYED
                answered == 0 -> DomainStatus.NOT_ASSESSED
                // A milestone from a band the child has already passed is a delay by
                // definition - they are late for it, not merely approaching it.
                missedEarlier.isNotEmpty() -> DomainStatus.DELAYED
                currentConcerns >= threshold -> DomainStatus.DELAYED
                currentConcerns >= 1 -> DomainStatus.CONCERN
                else -> DomainStatus.ON_TRACK
            }
            CategoryResult(
                category = category,
                answered = answered,
                total = allQuestions.size,
                concerns = concerns,
                status = status,
                redFlags = flags.map { it.label } +
                    uncertain.map { "Caregiver unsure: " + it.text },
                missedEarlier = missedEarlier.map { it.text },
                earliestMissBand = missedEarlier.minByOrNull { it.band.minMonths }?.band?.label,
            )
        }

        val allFlags = MilestoneBank.redFlags.filter { assessedAgeMonths >= it.ageMonths - 0.5f }
            .filter { answersById[it.questionId]?.let { a -> a.response == ResponseType.NOT_YET || a.response == ResponseType.NOT_SURE } == true }

        return ScoringOutput(
            categories = categories,
            redFlags = allFlags,
            dq = developmentalQuotient(assessedAgeMonths, chronologicalMonths, answersById),
        )
    }

    private fun triggeredFlagsFor(
        category: AssessmentCategory,
        ageMonths: Float,
        answersById: Map<String, AssessmentAnswer>,
    ): List<RedFlag> = MilestoneBank.redFlags.filter { flag ->
        flag.category == category &&
            ageMonths >= flag.ageMonths - 0.5f &&
            answersById[flag.questionId]?.let {
                it.response == ResponseType.NOT_YET || it.response == ResponseType.NOT_SURE
            } == true
    }

    /**
     * Developmental age is estimated from achieved milestones across the bands up to the
     * child's age, then DQ = (developmental age ÷ chronological age) × 100.
     */
    fun developmentalQuotient(
        assessedAgeMonths: Float,
        chronologicalMonths: Float,
        answersById: Map<String, AssessmentAnswer>,
    ): DevelopmentalQuotient {
        var devAge = 0f
        for (band in AgeBand.entries) {
            if (band.minMonths > assessedAgeMonths) break
            val questions = MilestoneBank.all().filter {
                it.band == band && it.category in DEV_CATEGORIES
            }
            if (questions.isEmpty()) continue
            val answered = questions.filter { answersById[it.id]?.response != null }
            if (answered.isEmpty()) continue
            // Graded credit: Yes = 1, Maybe = 0.5, No / Not sure = 0. A child who partly
            // does half their milestones is genuinely ahead of one who does none of them,
            // and a binary count erased that distinction.
            val achieved = answered.sumOf { (answersById[it.id]?.response?.score ?: 0f).toDouble() }
            val ratio = (achieved / answered.size).toFloat()
            val width = (band.maxMonths - band.minMonths).coerceAtLeast(1).toFloat()
            devAge += ratio * width
        }
        devAge = devAge.coerceIn(0f, maxOf(chronologicalMonths, 1f))
        val dq = if (chronologicalMonths > 0f) (devAge / chronologicalMonths) * 100f else 0f
        return DevelopmentalQuotient(
            developmentalAgeMonths = devAge,
            chronologicalAgeMonths = chronologicalMonths,
            dq = dq,
        )
    }

    /** DQ interpretation used for the composite score. */
    fun dqBand(dq: Float): DomainStatus = when {
        dq <= 0f -> DomainStatus.NOT_ASSESSED
        dq >= 85f -> DomainStatus.ON_TRACK
        dq >= 70f -> DomainStatus.CONCERN
        else -> DomainStatus.DELAYED
    }
}
