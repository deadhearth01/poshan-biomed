package com.teamposhan.biomed.domain.progress

import com.teamposhan.biomed.data.model.AssessmentSession
import com.teamposhan.biomed.domain.attention.AttentionBand
import com.teamposhan.biomed.domain.markers.EarlyMarkerBank
import com.teamposhan.biomed.domain.milestones.MilestoneBank
import com.teamposhan.biomed.domain.nutrition.NutritionQuestionBank
import com.teamposhan.biomed.domain.risk.RicketsSign
import com.teamposhan.biomed.domain.risk.VitaminDCounselling

/**
 * How far through each module a child actually is.
 *
 * ## Why this exists
 *
 * "Done" was previously inferred from whether a result object had been written — for module 1,
 * whether `categoryResults` was non-empty. But scoring runs on **every answer** so that the
 * home screen stays live, which meant a single answered question made the module look
 * finished. Reopening it then offered "see the result / do it again" and the obvious third
 * option, *carry on where you left off*, was the one the app could not do.
 *
 * Progress is therefore counted from answers against the question set for this child's age,
 * and "complete" means every question is answered — not that a result exists.
 */
object ModuleProgress {

    enum class Module { MILESTONES, ATTENTION, VITAMIN_D }

    data class Progress(
        val module: Module,
        val answered: Int,
        val total: Int,
        /** Set when the module has an explicit finish step that has been taken. */
        val finalised: Boolean,
    ) {
        val started: Boolean get() = answered > 0
        val fraction: Float get() = if (total == 0) 0f else answered.toFloat() / total

        /**
         * Every question answered, or the module explicitly finished.
         *
         * Both, because the modules differ: module 1 is complete when its questions are, while
         * module 3 has a Save step that records a referral decision and is the real finish.
         */
        val complete: Boolean get() = finalised || (total > 0 && answered >= total)

        /** Started but not finished — the case that should resume silently. */
        val resumable: Boolean get() = started && !complete

        val remaining: Int get() = (total - answered).coerceAtLeast(0)
    }

    /**
     * @param ageMonths the assessment age, which decides the question set — a child is asked
     *   catch-up questions from every earlier band, so the denominator grows with age.
     */
    fun of(
        session: AssessmentSession?,
        ageMonths: Float,
        module: Module,
    ): Progress = when (module) {
        Module.MILESTONES -> milestones(session, ageMonths)
        Module.ATTENTION -> attention(session, ageMonths)
        Module.VITAMIN_D -> vitaminD(session, ageMonths)
    }

    private fun milestones(session: AssessmentSession?, ageMonths: Float): Progress {
        // Every band up to this child's age, which is what the questionnaire actually asks.
        val questions = MilestoneBank.upTo(ageMonths)
        val nutrition = NutritionQuestionBank.forAge(ageMonths)
        val answeredMilestones = session?.answers
            ?.count { it.response != null && questions.any { q -> q.id == it.questionId } }
            ?: 0
        val answeredNutrition = session?.nutritionAnswers
            ?.count { entry -> nutrition.any { it.id == entry.key } }
            ?: 0
        return Progress(
            module = Module.MILESTONES,
            answered = answeredMilestones + answeredNutrition,
            total = questions.size + nutrition.size,
            // Module 1 has no separate finish step beyond answering everything.
            finalised = false,
        )
    }

    private fun attention(session: AssessmentSession?, ageMonths: Float): Progress {
        val markers = EarlyMarkerBank.forAge(ageMonths)
        val answeredMarkers = session?.markerAnswers
            ?.count { entry -> markers.any { it.id == entry.key } }
            ?: 0

        // Below the gaze threshold the module IS the questionnaire — there is no camera task
        // to complete, so counting one would leave it permanently one short of done.
        val cameraApplies = ageMonths >= AttentionBand.GAZE_MIN_MONTHS
        val cameraDone = session?.attention != null

        return Progress(
            module = Module.ATTENTION,
            answered = answeredMarkers + if (cameraApplies && cameraDone) 1 else 0,
            total = markers.size + if (cameraApplies) 1 else 0,
            finalised = false,
        )
    }

    private fun vitaminD(session: AssessmentSession?, ageMonths: Float): Progress {
        val signs = RicketsSign.forAge(ageMonths)
        val counselling = VitaminDCounselling.forAge(ageMonths)
        val answers = session?.vitaminDAnswers.orEmpty()
        val answered = (signs.count { answers.containsKey(it.id) } +
            counselling.count { answers.containsKey(it.id) })

        return Progress(
            module = Module.VITAMIN_D,
            answered = answered,
            total = signs.size + counselling.size,
            // Module 3's Save writes the referral decision; that is its real completion,
            // independent of how many optional counselling items were answered.
            finalised = session?.vitaminD != null,
        )
    }
}
