package com.teamposhan.biomed.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class Sex { MALE, FEMALE, OTHER }

@Serializable
enum class UserRole { PARENT, ANGANWADI_WORKER, HEALTHCARE_FACILITY, ADMIN }

@Serializable
enum class AssessmentCategory {
    GROSS_MOTOR,
    FINE_MOTOR,
    COMMUNICATION,
    SOCIAL_EMOTIONAL,
    NUTRITION,
}

@Serializable
enum class ResponseType(
    /**
     * Credit toward the milestone, 0..1. Used for the developmental quotient and for the
     * per-domain concern count, so the weighting is stated here once rather than being
     * re-derived at each call site.
     */
    val score: Float,
    /**
     * Whether this answer marks the question as a red flag in its own right.
     *
     * "Not sure" does. That is deliberate and worth explaining: a caregiver who cannot tell
     * whether their child does something has, in effect, not observed the skill — and for
     * the milestones in this bank, most of which are everyday behaviours a parent would
     * notice, sustained uncertainty is itself worth following up. It is scored as not
     * achieved AND flagged, so it can never be quietly averaged away.
     */
    val flagsConcern: Boolean,
) {
    /** Yes — the child does this. */
    YES_OBSERVED(score = 1.0f, flagsConcern = false),

    /** Maybe — sometimes, inconsistently, or the caregiver has seen it once or twice. */
    MAYBE(score = 0.5f, flagsConcern = false),

    /** No — the child does not do this yet. */
    NOT_YET(score = 0.0f, flagsConcern = false),

    /** Not sure — cannot say. Scored as not achieved and raised as a red flag. */
    NOT_SURE(score = 0.0f, flagsConcern = true);

    /** Counts toward "answered": every option does, including Not sure. */
    val isAnswered: Boolean get() = true

    /** True when the answer indicates the milestone is not securely achieved. */
    val isConcern: Boolean get() = score < 1.0f
}

@Serializable
enum class EvidenceSource { OBSERVATION, CAREGIVER_REPORT }

@Serializable
enum class DomainStatus { ON_TRACK, CONCERN, DELAYED, NOT_ASSESSED }

@Serializable
enum class RiskBand { ON_TRACK, MONITOR, REVIEW, PRIORITY }

@Serializable
enum class SessionStatus { DRAFT, COMPLETED, SYNCED }

@Serializable
enum class AttentionTaskType { PREFERENTIAL_LOOKING, RESPONSE_TO_NAME, DISENGAGEMENT, GENERAL_MOVEMENT }

@Serializable
data class UserProfile(
    val id: String,
    val role: UserRole,
    val displayName: String = "",
    val phone: String = "",
    val relationship: String = "",
    val location: String = "",
    /**
     * Coordinates of the last successful location fix.
     *
     * Kept alongside the human-readable location because the sun-window calculation needs a
     * latitude, and India spans 8 to 37 degrees north — far too wide to assume one.
     */
    val latitude: Double? = null,
    val longitude: Double? = null,
    val anganwadiName: String = "",
    val anganwadiCode: String = "",
    val facilityName: String = "",
    val hfrId: String = "",
    val employeeId: String = "",
    val isGuest: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),

    /**
     * A doctor or health worker the parent chooses to send reports to.
     *
     * Entered by the parent, stored on this device only, and used for nothing except
     * addressing a message the parent themselves sends. The app never contacts this number
     * on its own — an app that silently messages a clinician about a child would be sharing
     * health data without anyone deciding to.
     */
    val doctorName: String = "",
    val doctorPhone: String = "",
)

@Serializable
data class ChildProfile(
    val id: String,
    val name: String,
    val dateOfBirthEpochDay: Long,
    val sex: Sex,
    val bornPremature: Boolean = false,
    val gestationalAgeWeeks: Int? = null,
    val parentName: String = "",
    val parentMobile: String = "",
    val village: String = "",
    /** Baseline anthropometry captured when the child profile is created. */
    val baselineWeightKg: Float? = null,
    val baselineHeightCm: Float? = null,
    val baselineMuacMm: Float? = null,
    val createdByRole: UserRole = UserRole.PARENT,
    val createdAt: Long = System.currentTimeMillis(),
    /**
     * One-off nutrition background and risk factors, keyed by question id.
     *
     * Stored on the child because these describe circumstances — birth order, household
     * water, maternal education — that do not change between a 9-month and a 12-month
     * visit.
     */
    val nutritionBackground: Map<String, ResponseType> = emptyMap()
)

