package com.teamposhan.biomed.domain.milestones

import com.teamposhan.biomed.data.model.AssessmentCategory

/**
 * Age bands and the milestone question bank.
 *
 * Source of truth: "Question bank for developmental milestones 0-5 years" (supplied PDF).
 * The PDF lays the bank out as a table: one row per age band, one column per domain
 * (GROSS MOTOR / FINE MOTOR / COMMUNICATION / SOCIAL-EMOTIONAL). The nine bands below are
 * the PDF's own rows, verbatim: 0-3, 4-6, 7-9, 10-12, 13-18, 19-24, 25-36, 37-48, 49-60
 * months. They are inclusive whole-month ranges and do NOT overlap.
 */
enum class AgeBand(val minMonths: Int, val maxMonths: Int, val label: String) {
    M0_3(0, 3, "0–3 months"),
    M4_6(4, 6, "4–6 months"),
    M7_9(7, 9, "7–9 months"),
    M10_12(10, 12, "10–12 months"),
    M13_18(13, 18, "13–18 months"),
    M19_24(19, 24, "19–24 months"),
    M25_36(25, 36, "25–36 months"),
    M37_48(37, 48, "37–48 months"),
    M49_60(49, 60, "49–60 months");

    val midpointMonths: Float get() = (minMonths + maxMonths) / 2f

    companion object {
        /**
         * The band a child of [months] belongs to.
         *
         * Boundary rule: we take the LAST (oldest) band whose [minMonths] is still at or below
         * the child's age. This is deterministic even if the bands were ever made contiguous or
         * overlapping — e.g. with bands 0-3 / 3-6 a child of exactly 3.0 months would fall in the
         * later band (3-6). With the PDF's actual non-overlapping bands the rule simply means a
         * fractional age inside a gap (say 3.5 months, between the 0-3 and 4-6 rows) screens with
         * the younger, already-expected band; ages above 60 months clamp to the oldest band.
         */
        fun forAge(months: Float): AgeBand =
            entries.lastOrNull { it.minMonths <= months } ?: entries.first()

        /** Every band whose [minMonths] is at or below [months], youngest first. */
        fun upTo(months: Float): List<AgeBand> = entries.filter { it.minMonths <= months }
    }
}

data class MilestoneQuestion(
    val id: String,
    val category: AssessmentCategory,
    val band: AgeBand,
    val text: String,
)

/**
 * Red flags (source: AIIMS Child Neurology red-flag chart). A red flag escalates the
 * category regardless of the questionnaire score.
 */
data class RedFlag(
    val ageMonths: Int,
    val label: String,
    val category: AssessmentCategory,
    val referral: String,
    /** Band question that must be answered NOT_YET / NOT_SURE for this flag to fire. */
    val questionId: String,
)

object MilestoneBank {

