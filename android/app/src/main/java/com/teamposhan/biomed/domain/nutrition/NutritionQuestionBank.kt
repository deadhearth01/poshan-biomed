package com.teamposhan.biomed.domain.nutrition

import com.teamposhan.biomed.data.model.ResponseType
import com.teamposhan.biomed.domain.milestones.AgeBand

/**
 * Nutrition questions, in two parts.
 *
 * **Part 1 — feeding practice by age** ([byBand]). Asked at every visit, because what
 * counts as adequate feeding changes completely across the first five years: exclusive
 * breastfeeding at 2 months, complementary foods from 6, family foods by around 12, dietary
 * adequacy thereafter. Anchored on the WHO complementary-feeding guidance — complementary
 * feeding from 6 months alongside continued breastfeeding through 23 months, 3–4 meals a
 * day for 9–23 month olds with additional nutritious snacks as needed, and increasing food
 * variety and consistency with age.
 *
 * **Part 2 — background and risk factors** ([background]). Asked once per child, not once
 * per visit. Birth order, birth size, maternal age and education, household water, toilet,
 * fuel and wealth do not change between a 9-month and a 12-month visit, and re-asking them
 * wastes the caregiver's patience on questions whose answers the app already has.
 *
 * Both use the same Yes / Maybe / No / Not sure answers as every other questionnaire in the
 * app, so a caregiver never has to learn a second answer vocabulary.
 *
 * ## Polarity
 *
 * Feeding-practice questions are phrased so **Yes is good**, matching module 1. The
 * background factors are inherently the opposite — "Does the household have no toilet?" —
 * and are tagged [RiskPolarity.RISK], which inverts their scoring. The polarity is a
 * property of the question, checked once, rather than something each call site has to
 * remember.
 */
enum class RiskPolarity {
    /** Yes = the practice is in place = reassuring. */
    PRACTICE,

    /** Yes = the risk factor is present = concerning. */
    RISK,
}

data class NutritionQuestion(
    val id: String,
    val text: String,
    val polarity: RiskPolarity = RiskPolarity.PRACTICE,
    /** Shown under the question so the caregiver knows why it is being asked. */
    val rationale: String? = null,
) {
    fun isConcerning(response: ResponseType?): Boolean = when (response) {
        null -> false
        ResponseType.NOT_SURE -> true
        ResponseType.YES_OBSERVED -> polarity == RiskPolarity.RISK
        ResponseType.NOT_YET -> polarity == RiskPolarity.PRACTICE
        ResponseType.MAYBE -> true
    }

    /** Concern weight 0..1, so a "maybe" counts for less than a definite answer. */
    fun concernScore(response: ResponseType?): Float = when {
        response == null -> 0f
        !isConcerning(response) -> 0f
        response == ResponseType.MAYBE -> 0.5f
        else -> 1f
    }
}

/** A themed group of background questions, for a readable form. */
data class NutritionSection(
    val title: String,
    val questions: List<NutritionQuestion>,
)

object NutritionQuestionBank {

    private fun q(
        id: String,
        text: String,
        polarity: RiskPolarity = RiskPolarity.PRACTICE,
        rationale: String? = null,
    ) = NutritionQuestion(id, text, polarity, rationale)

    // ---------------------------------------------------------------------
    // Part 1: feeding practice, by age band
    // ---------------------------------------------------------------------