@Serializable
data class AssessmentAnswer(
    val questionId: String,
    val category: AssessmentCategory,
    val response: ResponseType? = null,
    val evidence: EvidenceSource = EvidenceSource.OBSERVATION,
    val updatedAt: Long = System.currentTimeMillis(),
) {
    val isConcern: Boolean
        get() = response == ResponseType.NOT_YET || response == ResponseType.NOT_SURE
    val isUnresolved: Boolean
        get() = response == null
}

@Serializable
data class GrowthMeasurement(
    val dateMillis: Long = System.currentTimeMillis(),
    val weightKg: Float? = null,
    val heightCm: Float? = null,
    /** Mid-upper arm circumference in millimetres (WHO/India community screening). */
    val muacMm: Float? = null,
)

/** MUAC-based acute malnutrition classification (6–59 months). */
@Serializable
enum class MuacBand { NORMAL, MAM, SAM, NOT_APPLICABLE }

@Serializable
data class GrowthFinding(
    val label: String,
    val detail: String,
    val severity: DomainStatus,
)

@Serializable
data class GrowthZ(
    val weightForAge: Float? = null,
    val heightForAge: Float? = null,
    val bmiForAge: Float? = null,
    /** Arm circumference-for-age z; MUAC is also read against the fixed 115/125 mm cut-offs. */
    val muacForAge: Float? = null,
) {
    val worst: Float?
        get() = listOfNotNull(weightForAge, heightForAge, bmiForAge).minOrNull()
}

@Serializable
data class NutritionData(
    val feedingStatus: String = "",
    val foodGroups: List<String> = emptyList(),
    val mealsPerDay: Int? = null,
    val supplements: List<String> = emptyList(),
    val feedingDifficulty: Boolean = false,
    val recentIllness: Boolean = false,
    val foodAccessConcern: Boolean = false,
    val dietaryDiversityScore: Int = 0,
)

@Serializable
data class AttentionResult(
    val taskType: AttentionTaskType = AttentionTaskType.PREFERENTIAL_LOOKING,
    val validFrameRatio: Float = 0f,
    val onScreenRatio: Float = 0f,
    val socialAttentionRatio: Float = 0f,
    val blinkRatePerMin: Float = 0f,
    val disengagementLatencyMs: Float? = null,
    val responseToNameRatio: Float? = null,
    val engagementIndex: Float = 0f,
    val band: RiskBand = RiskBand.ON_TRACK,
    val inconclusive: Boolean = true,
    val durationMs: Long = 0L,

    // ---- probes that were being computed and then discarded -----------------
    //
    // The recorder produces all of these; the conversion to this class simply did not carry
    // them, so three probes' worth of work was thrown away at the last step — run on the
    // child, scored, and never stored or reported. Every field defaults, so older records
    // deserialize unchanged.

    /**
     * Joint attention: share of gaze-following trials where the first fixation landed on the
     * cued side. Three trials run in every band and none of it reached the record.
     */
    val gazeFollowRatio: Float? = null,
    val gazeFollowLatencyMs: Float? = null,

    /**
     * Share of pair-trial looking time on the geometric side.
     *
     * Not merely the complement of [socialAttentionRatio]: time looking at neither side is
     * excluded from both, so the pair does not sum to 1. This is the GeoPref-style contrast
     * and is the half of the pair with published discriminative validity.
     */
    val geometricPreference: Float = 0f,

    /** Response-to-name latency, previously kept only as a ratio. */
    val responseToNameLatencyMs: Float? = null,

    /** Share of disengagement trials where the child shifted at all. */
    val disengagementRatio: Float? = null,

    // ---- smooth pursuit, from the drawn moving target -----------------------
    /** Correlation between gaze and the drawn target's known position. */
    val pursuitCorrelation: Float? = null,
    /** Share of samples with gaze within the on-target radius. */
    val pursuitTrackingFraction: Float? = null,

    // ---- data quality, needed to know whether any of the above is worth reading ----
    /** Overall trustworthiness of this session's data, 0..1. */
    val confidence: Float = 0f,
    /** Trials that met the usable-frames gate, and how many were attempted. */
    val validTrialCount: Int = 0,
    val seenTrialCount: Int = 0,

    /** Sustained attention across the session. */
    val sustainedAttention: Float = 0f,
    /** Mean fixation duration, a basic index of how settled the looking was. */
    val meanFixationMs: Long = 0L,
    val longestFixationMs: Long = 0L,
    /** Head angular travel per second — high values mean a restless or moving child. */
    val headMovementRate: Float = 0f,

    // ---- touch task ---------------------------------------------------------
    /** The bubble game ran and its results were discarded. */
    val touchTargetsShown: Int = 0,
    val touchTargetsPopped: Int = 0,
    val meanTouchLatencyMs: Long? = null,
)