    // Source: AIIMS Child Neurology red-flag chart, mapped onto the age-band questions.
    val redFlags: List<RedFlag> = listOf(
        RedFlag(2, "No social smile", AssessmentCategory.SOCIAL_EMOTIONAL, "Developmental review", "M0_3_SOCIAL_EMOTIONAL_2"),
        RedFlag(5, "Unable to hold or grasp a rattle", AssessmentCategory.FINE_MOTOR, "Developmental review", "M4_6_FINE_MOTOR_1"),
        RedFlag(9, "No sitting without support", AssessmentCategory.GROSS_MOTOR, "Physiotherapy / developmental review", "M7_9_GROSS_MOTOR_1"),
        RedFlag(12, "No standing with assistance", AssessmentCategory.GROSS_MOTOR, "Physiotherapy referral", "M10_12_GROSS_MOTOR_3"),
        RedFlag(12, "No pincer grasp", AssessmentCategory.FINE_MOTOR, "Developmental review", "M10_12_FINE_MOTOR_1"),
        RedFlag(12, "No babbling / vocal imitation", AssessmentCategory.COMMUNICATION, "Speech & hearing review", "M10_12_COMMUNICATION_3"),
        RedFlag(12, "No pointing", AssessmentCategory.FINE_MOTOR, "Developmental review", "M10_12_FINE_MOTOR_3"),
        RedFlag(17, "Unable to stand alone", AssessmentCategory.GROSS_MOTOR, "Physiotherapy referral", "M13_18_GROSS_MOTOR_1"),
        RedFlag(18, "Unable to walk alone", AssessmentCategory.GROSS_MOTOR, "Physiotherapy referral", "M13_18_GROSS_MOTOR_2"),
        RedFlag(18, "No use of single words", AssessmentCategory.COMMUNICATION, "Speech & hearing review", "M13_18_COMMUNICATION_1"),
        RedFlag(20, "Unable to manipulate objects / remove clothing", AssessmentCategory.FINE_MOTOR, "Developmental review", "M19_24_FINE_MOTOR_1"),
        RedFlag(24, "Unable to scribble", AssessmentCategory.FINE_MOTOR, "Developmental review", "M19_24_FINE_MOTOR_3"),
        RedFlag(24, "Single word vocabulary ≤ 10 words", AssessmentCategory.COMMUNICATION, "Speech & hearing review", "M19_24_COMMUNICATION_1"),
        RedFlag(24, "Unable to walk upstairs with help", AssessmentCategory.GROSS_MOTOR, "Physiotherapy referral", "M19_24_GROSS_MOTOR_3"),
        RedFlag(24, "Little interactive play", AssessmentCategory.SOCIAL_EMOTIONAL, "Developmental review", "M19_24_SOCIAL_EMOTIONAL_3"),
        RedFlag(30, "< 100 words, no two-word combination", AssessmentCategory.COMMUNICATION, "Speech & hearing review", "M25_36_COMMUNICATION_2"),
        RedFlag(36, "Cannot work simple toys", AssessmentCategory.FINE_MOTOR, "Developmental review", "M25_36_FINE_MOTOR_2"),
        RedFlag(36, "< 200 words, no telegraphic sentence", AssessmentCategory.COMMUNICATION, "Speech & hearing review", "M25_36_COMMUNICATION_1"),
        RedFlag(42, "< 600 words, no simple sentences", AssessmentCategory.COMMUNICATION, "Speech & hearing review", "M37_48_COMMUNICATION_1"),
        RedFlag(48, "Unable to jump", AssessmentCategory.GROSS_MOTOR, "Physiotherapy referral", "M37_48_GROSS_MOTOR_2"),
        RedFlag(48, "Does not respond to peers", AssessmentCategory.SOCIAL_EMOTIONAL, "Developmental review", "M37_48_SOCIAL_EMOTIONAL_1"),
        RedFlag(60, "Does not draw a picture", AssessmentCategory.FINE_MOTOR, "Developmental review", "M49_60_FINE_MOTOR_1"),
        RedFlag(60, "Unusually withdrawn and not active", AssessmentCategory.SOCIAL_EMOTIONAL, "Developmental review", "M49_60_SOCIAL_EMOTIONAL_1"),
    )

    private fun q(
        band: AgeBand,
        category: AssessmentCategory,
        vararg texts: String,
    ): List<MilestoneQuestion> = texts.mapIndexed { index, text ->
        MilestoneQuestion(
            id = "${band.name}_${category.name}_${index + 1}",
            category = category,
            band = band,
            text = text,
        )
    }

