package com.teamposhan.biomed.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.teamposhan.biomed.data.local.PoshanContainer
import com.teamposhan.biomed.data.model.AssessmentAnswer
import com.teamposhan.biomed.data.model.AssessmentCategory
import com.teamposhan.biomed.data.model.AssessmentSession
import com.teamposhan.biomed.data.model.AttentionResult
import com.teamposhan.biomed.data.model.CategoryResult
import com.teamposhan.biomed.data.model.ChildProfile
import com.teamposhan.biomed.data.model.DevelopmentalQuotient
import com.teamposhan.biomed.data.model.GrowthMeasurement
import com.teamposhan.biomed.data.model.NutritionData
import com.teamposhan.biomed.data.model.ResponseType
import com.teamposhan.biomed.data.model.SessionStatus
import com.teamposhan.biomed.data.model.UserProfile
import com.teamposhan.biomed.data.model.UserRole
import com.teamposhan.biomed.domain.age.AgeCalculator
import com.teamposhan.biomed.domain.attention.AttentionMetrics
import com.teamposhan.biomed.domain.growth.GrowthStandards
import com.teamposhan.biomed.domain.immunization.ImmunizationSchedule
import com.teamposhan.biomed.domain.milestones.AgeBand
import com.teamposhan.biomed.domain.milestones.MilestoneBank
import com.teamposhan.biomed.domain.milestones.MilestoneQuestion
import com.teamposhan.biomed.domain.risk.CompositeRiskEngine
import com.teamposhan.biomed.domain.risk.VitaminDEngine
import com.teamposhan.biomed.domain.risk.VitaminDInput
import com.teamposhan.biomed.domain.risk.AdherenceOutcome
import com.teamposhan.biomed.domain.risk.ProphylaxisAdvice
import com.teamposhan.biomed.domain.risk.ProphylaxisStatus
import com.teamposhan.biomed.domain.risk.RicketsScreenResult
import com.teamposhan.biomed.domain.risk.VitaminDCounselling
import com.teamposhan.biomed.data.model.RiskBand
import com.teamposhan.biomed.data.model.VitaminDResult
import com.teamposhan.biomed.domain.risk.VitaminDRiskModel
import com.teamposhan.biomed.domain.skin.SkinType
import com.teamposhan.biomed.domain.progress.ModuleProgress
import com.teamposhan.biomed.domain.scoring.Scoring
import com.teamposhan.biomed.locale.AppLocale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID

class AppViewModel(private val container: PoshanContainer) : ViewModel() {

    private val repo = container.repository
    private val prefs = container.prefs
    private val auth = container.auth
    private val sync = container.sync
    private val network = container.network

    private val _syncState = MutableStateFlow<com.teamposhan.biomed.data.remote.AppwriteSyncRepository.SyncReport?>(null)
    val syncState: StateFlow<com.teamposhan.biomed.data.remote.AppwriteSyncRepository.SyncReport?> = _syncState.asStateFlow()

    private val _syncing = MutableStateFlow(false)
    val syncing: StateFlow<Boolean> = _syncing.asStateFlow()

    private val _lastGaze = MutableStateFlow<AttentionMetrics?>(null)
    val lastGaze: StateFlow<AttentionMetrics?> = _lastGaze.asStateFlow()

    val children: StateFlow<List<ChildProfile>> = repo.children
    val profile: StateFlow<UserProfile?> = repo.profile
    val sessions: StateFlow<List<AssessmentSession>> = repo.sessions

    private val _language = MutableStateFlow("en")
    val language: StateFlow<String> = _language.asStateFlow()

    /** null until the stored preference has been read, so we can pick the right start screen. */
    private val _onboarded = MutableStateFlow<Boolean?>(null)
    val onboarded: StateFlow<Boolean?> = _onboarded.asStateFlow()

    private val _role = MutableStateFlow(UserRole.PARENT)
    val role: StateFlow<UserRole> = _role.asStateFlow()

    private val _activeChild = MutableStateFlow<ChildProfile?>(null)
    val activeChild: StateFlow<ChildProfile?> = _activeChild.asStateFlow()

    private val _session = MutableStateFlow<AssessmentSession?>(null)
    val session: StateFlow<AssessmentSession?> = _session.asStateFlow()