    private val practice: Map<AgeBand, List<NutritionQuestion>> = mapOf(
        AgeBand.M0_3 to listOf(
            q("NUT_M0_3_1", "Is the baby currently being breastfed?"),
            q(
                "NUT_M0_3_2", "Was breastfeeding started soon after birth?",
                rationale = "Early initiation protects against infection and helps establish supply.",
            ),
            q("NUT_M0_3_3", "Does the baby receive formula milk?", RiskPolarity.RISK),
            q("NUT_M0_3_4", "Has the baby had a postnatal health checkup?"),
        ),

        AgeBand.M4_6 to listOf(
            q("NUT_M4_6_1", "Is the baby currently being breastfed?"),
            q("NUT_M4_6_2", "Does the baby receive breast milk or formula milk regularly?"),
            q(
                "NUT_M4_6_3", "Has the baby's feeding pattern stayed suitable for their age?",
                rationale = "Milk alone remains adequate until about 6 months.",
            ),
            q("NUT_M4_6_4", "Has the baby been weighed recently?"),
        ),

        // Complementary feeding begins around 6 months, so the question set changes here.
        AgeBand.M7_9 to listOf(
            q(
                "NUT_M7_9_1", "Has the child started eating complementary foods?",
                rationale = "WHO recommends starting complementary foods at about 6 months.",
            ),
            q("NUT_M7_9_2", "Is the child still receiving breast milk or suitable milk feeds?"),
            q("NUT_M7_9_3", "Does the child eat different types of complementary foods?"),
            q("NUT_M7_9_4", "Is the child's weight being monitored regularly?"),
        ),

        AgeBand.M10_12 to listOf(
            q("NUT_M10_12_1", "Does the child receive complementary foods regularly?"),
            q(
                "NUT_M10_12_2", "Does the child's diet include a variety of foods?",
                rationale = "Feeding frequency, food variety and thickness should all increase in this period.",
            ),
            q("NUT_M10_12_3", "Is the child still receiving breast milk or suitable milk feeds?"),
            q("NUT_M10_12_4", "Has the child's growth been monitored recently?"),
        ),

        AgeBand.M13_18 to listOf(
            q(
                "NUT_M13_18_1", "Does the child eat family foods suitable for their age?",
                rationale = "By around 12 months most children can eat the same foods as the family.",
            ),
            q("NUT_M13_18_2", "Does the child eat a variety of foods across meals?"),
            q("NUT_M13_18_3", "Does the child still receive breast milk, where this applies?"),
            q("NUT_M13_18_4", "Does the child get extra nutritious food between meals when needed?"),
        ),

        AgeBand.M19_24 to listOf(
            q("NUT_M19_24_1", "Does the child eat a variety of family foods?"),
            q(
                "NUT_M19_24_2", "Does the child get regular meals each day?",
                rationale = "WHO recommends 3–4 meals a day at this age, plus nutritious snacks as needed.",
            ),
            q("NUT_M19_24_3", "Does the child get nutritious snacks when needed?"),
            q("NUT_M19_24_4", "Is the child's growth being monitored regularly?"),
        ),

        // From two years the focus shifts from infant feeding to dietary adequacy.
        AgeBand.M25_36 to listOf(
            q("NUT_M25_36_1", "Does the child eat a variety of foods every day?"),
            q("NUT_M25_36_2", "Does the child regularly eat nutrient-rich foods?"),
            q("NUT_M25_36_3", "Does the child have regular meals and suitable snacks?"),
            q("NUT_M25_36_4", "Have the child's weight and height been measured recently?"),
        ),

        AgeBand.M37_48 to listOf(
            q("NUT_M37_48_1", "Does the child eat a variety of foods from different food groups?"),
            q("NUT_M37_48_2", "Does the child have regular meals each day?"),
            q("NUT_M37_48_3", "Does the child regularly eat nutrient-rich foods?"),
            q("NUT_M37_48_4", "Have the child's weight and height been checked recently?"),
        ),

        AgeBand.M49_60 to listOf(
            q("NUT_M49_60_1", "Does the child have a varied diet with regular meals?"),
            q("NUT_M49_60_2", "Does the child regularly eat nutrient-rich foods?"),
            q("NUT_M49_60_3", "Does the child have a healthy, consistent eating pattern?"),
            q("NUT_M49_60_4", "Have the child's weight and height been monitored recently?"),
        ),
    )

    fun forBand(band: AgeBand): List<NutritionQuestion> = practice[band].orEmpty()

    fun forAge(ageMonths: Float): List<NutritionQuestion> = forBand(AgeBand.forAge(ageMonths))

    // ---------------------------------------------------------------------
    // Part 2: background and risk factors, asked once per child
    // ---------------------------------------------------------------------

