package com.teamposhan.biomed.domain.markers

import com.teamposhan.biomed.data.model.ResponseType
import com.teamposhan.biomed.data.model.UserRole
import com.teamposhan.biomed.domain.milestones.AgeBand

/**
 * Caregiver-reported early markers, from the *Comprehensive Developmental Guide for Early
 * Autism Identification in the Indian Population*.
 *
 * This is module 2's questionnaire half. The camera measures looking behaviour; these
 * questions capture what a camera cannot — crying quality, sensory reactions, regression,
 * repetitive behaviours, and the eye-contact *decline* between roughly 8 and 24 weeks that
 * the guide (citing Jones & Klin, 2013) identifies as more significant than an initially
 * low level. That decline is below this hardware's optical resolution, so the caregiver is
 * the right instrument for it.
 *
 * ## Polarity — the thing to get right
 *
 * Module 1's questions are all "can your child do X", where **Yes is good**. The guide's
 * tables are phrased the opposite way — *"Does the baby rarely smile, even when you smile at
 * them?"* — where **Yes is a concern**.
 *
 * Dropping those into the same Yes/Maybe/No/Not sure template unchanged would invert every
 * score silently, which is the worst possible failure for a screening instrument: it would
 * produce confident, wrong, *reassuring* results for exactly the children it exists to find.
 *
 * Two safeguards:
 *  1. Skill questions are **rephrased positively** wherever the behaviour is a skill, so the
 *     app's answer semantics stay uniform ("Does your baby smile back when you smile?").
 *  2. Behaviours that cannot be phrased as a skill — hand-flapping, toe-walking, echolalia,
 *     food restriction — keep their natural phrasing and are tagged [Polarity.CONCERN],
 *     which inverts their scoring. The polarity is a property of the question, checked once,
 *     rather than a convention every call site has to remember.
 */
enum class Polarity {
    /** Yes = the child does this = reassuring. Most questions. */
    SKILL,

    /** Yes = the behaviour is present = concerning. Repetitive and sensory items. */
    CONCERN,
}

/**
 * Who is best placed to answer. The guide's "care triad": parents see the home, the
 * Anganwadi worker sees the child in a group, the health worker sees the clinical picture.
 * Conflicting answers are informative — social at home but freezing at the centre points to
 * sensory overload rather than a core social difference.
 */
enum class Observer(val label: String) {
    PARENT("Parent or caregiver"),
    ANGANWADI("Anganwadi worker"),
    HEALTH_WORKER("Health worker"),
    ANY("Anyone"),
}

/** How much weight a "concerning" answer carries. */
enum class MarkerWeight {
    /** Contributes to the marker count. */
    STANDARD,

    /** The guide names this a critical red flag at this age. */
    CRITICAL,

    /**
     * Loss of a previously held skill. The guide calls regression "a neurodevelopmental
     * medical emergency" requiring immediate referral, so it escalates on its own.
     */
    REGRESSION,
}

data class EarlyMarkerQuestion(
    val id: String,
    val band: AgeBand,
    val text: String,
    val polarity: Polarity,
    val observer: Observer,
    val weight: MarkerWeight = MarkerWeight.STANDARD,
    /** Shown under the question — why this is being asked. */
    val rationale: String,
) {
    /**
     * Whether this answer indicates a concern, accounting for polarity.
     *
     * For a SKILL question the logic matches module 1: anything short of a confident yes is
     * a concern. For a CONCERN question it is mirrored — a confident "yes, this happens" is
     * the concern, and "no" is reassuring.
     */
    fun isConcerning(response: ResponseType?): Boolean = when (response) {
        null -> false
        ResponseType.NOT_SURE -> true // uncertain either way is worth following up
        ResponseType.YES_OBSERVED -> polarity == Polarity.CONCERN
        ResponseType.NOT_YET -> polarity == Polarity.SKILL
        ResponseType.MAYBE -> true // partial, in either direction
    }

    /** Concern strength 0..1, so "maybe" does not count as heavily as a definite answer. */
    fun concernScore(response: ResponseType?): Float = when {
        response == null -> 0f
        !isConcerning(response) -> 0f
        response == ResponseType.MAYBE -> 0.5f
        else -> 1f
    }
}

object EarlyMarkerBank {

    private fun q(
        band: AgeBand,
        index: Int,
        text: String,
        polarity: Polarity,
        observer: Observer,
        rationale: String,
        weight: MarkerWeight = MarkerWeight.STANDARD,
    ) = EarlyMarkerQuestion(
        id = "EM_${band.name}_$index",
        band = band,
        text = text,
        polarity = polarity,
        observer = observer,
        weight = weight,
        rationale = rationale,
    )

