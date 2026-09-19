package com.teamposhan.biomed.domain.report

import com.teamposhan.biomed.data.model.AssessmentSession
import com.teamposhan.biomed.data.model.ResponseType
import com.teamposhan.biomed.domain.markers.EarlyMarkerBank
import com.teamposhan.biomed.domain.milestones.AgeBand
import com.teamposhan.biomed.domain.milestones.MilestoneBank
import com.teamposhan.biomed.domain.nutrition.NutritionQuestionBank
import com.teamposhan.biomed.domain.risk.RicketsSign
import com.teamposhan.biomed.domain.risk.VitaminDCounselling

/**
 * Every question a child was asked, paired with what was answered.
 *
 * The report previously showed only aggregates — a domain status, a count of concerns. That
 * is enough to triage and not nearly enough to act on or to check: a health worker reading
 * "Communication: some concern" cannot tell which behaviours the parent actually reported,
 * and a parent cannot tell what they said three months ago.
 *
 * Nothing here computes a score. It is a transcript.
 */
object AnswerTranscript {

    /** One question and its answer. */
    data class Entry(
        val questionId: String,
        val text: String,
        val response: ResponseType?,
        /** Extra note shown under the question, such as what to look for. */
        val hint: String? = null,
    ) {
        val answered: Boolean get() = response != null

        /**
         * Whether this answer warrants attention.
         *
         * "Not sure" counts. A caregiver who cannot say whether their child does an everyday
         * thing has, in effect, not seen it — which is a finding, not a missing value.
         */
        val flagged: Boolean get() = response?.let { it.score < 1f } == true

        /** Distinguished from other concerns because it means "unobserved", not "absent". */
        val notSure: Boolean get() = response == ResponseType.NOT_SURE

        val answerLabel: String
            get() = when (response) {
                ResponseType.YES_OBSERVED -> "Yes"
                ResponseType.MAYBE -> "Maybe"
                ResponseType.NOT_YET -> "Not yet"
                ResponseType.NOT_SURE -> "Not sure"
                null -> "Not answered"
            }
    }

    /** A titled run of questions — one milestone domain, one age band, one sign check. */
    data class Section(
        val title: String,
        val subtitle: String? = null,
        val entries: List<Entry>,
    ) {
        val answered: Int get() = entries.count { it.answered }
        val flagged: Int get() = entries.count { it.flagged }
        val notSure: Int get() = entries.count { it.notSure }
        val hasAnything: Boolean get() = answered > 0
    }

    // ---------------------------------------------------------------- module 1

    /**
     * Milestones, grouped by age band.
     *
     * By band rather than by domain because catch-up questions span several bands in one
     * visit, and "which band is this child missing things from" is the question a reviewer
     * actually asks — a skill missed from a band already passed is the delay signal.
     */
    fun milestones(session: AssessmentSession, ageMonths: Float): List<Section> {
        val answers = session.answers.associateBy { it.questionId }
        return MilestoneBank.upTo(ageMonths)
            .groupBy { it.band }
            .toSortedMap(compareBy { it.minMonths })
            .map { (band: AgeBand, questions) ->
                Section(
                    title = band.label,
                    subtitle = null,
                    entries = questions.map { q ->
                        Entry(
                            questionId = q.id,
                            text = q.text,
                            response = answers[q.id]?.response,
                            hint = categoryLabel(q.category.name),
                        )
                    },
                )
            }
            .filter { it.hasAnything }
    }

    fun nutrition(session: AssessmentSession, ageMonths: Float): List<Section> {
        val practice = NutritionQuestionBank.forAge(ageMonths)
        val background = NutritionQuestionBank.backgroundQuestions
        val answers = session.nutritionAnswers
        return listOf(
            Section("Feeding practice", null, practice.map { q ->
                Entry(q.id, q.text, answers[q.id])
            }),
            Section("Household background", null, background.map { q ->
                Entry(q.id, q.text, answers[q.id])
            }),
        ).filter { it.hasAnything }
    }

    // ---------------------------------------------------------------- module 2

    fun markers(session: AssessmentSession, ageMonths: Float): List<Section> {
        val questions = EarlyMarkerBank.forAge(ageMonths)
        val answers = session.markerAnswers
        return listOf(
            Section(
                title = "What the caregiver reported",
                subtitle = AgeBand.forAge(ageMonths).label,
                entries = questions.map { q ->
                    Entry(q.id, q.text, answers[q.id], q.rationale)
                },
            ),
        ).filter { it.hasAnything }
    }

    // ---------------------------------------------------------------- module 3

    fun vitaminD(session: AssessmentSession, ageMonths: Float): List<Section> {
        val answers = session.vitaminDAnswers
        return listOf(
            Section(
                title = "Signs checked",
                subtitle = "India's national child health screening list",
                entries = RicketsSign.forAge(ageMonths).map { sign ->
                    Entry(sign.id, sign.question, answers[sign.id], sign.whatToLookFor)
                },
            ),
            Section(
                title = "Background questions",
                subtitle = null,
                entries = VitaminDCounselling.forAge(ageMonths).map { item ->
                    Entry(item.id, item.question, answers[item.id])
                },
            ),
        ).filter { it.hasAnything }
    }

    private fun categoryLabel(raw: String): String = when (raw) {
        "GROSS_MOTOR" -> "Moving about"
        "FINE_MOTOR" -> "Using the hands"
        "COMMUNICATION" -> "Talking and understanding"
        "SOCIAL_EMOTIONAL" -> "Playing with people"
        "NUTRITION" -> "Eating"
        else -> raw.lowercase().replace('_', ' ')
    }
}