    /**
     * The 19 background risk factors, grouped for a readable form.
     *
     * Every one is phrased so **Yes means the risk is present**, which is the natural
     * phrasing for these and the reason they carry [RiskPolarity.RISK] rather than being
     * awkwardly inverted into practice questions.
     */
    val background: List<NutritionSection> = listOf(
        NutritionSection(
            "Birth and early history",
            listOf(
                q("NUT_BG_1", "Is the child the 2nd, 3rd or 4th born?", RiskPolarity.RISK),
                q("NUT_BG_2", "Is the child the 5th born or later?", RiskPolarity.RISK),
                q("NUT_BG_3", "Was the child small at birth?", RiskPolarity.RISK),
                q("NUT_BG_4", "Was breastfeeding not started immediately after birth?", RiskPolarity.RISK),
                q("NUT_BG_5", "Did the child have a postnatal health checkup?"),
            ),
        ),
        NutritionSection(
            "Feeding history",
            listOf(
                q("NUT_BG_6", "Is the child currently breastfed?"),
                q("NUT_BG_7", "Does the child receive formula milk?", RiskPolarity.RISK),
                q("NUT_BG_8", "Does the child receive fresh animal milk?", RiskPolarity.RISK),
            ),
        ),
        NutritionSection(
            "Mother's health",
            listOf(
                q("NUT_BG_9", "Is the mother younger than 20 years?", RiskPolarity.RISK),
                q("NUT_BG_10", "Is the mother underweight?", RiskPolarity.RISK),
                q("NUT_BG_11", "Does the mother have anaemia?", RiskPolarity.RISK),
                q("NUT_BG_12", "Has the mother had no formal education?", RiskPolarity.RISK),
                q("NUT_BG_13", "Was the child delivered by caesarean section?", RiskPolarity.RISK),
            ),
        ),
        NutritionSection(
            "Household and surroundings",
            listOf(
                q("NUT_BG_14", "Is the household in the poorer wealth category?", RiskPolarity.RISK),
                q("NUT_BG_15", "Does the household have no toilet?", RiskPolarity.RISK),
                q("NUT_BG_16", "Does the household use an unimproved water source?", RiskPolarity.RISK),
                q("NUT_BG_17", "Has the child's father had no formal education?", RiskPolarity.RISK),
                q("NUT_BG_18", "Does the household cook with unclean fuel?", RiskPolarity.RISK),
                q("NUT_BG_19", "Does the child live in a rural area?", RiskPolarity.RISK),
            ),
        ),
    )

    val backgroundQuestions: List<NutritionQuestion> get() = background.flatMap { it.questions }

    fun byId(id: String): NutritionQuestion? =
        practice.values.flatten().firstOrNull { it.id == id }
            ?: backgroundQuestions.firstOrNull { it.id == id }

    val practiceCount: Int get() = practice.values.sumOf { it.size }
    val backgroundCount: Int get() = backgroundQuestions.size
}

/**
 * The outcome of the nutrition questionnaire.
 *
 * Reported as concern counts rather than a score out of 100. The background factors are
 * associated with undernutrition risk at population level; they are not a per-child
 * predictor, and presenting them as one would overstate what they can tell a caregiver
 * about their own child.
 *
 * The quantitative side of nutrition is the growth measurement — weight-for-age,
 * length/height-for-age, BMI-for-age and MUAC against the WHO standards. That is the part
 * that says something about this child; this questionnaire says what to talk about.
 */
data class NutritionResult(
    val practiceAnswered: Int,
    val practiceTotal: Int,
    val practiceConcerns: Float,
    val backgroundAnswered: Int,
    val backgroundTotal: Int,
    val backgroundRisks: Float,
    val concerns: List<String>,
) {
    val level: Level
        get() = when {
            practiceAnswered == 0 -> Level.NOT_ASSESSED
            practiceConcerns >= practiceTotal * 0.5f -> Level.NEEDS_SUPPORT
            practiceConcerns > 0f -> Level.SOME_GAPS
            else -> Level.ADEQUATE
        }

    enum class Level(val label: String, val guidance: String) {
        ADEQUATE(
            "Feeding looks appropriate",
            "Feeding practices match what is recommended for this age. Keep going.",
        ),
        SOME_GAPS(
            "Some gaps in feeding",
            "One or two feeding practices could be improved. Discuss these with the health worker.",
        ),
        NEEDS_SUPPORT(
            "Feeding needs support",
            "Several feeding practices differ from what is recommended at this age. " +
                "Arrange nutrition counselling.",
        ),
        NOT_ASSESSED("Not assessed", "The nutrition questions have not been answered yet."),
    }

    companion object {
        fun evaluate(
            practice: List<NutritionQuestion>,
            background: List<NutritionQuestion>,
            answers: Map<String, ResponseType>,
        ): NutritionResult {
            val concerns = mutableListOf<String>()
            var practiceScore = 0f
            var backgroundScore = 0f

            practice.forEach { question ->
                val score = question.concernScore(answers[question.id])
                if (score > 0f) {
                    practiceScore += score
                    if (score >= 1f) concerns += question.text
                }
            }
            background.forEach { question ->
                val score = question.concernScore(answers[question.id])
                if (score > 0f) backgroundScore += score
            }

            return NutritionResult(
                practiceAnswered = practice.count { answers[it.id] != null },
                practiceTotal = practice.size,
                practiceConcerns = practiceScore,
                backgroundAnswered = background.count { answers[it.id] != null },
                backgroundTotal = background.size,
                backgroundRisks = backgroundScore,
                concerns = concerns,
            )
        }
    }
}