    init {
        viewModelScope.launch {
            _language.value = prefs.currentLanguage()
            _onboarded.value = prefs.isOnboarded() && repo.profile.value != null
        }
        viewModelScope.launch {
            prefs.role.collect { r ->
                UserRole.entries.firstOrNull { it.name == r }?.let { _role.value = it }
            }
        }
        viewModelScope.launch {
            prefs.activeChildId.collect { id ->
                if (id.isNotBlank()) repo.child(id)?.let { _activeChild.value = it }
            }
        }
        viewModelScope.launch { repo.children.collect { list ->
            val current = _activeChild.value
            if (current == null && list.size == 1) _activeChild.value = list.first()
        } }
    }

    // ---- onboarding -----------------------------------------------------

    fun setLanguage(code: String) {
        _language.value = code
        viewModelScope.launch { prefs.setLanguage(code) }
        // Applies the locale app-wide and recreates the running activity.
        AppLocale.apply(code)
    }

    /** Chosen on the user-type screen before the profile form is shown. */
    fun setRoleHint(role: UserRole) {
        _role.value = role
    }

    fun latestFor(childId: String): AssessmentSession? = repo.latestSessionForChild(childId)

    /**
     * Continuous sync while a live-capture module is running.
     *
     * Module 2 is the one place where a crash or a flat battery loses work that cannot be
     * re-derived — the child has to sit through the whole protocol again — so its results
     * are pushed every few seconds rather than waiting for the 15-minute periodic job.
     *
     * Each tick is a no-op when offline or when a sync is already in flight, so this costs
     * nothing on a device with no connection.
     */
    fun startContinuousSync(intervalMs: Long = 3_000L) {
        if (continuousSyncJob?.isActive == true) return
        continuousSyncJob = viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(intervalMs)
                if (!_syncing.value && network.isOnline()) syncNow()
            }
        }
    }

    fun stopContinuousSync() {
        continuousSyncJob?.cancel()
        continuousSyncJob = null
    }

    private var continuousSyncJob: kotlinx.coroutines.Job? = null

    /** Push the local screening data to Appwrite. Safe to call offline. */
    fun syncNow() {
        if (_syncing.value) return
        viewModelScope.launch {
            _syncing.value = true
            _syncState.value = com.teamposhan.biomed.data.remote.AppwriteSyncRepository.SyncReport(message = "Syncing…")
            val report = runCatching {
                sync.syncAll(
                    profile = repo.profile.value,
                    children = repo.children.value,
                    sessions = repo.sessions.value,
                )
            }.getOrElse {
                com.teamposhan.biomed.data.remote.AppwriteSyncRepository.SyncReport(
                    failed = 1,
                    message = "Offline — will retry automatically",
                )
            }
            _syncState.value = report
            _syncing.value = false
        }
    }

    fun recordGazeMetrics(metrics: AttentionMetrics) {
        _lastGaze.value = metrics
    }

    /** DQ computed live from the answers collected so far, so the reviewer always sees it. */
    fun liveDq(): DevelopmentalQuotient? {
        val session = _session.value ?: return null
        if (session.answers.isEmpty()) return null
        val chrono = chronologicalMonthsOf(session)
        return Scoring.evaluate(session.ageMonthsAtAssessment, chrono, session.answers).dq
    }

    fun completeOnboarding(role: UserRole, profile: UserProfile) {
        _role.value = role
        repo.setProfile(profile)
        _onboarded.value = true
        viewModelScope.launch {
            prefs.setRole(role.name)
            prefs.setConsented(true)
            prefs.setOnboarded(true)
        }
    }

    fun setActiveChild(child: ChildProfile) {
        _activeChild.value = child
        viewModelScope.launch { prefs.setActiveChild(child.id) }
    }

    fun logout() {
        viewModelScope.launch {
            auth.signOut()
            repo.clearProfile()
            prefs.setOnboarded(false)
            prefs.setRole("")
            _activeChild.value = null
            _session.value = null
        }
    }

    // ---- children -------------------------------------------------------

    fun addChild(child: ChildProfile) {
        repo.addChild(child)
        if (_activeChild.value == null) setActiveChild(child)
    }

    fun updateChild(child: ChildProfile) = repo.updateChild(child)

    /** Saves edits to the signed-in person's own profile. */
    fun updateProfile(profile: UserProfile) = repo.setProfile(profile)

    /**
     * Removes a child and everything recorded for them.
     *
     * Their sessions go too. Deleting the child alone would leave sessions pointing at an id
     * that no longer resolves — they would vanish from every screen while still occupying
     * the store and still syncing, and a later child could not reuse the id safely.
     *
     * The active child is cleared when it was the one deleted, otherwise the home screen
     * keeps rendering a profile that is gone.
     */
    fun deleteChild(childId: String) {
        repo.sessionsForChild(childId).forEach { repo.deleteSession(it.id) }
        repo.removeChild(childId)
        if (_activeChild.value?.id == childId) {
            _activeChild.value = repo.children.value.firstOrNull()
            _session.value = null
        }
    }

    fun newChildId(): String = UUID.randomUUID().toString()

    fun ageMonths(child: ChildProfile): Float = AgeCalculator.assessmentMonths(child)
    fun chronologicalMonths(child: ChildProfile): Float = AgeCalculator.chronologicalMonths(child)
    fun correctedMonths(child: ChildProfile): Float? = AgeCalculator.correctedMonths(child)

    // ---- assessment session ---------------------------------------------

    /**
     * Resumes this child's open draft, or starts a new session if there is none.
     *
     * The in-memory session is not sufficient to decide: returning to Home, switching child
     * and coming back, or a process restart all leave `_session` null while a perfectly good
     * draft is still on disk. Guarding only on the in-memory value meant every tap on a
     * module card after that point minted a *fresh empty* session, which then became current
     * and made the completed modules look as though they had reset.
     */
    /**
     * A session is one **visit**, not one module.
     *
     * All three modules write into the same session, so re-opening a module that has already
     * been done has to return the session that holds its answers. It previously did not: the
     * check below was `status == DRAFT`, and finishing module 1 sets the status to COMPLETED,
     * so tapping module 1 again found no draft, minted an empty session and silently
     * discarded everything the visit had collected.
     *
     * The rule is therefore the calendar day rather than the status. Anything started today
     * for this child is the visit in progress, finished or not; tomorrow is a new visit.
     */
    fun startSession(child: ChildProfile, forceNew: Boolean = false): AssessmentSession {
        // forceNew is how "do it again" works. Resuming would overwrite the completed
        // record in place and the earlier result would vanish from the history; minting a
        // fresh session keeps both, each stamped with its own date, so the child's timeline
        // shows a repeat rather than a replacement.
        if (forceNew) return mintSession(child)

        val existing = _session.value
        if (existing != null && existing.childId == child.id && startedToday(existing)) {
            return existing
        }

        val resumable = repo.sessionsForChild(child.id)
            .filter { startedToday(it) }
            .maxByOrNull { it.startedAt }
        // An older unfinished draft is still worth resuming: a visit interrupted yesterday
        // should continue rather than be abandoned half-answered.
            ?: repo.sessionsForChild(child.id)
                .filter { it.status == SessionStatus.DRAFT }
                .maxByOrNull { it.startedAt }

        if (resumable != null) {
            _session.value = resumable
            return resumable
        }

        return mintSession(child)
    }

    /** A brand-new session for this child, persisted and made current. */
    private fun mintSession(child: ChildProfile): AssessmentSession {
        val chrono = chronologicalMonths(child)
        val corrected = correctedMonths(child)
        val session = repo.newSession(child.id).copy(
            ageMonthsAtAssessment = AgeCalculator.assessmentMonths(child),
            correctedAgeMonths = corrected,
            assessorRole = _role.value,
        )
        if (corrected != null && corrected != chrono) {
            // keep chrono via ageMonthsAtAssessment already corrected; store chrono in dq later
        }
        repo.upsertSession(session)
        _session.value = session
        return session
    }

    /**
     * Which of the three modules a given session already holds a completed result for.
     *
     * Read from the stored result rather than from a flag, so it cannot disagree with what
     * the report will show.
     */
    enum class Module { MILESTONES, ATTENTION, VITAMIN_D }

    /**
     * How far through [module] this session is.
     *
     * Replaces the old `moduleDone`, which asked whether a result object existed. Scoring
     * runs on every answer to keep the home screen live, so that returned true after a single
     * question and made a barely-started module look finished.
     */
    fun progressOf(
        session: AssessmentSession?,
        child: ChildProfile?,
        module: Module,
    ): ModuleProgress.Progress {
        val age = child?.let { ageMonths(it) } ?: session?.ageMonthsAtAssessment ?: 12f
        return ModuleProgress.of(session, age, module.toDomain())
    }

    fun moduleDone(session: AssessmentSession?, child: ChildProfile?, module: Module): Boolean =
        progressOf(session, child, module).complete

    private fun Module.toDomain(): ModuleProgress.Module = when (this) {
        Module.MILESTONES -> ModuleProgress.Module.MILESTONES
        Module.ATTENTION -> ModuleProgress.Module.ATTENTION
        Module.VITAMIN_D -> ModuleProgress.Module.VITAMIN_D
    }

    /**
     * When this child last completed [module], or null if never.
     *
     * Searches the whole history, not just the current session: "you already did this" has
     * to be true across visits, otherwise it only ever fires on the same day.
     */
    /**
     * When this child last *finished* [module], or null if never.
     *
     * Only complete runs count. A half-answered module is something to resume, not something
     * to be told you have already done.
     */
    fun lastCompleted(childId: String, module: Module): Long? {
        val child = repo.child(childId)
        return repo.sessionsForChild(childId)
            .filter { moduleDone(it, child, module) }
            .maxOfOrNull { it.completedAt ?: it.startedAt }
    }

    /** Whether this session belongs to the visit happening now. */
    private fun startedToday(session: AssessmentSession): Boolean =
        java.time.Instant.ofEpochMilli(session.startedAt)
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalDate() == java.time.LocalDate.now()

    fun resumeLatestDraft(child: ChildProfile): AssessmentSession? {
        val draft = repo.sessionsForChild(child.id).firstOrNull { it.status == SessionStatus.DRAFT }
        _session.value = draft
        return draft
    }

    /**
     * Every milestone in this category the child should already have reached, oldest band
     * first. A child first seen at 11 months is asked the earlier questions too - that is
     * how a delay becomes visible rather than being skipped over.
     */
    fun questionsFor(category: AssessmentCategory): List<MilestoneQuestion> {
        val age = _session.value?.ageMonthsAtAssessment
            ?: _activeChild.value?.let { ageMonths(it) } ?: 0f
        return MilestoneBank.catchUpQuestions(age, category)
    }

    /** The band the child's own age falls in, for marking earlier questions as catch-up. */
    fun currentBand(): AgeBand {
        val age = _session.value?.ageMonthsAtAssessment
            ?: _activeChild.value?.let { ageMonths(it) } ?: 0f
        return AgeBand.forAge(age)
    }

    fun answer(questionId: String, category: AssessmentCategory, response: ResponseType) {
        val current = _session.value ?: return
        val updated = current.answers.filterNot { it.questionId == questionId } + AssessmentAnswer(
            questionId = questionId,
            category = category,
            response = response,
        )
        // Score on every answer, not only at submit.
        //
        // `finalize()` used to be the only place categoryResults and dq were computed, and
        // it runs only if the caregiver completes the review-and-submit flow. Anyone who
        // answered a category and backed out left the session with empty results — so the
        // home screen showed the module as not started, the child profile showed nothing,
        // and the report had no domains in it, even though every answer was safely on disk.
        // Re-scoring here costs a few milliseconds and keeps every screen truthful.
        persist(rescored(current.copy(answers = updated)))
    }

    /**
     * Recomputes the derived results for a session from its current answers.
     *
     * Deliberately does not set `completedAt` or change the status: this is a live view of
     * an assessment in progress, and marking it complete is still an explicit act.
     */
    private fun rescored(session: AssessmentSession): AssessmentSession {
        if (session.answers.isEmpty()) return session
        val chrono = chronologicalMonthsOf(session)
        val scoring = Scoring.evaluate(session.ageMonthsAtAssessment, chrono, session.answers)
        return session.copy(
            categoryResults = scoring.categories,
            dq = scoring.dq,
        )
    }

    /** Records one nutrition feeding-practice answer against the live session. */
    fun answerNutrition(questionId: String, response: ResponseType) {
        val current = _session.value ?: return
        persist(
            withComposite(
                current.copy(nutritionAnswers = current.nutritionAnswers + (questionId to response))
            )
        )
    }

    /** Records one background risk factor against the child, not the session. */
    fun answerNutritionBackground(questionId: String, response: ResponseType) {
        val child = _activeChild.value ?: return
        val updated = child.copy(
            nutritionBackground = child.nutritionBackground + (questionId to response),
        )
        repo.updateChild(updated)
        _activeChild.value = updated
    }

    /** Records one early-marker answer against the live session. */
    fun answerMarker(questionId: String, response: ResponseType) {
        val current = _session.value ?: return
        persist(current.copy(markerAnswers = current.markerAnswers + (questionId to response)))
    }

    /** Answers recorded so far for the live session. */
    fun markerAnswers(): Map<String, ResponseType> = _session.value?.markerAnswers ?: emptyMap()

    fun saveGrowth(measurement: GrowthMeasurement) {
        val current = _session.value ?: return
        val child = repo.child(current.childId)
        val z = child?.let {
            GrowthStandards.z(
                sex = it.sex,
                ageMonths = current.ageMonthsAtAssessment,
                weightKg = measurement.weightKg,
                heightCm = measurement.heightCm,
                muacMm = measurement.muacMm,
            )
        }
        persist(withComposite(current.copy(growth = measurement, growthZ = z)))
    }

    /** Latest anthropometry for the active child: session data first, then the baseline. */
    fun latestAnthropometry(child: ChildProfile): Triple<Float?, Float?, Float?> {
        val session = repo.latestSessionForChild(child.id)
        val g = session?.growth
        return Triple(
            g?.weightKg ?: child.baselineWeightKg,
            g?.heightCm ?: child.baselineHeightCm,
            g?.muacMm ?: child.baselineMuacMm,
        )
    }

    fun saveNutrition(nutrition: NutritionData) {
        val current = _session.value ?: return
        persist(current.copy(nutrition = nutrition))
    }

    fun saveAttention(result: AttentionResult) {
        val current = _session.value ?: return
        persist(withComposite(current.copy(attention = result)))
    }

    /**
     * Latitude of the last successful location fix, for the offline sun-window calculation.
     *
     * Null when location was never granted or never obtained — the sun card then simply
     * does not appear, rather than guessing a latitude for a country spanning 8 to 37 degrees.
     */
    fun lastKnownLatitude(): Double? = repo.profile.value?.latitude

    /** Records one vitamin D sign-check or counselling answer. */
    fun answerVitaminD(questionId: String, response: ResponseType) {
        val current = _session.value ?: return
        persist(
            withComposite(
                current.copy(vitaminDAnswers = current.vitaminDAnswers + (questionId to response))
            )
        )
    }

    /**
     * Stores a skin-tone reading.
     *
     * Only the ITA angle is kept. The pixel patches are discarded with the frame: they are
     * an image of a child, they add nothing the angle does not already carry, and keeping
     * them would turn an offline screening record into a photo store.
     */
    fun setSkinIta(itaDegrees: Double) {
        val current = _session.value ?: return
        persist(withComposite(current.copy(skinItaDegrees = itaDegrees)))
    }

    /** Records whether the infant is actually receiving the daily drops. */
    fun setProphylaxisStatus(status: ProphylaxisStatus) {
        val current = _session.value ?: return
        persist(withComposite(current.copy(prophylaxisStatus = status.name)))
    }

    /**
     * Completes module 3.
     *
     * The stored [VitaminDResult] is kept for the composite risk engine and the report, but
     * it now carries a referral decision rather than a risk score: the band is PRIORITY when
     * a rickets sign needs urgent attention, REVIEW when any sign was seen, MONITOR when the
     * infant is not receiving the recommended drops, and ON_TRACK otherwise.
     */
    fun completeVitaminD() {
        val current = _session.value ?: return
        val screen = RicketsScreenResult.evaluate(current.vitaminDAnswers)
        val status = runCatching { ProphylaxisStatus.valueOf(current.prophylaxisStatus) }
            .getOrDefault(ProphylaxisStatus.NOT_ASKED)
        val child = repo.child(current.childId)
        val advice = ProphylaxisAdvice.forAge(current.ageMonthsAtAssessment)
        val adherence = AdherenceOutcome.evaluate(status, advice)

        // Rickets signs outrank the risk score: a sign is something seen on the child,
        // whereas the score is a prior built from factors. A present sign must never be
        // softened by a low score.
        val risk = VitaminDRiskModel.assess(
            skinType = SkinType.fromIta(current.skinItaDegrees),
            answers = current.vitaminDAnswers,
            ageMonths = current.ageMonthsAtAssessment,
            prophylaxis = status,
            sunWindowAvailable = null,
        )
        val band = when {
            screen.urgent -> RiskBand.PRIORITY
            screen.refer -> RiskBand.REVIEW
            risk.tier == VitaminDRiskModel.Tier.HIGH -> RiskBand.REVIEW
            adherence.needsFollowUp -> RiskBand.MONITOR
            risk.tier == VitaminDRiskModel.Tier.MODERATE -> RiskBand.MONITOR
            else -> RiskBand.ON_TRACK
        }

        val notes = buildList {
            screen.signsPresent.forEach { add(it.question) }
            if (adherence.needsFollowUp) add(adherence.message)
            risk.present.sortedByDescending { it.points }.forEach { add(it.label) }
            VitaminDCounselling.triggered(current.vitaminDAnswers, current.ageMonthsAtAssessment)
                .forEach { add(it.advice) }
        }

        persist(
            withComposite(
                current.copy(
                    vitaminD = VitaminDResult(
                        // Retained for schema compatibility; no longer a risk score.
                        // Now the risk score rather than a sign count.
                        score = risk.points,
                        band = band,
                        factors = notes,
                    )
                )
            )
        )
    }

    /**
     * Updates the composite risk band from whatever the session currently holds.
     *
     * Called as each module finishes rather than only at the end, so the band shown on the
     * home screen and the child profile reflects what has actually been measured so far.
     */
    private fun withComposite(session: AssessmentSession): AssessmentSession {
        val scored = rescored(session)
        val chrono = chronologicalMonthsOf(scored)
        val scoring = Scoring.evaluate(scored.ageMonthsAtAssessment, chrono, scored.answers)
        val ageDays = (scored.ageMonthsAtAssessment * 30.4375f).toInt()
        val overdue = ImmunizationSchedule.overdueFraction(ageDays, scored.givenVaccines.toSet())
        return scored.copy(
            composite = CompositeRiskEngine.compute(
                categories = scoring.categories,
                redFlags = scoring.redFlags,
                dq = scoring.dq,
                growthZ = scored.growthZ,
                attention = scored.attention,
                vitaminD = scored.vitaminD,
                overdueFraction = overdue,
            ),
        )
    }

    fun toggleVaccine(id: String) {
        val current = _session.value ?: return
        val given = current.givenVaccines.toMutableList()
        if (given.contains(id)) given.remove(id) else given.add(id)
        persist(current.copy(givenVaccines = given))
    }

    fun scoredCategories(): List<com.teamposhan.biomed.data.model.CategoryResult> {
        val current = _session.value ?: return emptyList()
        return Scoring.evaluate(
            assessedAgeMonths = current.ageMonthsAtAssessment,
            chronologicalMonths = chronologicalMonthsOf(current),
            answers = current.answers,
        ).categories
    }

    fun finalize(): AssessmentSession {
        val current = _session.value ?: return AssessmentSession(id = "empty", childId = "")
        val chrono = chronologicalMonthsOf(current)
        val scoring = Scoring.evaluate(current.ageMonthsAtAssessment, chrono, current.answers)
        val child = repo.child(current.childId)
        val ageDays = (current.ageMonthsAtAssessment * 30.4375f).toInt()
        val overdue = ImmunizationSchedule.overdueFraction(ageDays, current.givenVaccines.toSet())

        val composite = CompositeRiskEngine.compute(
            categories = scoring.categories,
            redFlags = scoring.redFlags,
            dq = scoring.dq,
            growthZ = current.growthZ,
            attention = current.attention,
            vitaminD = current.vitaminD,
            overdueFraction = overdue,
        )

        val finalized = current.copy(
            completedAt = System.currentTimeMillis(),
            categoryResults = scoring.categories,
            dq = scoring.dq,
            composite = composite,
            status = SessionStatus.COMPLETED,
        )
        repo.upsertSession(finalized)
        _session.value = finalized
        return finalized
    }

    fun submitCurrent() = finalize()

    private fun chronologicalMonthsOf(session: AssessmentSession): Float {
        val child = repo.child(session.childId) ?: return session.ageMonthsAtAssessment
        return AgeCalculator.chronologicalMonths(child)
    }

    private fun persist(session: AssessmentSession) {
        repo.upsertSession(session)
        _session.value = session
    }
}

class AppViewModelFactory(private val container: PoshanContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = AppViewModel(container) as T
}
