package com.teamposhan.biomed.domain.attention

import kotlin.math.abs
import kotlin.math.ln

/**
 * Turns a stream of [GazeSample]s plus stimulus events into screening metrics.
 *
 * Metrics implemented (all from the published eye-tracking literature, restricted to the
 * ones that are actually feasible on a phone front camera):
 *  - visual engagement: proportion of valid frames the child spends attending the screen
 *  - social preference: dwell on the face half vs the object half, sides counterbalanced
 *  - sustained attention: mean fixation duration via streaming I-DT
 *  - scan organisation: gaze entropy over the AOIs + shift rate between them
 *  - response to name: head-turn / gaze shift inside the name-call window
 *  - attentional disengagement: latency to leave the centre for a peripheral target
 *  - blink rate as an alertness / fatigue signal
 */
class AttentionRecorder(
    private val dispersionThreshold: Float = 0.055f,
    private val minFixationMs: Long = 120L,
    private val onScreenYaw: Float = 0.42f,
    private val onScreenPitch: Float = 0.42f,
    private val nameTurnThreshold: Float = 0.18f,
    private val disengageThreshold: Float = 0.17f,
) {

    private data class NameWindow(val startMs: Long, val baselineYaw: Float, var responded: Boolean = false, var latencyMs: Long? = null, var closed: Boolean = false)
    private data class DisengageWindow(val startMs: Long, val targetOnLeft: Boolean, var latencyMs: Long? = null, var closed: Boolean = false)
    private data class GazeFollowWindow(val startMs: Long, val cueOnLeft: Boolean, var correct: Boolean? = null, var latencyMs: Long? = null, var closed: Boolean = false)

    // --- head movement (SenseToKnow head-movement rate / complexity) ------------
    private var headMovementRad = 0f
    private var lastYaw = 0f
    private var lastPitch = 0f
    private var headSamples = 0

    // --- per-trial validity gating (a trial counts only if >= 50% frames usable) --
    private var currentTrial = -1
    private var currentTrialTotal = 0
    private var currentTrialValid = 0
    private val validTrials = mutableSetOf<Int>()
    private val seenTrials = mutableSetOf<Int>()

    private var totalFrames = 0
    private var faceFrames = 0
    private var onScreenFrames = 0
    private var eyesOpenFrames = 0
    private var blinks = 0
    private var wasOpen = true

    private var socialFrames = 0
    private var nonSocialFrames = 0

    // --- saccade dynamics (GeoPref: "number of saccades per second") -------------
    private var saccadeCount = 0
    private var lastSaccadeAtMs = 0L

    // --- touch task (SenseToKnow: bubble-popping game) --------------------------
    private var touchTargetsShown = 0
    private var touchTargetsPopped = 0
    private val touchLatencies = mutableListOf<Long>()

    private val regionCounts = mutableMapOf<GazeRegion, Int>()
    private var lastRegion: GazeRegion? = null
    private var shifts = 0

    private val window = ArrayDeque<GazeSample>()
    private var fixations = 0
    private var fixationDurationTotal = 0L
    private var currentFixationStart: Long? = null
    private var longestFixation = 0L

    private var lastSample: GazeSample? = null
    private var startedAt = 0L
    private var lastTimestamp = 0L

    private val nameWindows = mutableListOf<NameWindow>()
    private val disengageWindows = mutableListOf<DisengageWindow>()
    private val gazeFollowWindows = mutableListOf<GazeFollowWindow>()

    val frames: Int get() = totalFrames
    val faceFramesSeen: Int get() = faceFrames

    fun onSample(sample: GazeSample, aoi: AoiContext?) {
        if (startedAt == 0L) startedAt = sample.tMs
        lastTimestamp = sample.tMs
        totalFrames++

        // per-trial validity accounting
        if (currentTrial >= 0) {
            currentTrialTotal++
            if (sample.facePresent) currentTrialValid++
        }

        if (!sample.facePresent) {
            bump(GazeRegion.AWAY)
            closeFixation()
            window.clear()
            lastSample = sample
            return
        }

        faceFrames++
        val onScreen = abs(sample.yaw) < onScreenYaw && abs(sample.pitch) < onScreenPitch
        if (onScreen) onScreenFrames++

        // head movement: total angular travel over time (SenseToKnow head-movement rate)
        if (headSamples > 0) {
            headMovementRad += abs(sample.yaw - lastYaw) + abs(sample.pitch - lastPitch)
        }
        lastYaw = sample.yaw
        lastPitch = sample.pitch
        headSamples++

        if (sample.eyesOpen) {
            eyesOpenFrames++
            wasOpen = true
        } else {
            if (wasOpen) blinks++
            wasOpen = false
        }

        val region = when {
            !onScreen -> GazeRegion.AWAY
            sample.gazeX < 0.42f -> GazeRegion.LEFT
            sample.gazeX > 0.58f -> GazeRegion.RIGHT
            else -> GazeRegion.CENTRE
        }
        bump(region)

        // Social vs non-social attribution, only while a pair trial is on screen.
        if (aoi != null && onScreen) {
            val lookingSocial = if (aoi.socialOnLeft) {
                sample.gazeX < 0.42f
            } else {
                sample.gazeX > 0.58f
            }
            val lookingNonSocial = if (aoi.socialOnLeft) {
                sample.gazeX > 0.58f
            } else {
                sample.gazeX < 0.42f
            }
            if (lookingSocial) socialFrames++
            if (lookingNonSocial) nonSocialFrames++
        }

        // Streaming I-DT fixation detection
        window.addLast(sample)
        while (window.size > 45) window.removeFirst()
        if (dispersion() < dispersionThreshold) {
            if (currentFixationStart == null) currentFixationStart = window.first().tMs
        } else {
            closeFixation()
        }

        // Saccade detection: a fast, large jump in gaze position between consecutive
        // frames. This mirrors the GeoPref "saccades per second" measure.
        val prev = lastSample
        if (prev != null && prev.facePresent) {
            val dt = (sample.tMs - prev.tMs).coerceAtLeast(1L)
            val dx = abs(sample.gazeX - prev.gazeX)
            val dy = abs(sample.gazeY - prev.gazeY)
            val velocity = (dx + dy) / (dt / 1000f) // screen-widths per second
            val isQuickPhase = dt < 120L && velocity > 1.6f
            val cooledDown = sample.tMs - lastSaccadeAtMs > 80L
            if (isQuickPhase && cooledDown) {
                saccadeCount++
                lastSaccadeAtMs = sample.tMs
            }
        }

        // Event windows
        nameWindows.forEach { w ->
            if (!w.closed) {
                if (abs(sample.yaw - w.baselineYaw) > nameTurnThreshold) {
                    w.responded = true
                    w.latencyMs = sample.tMs - w.startMs
                }
                if (sample.tMs - w.startMs > 4_000L) w.closed = true
            }
        }
        disengageWindows.forEach { w ->
            if (!w.closed && sample.tMs >= w.startMs) {
                val awayFromCentre = abs(sample.gazeX - 0.5f) > disengageThreshold
                if (awayFromCentre) {
                    w.latencyMs = sample.tMs - w.startMs
                }
                if (sample.tMs - w.startMs > 4_000L) w.closed = true
            }
        }
        gazeFollowWindows.forEach { w ->
            if (!w.closed && sample.tMs >= w.startMs) {
                val lookedLeft = sample.gazeX < 0.42f
                val lookedRight = sample.gazeX > 0.58f
                // first fixation to either side decides the trial
                if (w.correct == null && (lookedLeft || lookedRight)) {
                    val wentLeft = lookedLeft
                    w.correct = (wentLeft == w.cueOnLeft)
                    w.latencyMs = sample.tMs - w.startMs
                }
                if (sample.tMs - w.startMs > 3_000L) w.closed = true
            }
        }

        lastSample = sample
    }

    private fun bump(region: GazeRegion) {
        regionCounts[region] = (regionCounts[region] ?: 0) + 1
        if (lastRegion != null && lastRegion != region) shifts++
        lastRegion = region
    }

    private fun dispersion(): Float {
        if (window.size < 3) return 1f
        var minX = 1f; var maxX = 0f; var minY = 1f; var maxY = 0f
        window.forEach {
            if (it.gazeX < minX) minX = it.gazeX
            if (it.gazeX > maxX) maxX = it.gazeX
            if (it.gazeY < minY) minY = it.gazeY
            if (it.gazeY > maxY) maxY = it.gazeY
        }
        return (maxX - minX) + (maxY - minY)
    }

    private fun closeFixation() {
        val start = currentFixationStart ?: return
        val duration = lastTimestamp - start
        if (duration >= minFixationMs) {
            fixations++
            fixationDurationTotal += duration
            if (duration > longestFixation) longestFixation = duration
        }
        currentFixationStart = null
    }

    /** Call at the exact moment the child's name is spoken. */
    fun markNameCall() {
        nameWindows += NameWindow(
            startMs = lastTimestamp,
            baselineYaw = lastSample?.yaw ?: 0f,
        )
    }

    /** Call when the peripheral target appears in a disengagement trial. */
    fun markDisengageTarget(targetOnLeft: Boolean) {
        disengageWindows += DisengageWindow(startMs = lastTimestamp, targetOnLeft = targetOnLeft)
    }

    /** Call the moment the gaze-following face turns to one side. */
    fun markGazeFollowCue(cueOnLeft: Boolean) {
        gazeFollowWindows += GazeFollowWindow(startMs = lastTimestamp, cueOnLeft = cueOnLeft)
    }

    /**
     * Trial validity gating: a trial only counts toward the session if at least half of its
     * frames were usable (SenseToKnow uses >50% usable frames per clip).
     */
    fun beginTrial(index: Int) {
        currentTrial = index
        currentTrialTotal = 0
        currentTrialValid = 0
        seenTrials += index
    }

    fun endTrial() {
        val idx = currentTrial
        if (idx >= 0 && currentTrialTotal > 0) {
            val ratio = currentTrialValid.toFloat() / currentTrialTotal
            if (ratio >= 0.5f) validTrials += idx
        }
        currentTrial = -1
        currentTrialTotal = 0
        currentTrialValid = 0
    }

    // -----------------------------------------------------------------------
    // Touch task (bubble-popping). Mirrors the SenseToKnow touch modality:
    // how many targets the child pops, and how quickly they respond.
    // -----------------------------------------------------------------------

    fun markTouchTargetShown() {
        touchTargetsShown++
    }

    fun recordTouchPop(latencyMs: Long) {
        touchTargetsPopped++
        touchLatencies += latencyMs.coerceIn(0L, 20_000L)
    }

    fun finish(): AttentionMetrics {
        closeFixation()
        val total = totalFrames.coerceAtLeast(1)
        val face = faceFrames.coerceAtLeast(1)
        val durationMs = (lastTimestamp - startedAt).coerceAtLeast(1L)
        val durationMin = durationMs / 60_000f

        val validRatio = faceFrames.toFloat() / total
        val onScreenRatio = onScreenFrames.toFloat() / face
        val eyesRatio = eyesOpenFrames.toFloat() / face

        val socialTotal = (socialFrames + nonSocialFrames)
        val socialPreference = if (socialTotal > 0) socialFrames.toFloat() / socialTotal else 0f
        val geometricPreference = if (socialTotal > 0) nonSocialFrames.toFloat() / socialTotal else 0f
        val saccadeRate = if (durationMin > 0f) saccadeCount / (durationMin * 60f) else 0f

        val shotTotal = regionCounts.values.sum().coerceAtLeast(1)
        val entropy = -regionCounts.values
            .map { it.toFloat() / shotTotal }
            .filter { it > 0f }
            .sumOf { (it * (ln(it) / ln(2f))).toDouble() }
            .let { -it }.toFloat()

        val responded = nameWindows.count { it.responded }
        val nameRatio = if (nameWindows.isEmpty()) null else responded.toFloat() / nameWindows.size
        val nameLatency = nameWindows.mapNotNull { it.latencyMs }.takeIf { it.isNotEmpty() }?.average()?.toFloat()

        val disengageLatencies = disengageWindows.mapNotNull { it.latencyMs }
        val disengageRatio = if (disengageWindows.isEmpty()) null else disengageLatencies.size.toFloat() / disengageWindows.size
        val disengageLatency = disengageLatencies.takeIf { it.isNotEmpty() }?.average()?.toFloat()

        // Joint attention (gaze following)
        val followDecided = gazeFollowWindows.filter { it.correct != null }
        val followRatio = if (gazeFollowWindows.isEmpty()) null else followDecided.count { it.correct == true }.toFloat() / gazeFollowWindows.size
        val followLatency = gazeFollowWindows.mapNotNull { it.latencyMs }.takeIf { it.isNotEmpty() }?.average()?.toFloat()

        val durationSec = (durationMs / 1000f).coerceAtLeast(1f)
        val headMovementRate = if (headSamples > 1) headMovementRad / durationSec else 0f

        val validTrialCount = validTrials.size
        val seenTrialCount = seenTrials.size

        // Session confidence: how much of the session produced trustworthy signal.
        val trialQuality = if (seenTrialCount == 0) 0f else validTrialCount.toFloat() / seenTrialCount
        val confidence = (0.5f * validRatio + 0.5f * trialQuality).coerceIn(0f, 1f)

        val meanFixation = if (fixations == 0) 0L else fixationDurationTotal / fixations
        val sustained = (meanFixation / 1000f).coerceAtMost(4f) / 4f
        val fixationRate = if (durationMin > 0f) fixations / durationMin else 0f
        val shiftRate = if (durationMin > 0f) shifts / durationMin else 0f

        // Composite engagement: attention to screen, alertness, and social orienting.
        var engagement = 0.40f * onScreenRatio + 0.20f * eyesRatio + 0.20f * sustained + 0.20f * socialPreference
        nameRatio?.let { engagement = engagement * 0.85f + 0.15f * it }
        engagement = engagement.coerceIn(0f, 1f)

        val inconclusive = validRatio < 0.55f || faceFrames < 60 || validTrialCount < 6

        return AttentionMetrics(
            totalFrames = totalFrames,
            faceFrames = faceFrames,
            validFrameRatio = validRatio,
            onScreenRatio = onScreenRatio,
            eyesOpenRatio = eyesRatio,
            socialPreference = socialPreference,
            geometricPreference = geometricPreference,
            socialFrames = socialFrames,
            nonSocialFrames = nonSocialFrames,
            saccadeCount = saccadeCount,
            saccadeRatePerSec = saccadeRate,
            touchTargetsShown = touchTargetsShown,
            touchTargetsPopped = touchTargetsPopped,
            meanTouchLatencyMs = touchLatencies.takeIf { it.isNotEmpty() }?.average()?.toLong(),
            blinkRatePerMin = if (durationMin > 0f) blinks / durationMin else 0f,
            fixationCount = fixations,
            meanFixationMs = meanFixation,
            longestFixationMs = longestFixation,
            fixationRatePerMin = fixationRate,
            shiftRatePerMin = shiftRate,
            scanEntropy = entropy,
            responseToNameRatio = nameRatio,
            responseToNameLatencyMs = nameLatency,
            disengagementRatio = disengageRatio,
            disengagementLatencyMs = disengageLatency,
            gazeFollowRatio = followRatio,
            gazeFollowLatencyMs = followLatency,
            headMovementRate = headMovementRate,
            validTrialCount = validTrialCount,
            seenTrialCount = seenTrialCount,
            confidence = confidence,
            sustainedAttention = sustained,
            engagementIndex = engagement,
            inconclusive = inconclusive,
            durationMs = durationMs,
        )
    }
}