    /**
     * Every question in the PDF, in the PDF's own row/column order: 123 in total
     * (13/13/13/13/15/13/14/13/16 per band). Wording is the app's existing caregiver-facing
     * phrasing, which matches the PDF entry for entry.
     * The PDF carries no NUTRITION column, so that category has no milestone questions.
     */
    private val questions: List<MilestoneQuestion> = buildList {
        // ---- 0–3 months ----
        addAll(q(AgeBand.M0_3, AssessmentCategory.GROSS_MOTOR,
            "When lying on their stomach, does your baby lift their head momentarily?",
            "Is your baby able to actively move both arms and legs?",
            "Does your baby's head control improve over time?"))
        addAll(q(AgeBand.M0_3, AssessmentCategory.FINE_MOTOR,
            "Does your baby briefly open their hands?",
            "Does your baby bring their hands toward their mouth?",
            "Does your baby briefly hold an object placed in their hand?"))
        addAll(q(AgeBand.M0_3, AssessmentCategory.COMMUNICATION,
            "Other than crying, does your baby make any other noises?",
            "Does your baby react to voices they recognise?",
            "Does your baby seem to pay attention when others speak?"))
        addAll(q(AgeBand.M0_3, AssessmentCategory.SOCIAL_EMOTIONAL,
            "Does your baby look at your face?",
            "Does your baby smile responsively?",
            "Does your baby show interest when someone interacts with them?",
            "Does your infant turn their head in the direction of activity or sounds?"))

        // ---- 4–6 months ----
        addAll(q(AgeBand.M4_6, AssessmentCategory.GROSS_MOTOR,
            "Can your baby hold their head steadily?",
            "Can your baby push up on their arms while lying on their tummy?",
            "Can your baby roll from tummy to back?",
            "Does your baby bear some weight through their legs when supported?"))
        addAll(q(AgeBand.M4_6, AssessmentCategory.FINE_MOTOR,
            "Does your baby grasp for toys?",
            "Is your baby able to grab an object?",
            "Does your baby put things in their mouth?"))
        addAll(q(AgeBand.M4_6, AssessmentCategory.COMMUNICATION,
            "Does your baby babble or make repeated sounds?",
            "Does your baby respond when you speak?",
            "Does your baby make sounds to gain attention?"))
        addAll(q(AgeBand.M4_6, AssessmentCategory.SOCIAL_EMOTIONAL,
            "Does your baby laugh or grin when you interact?",
            "Does your baby like staring at familiar faces?",
            "Does your baby show curiosity about the environment and toys?"))

        // ---- 7–9 months ----
        addAll(q(AgeBand.M7_9, AssessmentCategory.GROSS_MOTOR,
            "Can your child sit upright without support, or with only minimal support?",
            "Can your child roll over from back to tummy and tummy to back?",
            "Can your child move from one position to another?",
            "Does your child attempt to crawl or move forward?"))
        addAll(q(AgeBand.M7_9, AssessmentCategory.FINE_MOTOR,
            "Can your child reach out and grasp an object accurately?",
            "Can your child pass a toy from one hand to the other?",
            "Can your child pick up small objects using thumb and fingers?"))
        addAll(q(AgeBand.M7_9, AssessmentCategory.COMMUNICATION,
            "Does your child babble using different sounds?",
            "Does your child respond to their name?",
            "Does your child imitate sounds?"))
        addAll(q(AgeBand.M7_9, AssessmentCategory.SOCIAL_EMOTIONAL,
            "Does your child recognise familiar people?",
            "Does your child show a preference for familiar caregivers?",
            "Does your child participate in games such as peek-a-boo?"))

        // ---- 10–12 months ----
        addAll(q(AgeBand.M10_12, AssessmentCategory.GROSS_MOTOR,
            "Does your child sit without support?",
            "Can your child crawl or move independently from one place to another?",
            "Can your child pull themselves up to stand using support?",
            "Can your child stand while holding onto furniture?"))
        addAll(q(AgeBand.M10_12, AssessmentCategory.FINE_MOTOR,
            "Can your child pick up small objects between thumb and finger?",
            "Can your child release an object intentionally?",
            "Can your child point toward objects?"))
        addAll(q(AgeBand.M10_12, AssessmentCategory.COMMUNICATION,
            "Can your child understand and follow simple instructions, like \"come here\"?",
            "Does your child use gestures such as pointing, waving or nodding?",
            "Does your child make sounds or say words with clear meaning?"))
        addAll(q(AgeBand.M10_12, AssessmentCategory.SOCIAL_EMOTIONAL,
            "Does your child respond to their name?",
            "Does your child imitate simple actions?",
            "Does your child interact with caregivers during play?"))

        // ---- 13–18 months ----
        addAll(q(AgeBand.M13_18, AssessmentCategory.GROSS_MOTOR,
            "Can your child stand independently?",
            "Can your child walk without assistance?",
            "Can your child squat and return to standing?",
            "Can your child climb onto low furniture with supervision?"))
        addAll(q(AgeBand.M13_18, AssessmentCategory.FINE_MOTOR,
            "Can your child pick up small objects using thumb and finger?",
            "Can your child stack two objects, one on top of the other?",
            "Can your child use a spoon to eat, with some assistance?",
            "Can your child scribble on paper using a crayon or pencil?"))
        addAll(q(AgeBand.M13_18, AssessmentCategory.COMMUNICATION,
            "Does your child use meaningful words?",
            "Can your child understand simple instructions?",
            "Does your child point to communicate what they want?",
            "Does your child imitate words or sounds?"))
        addAll(q(AgeBand.M13_18, AssessmentCategory.SOCIAL_EMOTIONAL,
            "Does your child show affection toward familiar people?",
            "Does your child participate in simple interactive games?",
            "Does your child show interest in other children?"))

        // ---- 19–24 months ----
        addAll(q(AgeBand.M19_24, AssessmentCategory.GROSS_MOTOR,
            "Can your child walk independently?",
            "Can your child run?",
            "Can your child climb stairs with assistance?",
            "Can your child kick a ball?"))
        addAll(q(AgeBand.M19_24, AssessmentCategory.FINE_MOTOR,
            "Can your child build a tower using several blocks?",
            "Can your child turn pages of a book?",
            "Can your child make simple marks with a crayon?"))
        addAll(q(AgeBand.M19_24, AssessmentCategory.COMMUNICATION,
            "Does your child use several meaningful words?",
            "Can your child combine two words?",
            "Can your child follow simple two-step instructions?"))
        addAll(q(AgeBand.M19_24, AssessmentCategory.SOCIAL_EMOTIONAL,
            "Does your child communicate wants using words or gestures?",
            "Does your child show increasing independence?",
            "Does your child interact with familiar adults and children?"))

        // ---- 25–36 months ----
        addAll(q(AgeBand.M25_36, AssessmentCategory.GROSS_MOTOR,
            "Can your child run without frequently falling?",
            "Can your child jump with both feet?",
            "Can your child walk up and down stairs with assistance?",
            "Can your child throw a ball?"))
        addAll(q(AgeBand.M25_36, AssessmentCategory.FINE_MOTOR,
            "Can your child copy simple lines?",
            "Can your child stack blocks?",
            "Can your child turn pages individually?"))
        addAll(q(AgeBand.M25_36, AssessmentCategory.COMMUNICATION,
            "Can your child use short sentences?",
            "Can unfamiliar people understand much of what your child says?",
            "Can your child name familiar objects?",
            "Can your child answer simple questions?"))
        addAll(q(AgeBand.M25_36, AssessmentCategory.SOCIAL_EMOTIONAL,
            "Can your child point or show things that interest them?",
            "Can your child interact or play with other children?",
            "Can your child engage in pretend play?"))

        // ---- 37–48 months ----
        addAll(q(AgeBand.M37_48, AssessmentCategory.GROSS_MOTOR,
            "Can your child run, stop and change direction without falling?",
            "Can your child jump forward using both feet?",
            "Can your child climb up and down stairs without assistance?",
            "Can your child stand on one foot briefly?"))
        addAll(q(AgeBand.M37_48, AssessmentCategory.FINE_MOTOR,
            "Can your child draw a simple circle?",
            "Can your child hold a pencil or crayon appropriately?",
            "Can your child manipulate buttons or simple fasteners?"))
        addAll(q(AgeBand.M37_48, AssessmentCategory.COMMUNICATION,
            "Can your child speak in sentences?",
            "Can your child tell you about something that happened?",
            "Can your child answer \"who\", \"what\" and \"where\" questions?"))
        addAll(q(AgeBand.M37_48, AssessmentCategory.SOCIAL_EMOTIONAL,
            "Does your child participate in group play?",
            "Can your child take turns?",
            "Can your child separate from caregivers with increasing ease?"))

        // ---- 49–60 months ----
        addAll(q(AgeBand.M49_60, AssessmentCategory.GROSS_MOTOR,
            "Can your child hop on one foot?",
            "Can your child run and change direction easily?",
            "Can your child catch a large ball?",
            "Can your child climb playground equipment safely?"))
        addAll(q(AgeBand.M49_60, AssessmentCategory.FINE_MOTOR,
            "Can your child draw a person with recognisable body parts?",
            "Can your child copy simple shapes such as a circle, square or cross?",
            "Can your child use scissors to cut paper safely with supervision?",
            "Can your child manage simple fasteners with little or no help?"))
        addAll(q(AgeBand.M49_60, AssessmentCategory.COMMUNICATION,
            "Can your child speak clearly enough for unfamiliar people to understand?",
            "Can your child tell a simple story?",
            "Can your child answer questions about a story?",
            "Can your child follow multi-step instructions?"))
        addAll(q(AgeBand.M49_60, AssessmentCategory.SOCIAL_EMOTIONAL,
            "Can your child play cooperatively with other children?",
            "Can your child follow simple rules?",
            "Can your child express emotions appropriately?",
            "Can your child manage simple daily routines with minimal assistance?"))
    }

    fun forAge(months: Float): List<MilestoneQuestion> {
        val band = AgeBand.forAge(months)
        return questions.filter { it.band == band }
    }

    /** Questions for a category within the child's band. */
    fun forAgeAndCategory(months: Float, category: AssessmentCategory): List<MilestoneQuestion> =
        forAge(months).filter { it.category == category }

    /** Every band up to and including the child's own, youngest first - used for catch-up screening. */
    fun catchUpQuestions(ageMonths: Float, category: AssessmentCategory): List<MilestoneQuestion> {
        val bands = AgeBand.upTo(ageMonths)
        return questions.filter { it.category == category && it.band in bands }
    }

    /** All questions up to a given age, used for the developmental-age estimate. */
    fun upTo(months: Float): List<MilestoneQuestion> =
        questions.filter { it.band.minMonths <= months }

    fun byId(id: String): MilestoneQuestion? = questions.firstOrNull { it.id == id }

    fun all(): List<MilestoneQuestion> = questions
}