    private val questions: List<EarlyMarkerQuestion> = buildList {
        // ---- 0-3 months: the survival window and social emergence ----------
        add(q(AgeBand.M0_3, 1,
            "Does your baby settle or turn toward you when they hear your voice?",
            Polarity.SKILL, Observer.PARENT,
            "A consistent lack of orientation toward familiar voices can be an early signal."))
        // Removed: "Is your baby's cry often very high-pitched, flat in tone, or impossible
        // to soothe?"
        //
        // Three problems. It asks a parent to judge acoustic qualities they have no
        // comparison for — almost every parent of a newborn has, at some point, failed to
        // soothe a cry. It is frightening to be asked at all. And the underlying finding is
        // weak: unusual cry acoustics are reported in retrospective samples but have no
        // established screening value prospectively. Numbering below is unchanged so stored
        // answers to the remaining questions keep resolving to the same items.
        add(q(AgeBand.M0_3, 3,
            "Does your baby smile back when you smile at them?",
            Polarity.SKILL, Observer.PARENT,
            "The social smile is expected by 6 to 8 weeks.", MarkerWeight.CRITICAL))
        add(q(AgeBand.M0_3, 4,
            "Does your baby hold your gaze during feeding or play?",
            Polarity.SKILL, Observer.PARENT,
            "Sustained eye contact is expected by about 2 months.", MarkerWeight.CRITICAL))
        add(q(AgeBand.M0_3, 5,
            "Does your baby become very distressed by gentle handling or cuddling?",
            Polarity.CONCERN, Observer.PARENT,
            "Distress specifically on being touched or held suggests early sensory aversion, " +
                "unlike arching that happens after feeds and settles with burping."))
        add(q(AgeBand.M0_3, 6,
            "Does your baby startle very strongly at ordinary household sounds?",
            Polarity.CONCERN, Observer.PARENT,
            "Over-responsiveness to everyday sound is a sensory signal at this age."))
        add(q(AgeBand.M0_3, 7,
            "Does your baby seem not to react at all to loud sounds?",
            Polarity.CONCERN, Observer.PARENT,
            "Under-responsiveness matters as much as over-responsiveness, and also warrants a " +
                "hearing check."))

        // ---- 4-6 months: communication and the eye-contact decline ---------
        add(q(AgeBand.M4_6, 1,
            "Does your baby coo or babble back and forth with you?",
            Polarity.SKILL, Observer.PARENT,
            "Absence of babbling or cooing by 4 to 5 months is a critical red flag in the guide.",
            MarkerWeight.CRITICAL))
        add(q(AgeBand.M4_6, 2,
            "Does your baby look at faces more than at objects such as a ceiling fan or light?",
            Polarity.SKILL, Observer.PARENT,
            "A marked preference for tracking inanimate objects over human faces is an early marker."))
        add(q(AgeBand.M4_6, 3,
            "Does your baby reach out toward you during play?",
            Polarity.SKILL, Observer.PARENT,
            "Reaching toward a caregiver during play is expected in this window."))
        add(q(AgeBand.M4_6, 4,
            "Is your baby's eye contact less frequent or harder to catch than it was a month or two ago?",
            Polarity.CONCERN, Observer.PARENT,
            "This is the single most important question in this set. Eye contact that starts " +
                "typical and then declines between about 8 and 24 weeks carries more weight " +
                "than eye contact that was always low.",
            MarkerWeight.CRITICAL))
        add(q(AgeBand.M4_6, 5,
            "Does your baby arch their back away specifically when held or cuddled, rather than after feeds?",
            Polarity.CONCERN, Observer.PARENT,
            "Arching after a feed that settles with burping is usually reflux. Arching in " +
                "response to contact suggests sensory aversion."))

        // ---- 7-9 months ----------------------------------------------------
        add(q(AgeBand.M7_9, 1,
            "Does your child turn to look at you when you call their name?",
            Polarity.SKILL, Observer.PARENT,
            "Not responding to their name by 9 months is a significant marker.",
            MarkerWeight.CRITICAL))
        add(q(AgeBand.M7_9, 2,
            "Does your child show clear facial expressions — happy, sad, surprised?",
            Polarity.SKILL, Observer.PARENT,
            "A lack of facial expression by 9 months is flagged in the guide."))
        add(q(AgeBand.M7_9, 3,
            "Does your child reach out to be picked up?",
            Polarity.SKILL, Observer.PARENT,
            "From the guide's home monitoring table, 6 to 12 months."))
        add(q(AgeBand.M7_9, 4,
            "Does your child repeatedly flap their hands, rock, or spin?",
            Polarity.CONCERN, Observer.HEALTH_WORKER,
            "Repetitive body movements are on the clinical screen from 6 to 12 months."))

        // ---- 10-12 months: the group-setting checks ------------------------
        add(q(AgeBand.M10_12, 1,
            "Does your child respond to their name when called in a group or a noisy place?",
            Polarity.SKILL, Observer.ANGANWADI,
            "The group setting is the Anganwadi worker's unique vantage point: a child may " +
                "respond at home and not in a group.",
            MarkerWeight.CRITICAL))
        add(q(AgeBand.M10_12, 2,
            "Does your child enjoy games such as peek-a-boo or pat-a-cake?",
            Polarity.SKILL, Observer.ANGANWADI,
            "Lack of interest in interactive games by 12 months is a high-priority sign."))
        add(q(AgeBand.M10_12, 3,
            "Does your child wave \"tata\" or point at things they want?",
            Polarity.SKILL, Observer.ANGANWADI,
            "Absence of gestures such as waving or pointing by 12 months is a high-priority sign.",
            MarkerWeight.CRITICAL))
        add(q(AgeBand.M10_12, 4,
            "Does your child repeatedly flap their hands, rock, or spin?",
            Polarity.CONCERN, Observer.HEALTH_WORKER,
            "Repetitive movements may emerge by 12 months."))
        add(q(AgeBand.M10_12, 5,
            "Does your child prefer watching spinning or moving objects over looking at people?",
            Polarity.CONCERN, Observer.PARENT,
            "A shift away from social stimuli toward the physical environment."))

        // ---- 13-18 months: regression becomes the headline -----------------
        add(q(AgeBand.M13_18, 1,
            "Has your child lost any words, gestures, or social skills they used to have?",
            Polarity.CONCERN, Observer.ANY,
            "The guide calls loss of a previously held skill a neurodevelopmental medical " +
                "emergency needing immediate referral. It is not something a child outgrows.",
            MarkerWeight.REGRESSION))
        add(q(AgeBand.M13_18, 2,
            "Does your child use any meaningful words?",
            Polarity.SKILL, Observer.PARENT,
            "Total absence of early words is flagged from 13 months."))
        add(q(AgeBand.M13_18, 3,
            "Does your child walk only or mostly on their toes?",
            Polarity.CONCERN, Observer.ANY,
            "Persistent toe-walking is listed among motor markers in this window."))
        add(q(AgeBand.M13_18, 4,
            "Does your child become extremely upset when a small routine changes?",
            Polarity.CONCERN, Observer.PARENT,
            "Extreme distress at minor routine change, beyond ordinary toddler frustration."))
        add(q(AgeBand.M13_18, 5,
            "Does your child line up toys or repeat the same action over and over?",
            Polarity.CONCERN, Observer.PARENT,
            "Repetitive actions such as lining up toys or driving a truck into a wall repeatedly."))
        add(q(AgeBand.M13_18, 6,
            "Does your child seek very intense sensations, such as chewing on toys or very tight hugs?",
            Polarity.CONCERN, Observer.PARENT,
            "Sensory seeking is described in this age band."))

        // ---- 19-24 months --------------------------------------------------
        add(q(AgeBand.M19_24, 1,
            "Has your child lost any words, gestures, or social skills they used to have?",
            Polarity.CONCERN, Observer.ANY,
            "Regression at any point requires immediate referral to an RCI-registered " +
                "clinical psychologist.",
            MarkerWeight.REGRESSION))
        add(q(AgeBand.M19_24, 2,
            "Does your child become physically aggressive — biting or hitting — when there is " +
                "a lot of noise, light or activity?",
            Polarity.CONCERN, Observer.ANY,
            "Aggression when overwhelmed by sensory input, rather than in ordinary conflict."))
        add(q(AgeBand.M19_24, 3,
            "Is your child unusually sensitive to loud noises or to clothing textures?",
            Polarity.CONCERN, Observer.HEALTH_WORKER,
            "Hypersensitivity is on the clinical screen as an ongoing observation."))
        add(q(AgeBand.M19_24, 4,
            "Does your child walk only or mostly on their toes?",
            Polarity.CONCERN, Observer.ANY,
            "Persistent toe-walking."))
        add(q(AgeBand.M19_24, 5,
            "Does your child line up toys or repeat the same action over and over?",
            Polarity.CONCERN, Observer.PARENT,
            "Repetitive play patterns."))

        // ---- 25-36, 37-48, 49-60 months: the 2-5 year picture --------------
        listOf(AgeBand.M25_36, AgeBand.M37_48, AgeBand.M49_60).forEach { band ->
            add(q(band, 1,
                "Has your child lost any words, gestures, or social skills they used to have?",
                Polarity.CONCERN, Observer.ANY,
                "Regression at any age is a medical emergency, not a phase.",
                MarkerWeight.REGRESSION))
            add(q(band, 2,
                "Does your child repeat words or phrases out of context?",
                Polarity.CONCERN, Observer.ANY,
                "Echolalia — repeating phrases out of the situation they belong to."))
            add(q(band, 3,
                "Does your child play pretend games, such as feeding a doll or driving a toy car?",
                Polarity.SKILL, Observer.PARENT,
                "Little or no interest in pretend or imaginative play is a marker from 2 years."))
            add(q(band, 4,
                "Does your child freeze or avoid other children in a group?",
                Polarity.CONCERN, Observer.ANGANWADI,
                "Behaviour in a group setting that a parent may never see at home."))
            add(q(band, 5,
                "Will your child eat only one texture or colour of food?",
                Polarity.CONCERN, Observer.PARENT,
                "Sensory-based food restriction, distinct from ordinary toddler fussiness."))
            add(q(band, 6,
                "Does your child clearly prefer to be alone?",
                Polarity.CONCERN, Observer.PARENT,
                "A clear and consistent preference for solitude."))
            add(q(band, 7,
                "Can your child follow simple spoken instructions?",
                Polarity.SKILL, Observer.ANY,
                "Difficulty following simple verbal instructions at this age."))
        }
    }