data class AttentionMetrics(
    val totalFrames: Int,
    val faceFrames: Int,
    val validFrameRatio: Float,
    val onScreenRatio: Float,
    val eyesOpenRatio: Float,
    val socialPreference: Float,
    /** GeoPref-style contrast: proportion of pair-trial looking time on the geometric side. */
    val geometricPreference: Float,
    val socialFrames: Int,
    val nonSocialFrames: Int,
    /** GeoPref-style saccade dynamics. */
    val saccadeCount: Int,
    val saccadeRatePerSec: Float,
    /** SenseToKnow-style touch task. */
    val touchTargetsShown: Int,
    val touchTargetsPopped: Int,
    val meanTouchLatencyMs: Long?,
    val blinkRatePerMin: Float,
    val fixationCount: Int,
    val meanFixationMs: Long,
    val longestFixationMs: Long,
    val fixationRatePerMin: Float,
    val shiftRatePerMin: Float,
    val scanEntropy: Float,
    val responseToNameRatio: Float?,
    val responseToNameLatencyMs: Float?,
    val disengagementRatio: Float?,
    val disengagementLatencyMs: Float?,
    /** Joint attention: proportion of gaze-following trials where the first fixation was correct. */
    val gazeFollowRatio: Float?,
    val gazeFollowLatencyMs: Float?,
    /** Head angular travel per second (SenseToKnow head-movement rate). */
    val headMovementRate: Float,
    /** Trials that met the >=50%-usable-frames gate, and how many were attempted. */
    val validTrialCount: Int,
    val seenTrialCount: Int,
    /** Overall data-trustworthiness score (0..1). */
    val confidence: Float,
    val sustainedAttention: Float,
    val engagementIndex: Float,
    val inconclusive: Boolean,
    val durationMs: Long,
)