@Serializable
data class VitaminDResult(
    val score: Int = 0,
    val band: RiskBand = RiskBand.ON_TRACK,
    val factors: List<String> = emptyList(),
)

@Serializable
data class CategoryResult(
    val category: AssessmentCategory,
    val answered: Int = 0,
    val total: Int = 0,
    val concerns: Int = 0,
    val status: DomainStatus = DomainStatus.NOT_ASSESSED,
    val redFlags: List<String> = emptyList(),
    /**
     * Milestones from age bands the child has already passed that are still not achieved.
     * These are the delay signal: a 5-month skill missing at 11 months matters far more
     * than one the child is only just due to reach.
     */
    val missedEarlier: List<String> = emptyList(),
    /** The oldest band label containing a miss, for "delayed since ..." messaging. */
    val earliestMissBand: String? = null,
)

@Serializable
data class DevelopmentalQuotient(
    val developmentalAgeMonths: Float = 0f,
    val chronologicalAgeMonths: Float = 0f,
    val dq: Float = 0f,
)

@Serializable
data class CompositeRisk(
    val devScore: Float = 0f,
    val growthScore: Float = 0f,
    val attentionScore: Float = 0f,
    val vitaminDScore: Float = 0f,
    val immunizationScore: Float = 0f,
    val overall: Float = 0f,
    val band: RiskBand = RiskBand.ON_TRACK,
    val reasons: List<String> = emptyList(),
    val referrals: List<String> = emptyList(),
)

@Serializable
data class AssessmentSession(
    val id: String,
    val childId: String,
    val startedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val ageMonthsAtAssessment: Float = 0f,
    val correctedAgeMonths: Float? = null,
    val answers: List<AssessmentAnswer> = emptyList(),
    val growth: GrowthMeasurement? = null,
    val growthZ: GrowthZ? = null,
    val nutrition: NutritionData? = null,
    val attention: AttentionResult? = null,
    val vitaminD: VitaminDResult? = null,
    val givenVaccines: List<String> = emptyList(),
    val dq: DevelopmentalQuotient? = null,
    /**
     * Module 2's caregiver-reported early markers, keyed by question id.
     *
     * Kept alongside the camera result rather than inside it: the two halves of module 2
     * are answered at different moments and either can be completed without the other.
     */
    val markerAnswers: Map<String, ResponseType> = emptyMap(),
    /**
     * Nutrition feeding-practice answers for this visit, keyed by question id.
     *
     * The background risk factors live on [ChildProfile] instead: they describe the
     * household rather than the visit, and re-asking them every few months would waste the
     * caregiver's patience on answers the app already holds.
     */
    val nutritionAnswers: Map<String, ResponseType> = emptyMap(),
    /** Vitamin D sign checks and counselling answers, keyed by question id. */
    val vitaminDAnswers: Map<String, ResponseType> = emptyMap(),
    /** Whether the infant is actually receiving the recommended daily drops. */
    val prophylaxisStatus: String = "NOT_ASKED",
    /**
     * Individual Typology Angle from the skin-tone camera reading, in degrees.
     *
     * A melanin proxy, used only to scale suggested sun-exposure time. It is explicitly not
     * a vitamin D level and must never be reported as one.
     */
    val skinItaDegrees: Double? = null,
    val categoryResults: List<CategoryResult> = emptyList(),
    val composite: CompositeRisk? = null,
    val status: SessionStatus = SessionStatus.DRAFT,
    val assessorRole: UserRole = UserRole.PARENT,
) {
    fun answerFor(questionId: String): AssessmentAnswer? = answers.firstOrNull { it.questionId == questionId }

    val answeredCount: Int
        get() = answers.count { it.response != null }
}