    fun all(): List<EarlyMarkerQuestion> = questions

    fun forBand(band: AgeBand): List<EarlyMarkerQuestion> = questions.filter { it.band == band }

    /** The child's own band only — these markers are age-specific, not cumulative. */
    fun forAge(ageMonths: Float): List<EarlyMarkerQuestion> = forBand(AgeBand.forAge(ageMonths))

    fun byId(id: String): EarlyMarkerQuestion? = questions.firstOrNull { it.id == id }

    val totalCount: Int get() = questions.size
}

/**
 * The outcome of one early-marker questionnaire.
 *
 * Reported as a count of markers and a referral signal, never as a probability of autism.
 * The guide is explicit that the goal is identifying *risk markers* to trigger timely
 * assessment, not producing a diagnosis — and a single-stage screener of this kind has a
 * low positive predictive value, so a confident-sounding number would mislead.
 */
data class EarlyMarkerResult(
    val answered: Int,
    val total: Int,
    val markerCount: Float,
    val criticalMarkers: List<String>,
    val regressionReported: Boolean,
    val signal: Signal,
) {
    enum class Signal(val label: String, val guidance: String) {
        NONE(
            "No markers noted",
            "Nothing in this set was flagged at this visit. Continue routine monitoring.",
        ),
        SOME(
            "Some markers noted",
            "A few markers were noted. Review these at the next scheduled visit.",
        ),
        SEVERAL(
            "Several markers noted",
            "Several markers were noted together. Discuss with a health worker and consider " +
                "a developmental assessment.",
        ),
        REFER(
            "Refer for assessment",
            "Refer to a developmental paediatrician and an RCI-registered clinical " +
                "psychologist. Bring this report to the appointment.",
        ),
        NOT_ASSESSED("Not assessed", "This questionnaire has not been completed."),
    }

    companion object {
        fun evaluate(
            questions: List<EarlyMarkerQuestion>,
            answers: Map<String, ResponseType>,
        ): EarlyMarkerResult {
            if (questions.isEmpty() || answers.isEmpty()) {
                return EarlyMarkerResult(0, questions.size, 0f, emptyList(), false, Signal.NOT_ASSESSED)
            }

            var markers = 0f
            val critical = mutableListOf<String>()
            var regression = false

            questions.forEach { q ->
                val response = answers[q.id]
                val score = q.concernScore(response)
                if (score > 0f) {
                    markers += score
                    when (q.weight) {
                        MarkerWeight.CRITICAL -> critical += q.text
                        MarkerWeight.REGRESSION -> {
                            regression = true
                            critical += q.text
                        }
                        MarkerWeight.STANDARD -> Unit
                    }
                }
            }

            val answered = questions.count { answers[it.id] != null }

            // Regression escalates on its own; otherwise the signal rises with the number of
            // markers, scaled to the length of this band's question set.
            val proportion = if (questions.isEmpty()) 0f else markers / questions.size
            val signal = when {
                regression -> Signal.REFER
                critical.size >= 2 -> Signal.REFER
                critical.size == 1 || proportion >= 0.34f -> Signal.SEVERAL
                markers > 0f -> Signal.SOME
                else -> Signal.NONE
            }

            return EarlyMarkerResult(
                answered = answered,
                total = questions.size,
                markerCount = markers,
                criticalMarkers = critical,
                regressionReported = regression,
                signal = signal,
            )
        }
    }
}
