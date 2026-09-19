package com.teamposhan.biomed.domain.skin

import kotlin.math.abs

/**
 * Decides when a live skin reading is good enough to freeze.
 *
 * ## Why a lock at all
 *
 * Without one, whatever number happened to be on screen when the operator's thumb landed is
 * the number that gets saved. That is the worst possible sampling rule: the press itself
 * shakes the phone, and a reading taken mid-wobble can sit a whole skin-type band away from
 * the truth. The measured error budget is 1.5 to 2.6 units of L\*, which is 3 to 7 degrees of
 * ITA, against band widths of 13 to 18 degrees in the range where most children sit.
 *
 * So the app behaves like a camera acquiring focus: it watches, decides the reading has
 * settled, locks it, and saves *that* — not whatever the last frame said.
 *
 * ## The four conditions
 *
 * All must hold simultaneously, and all must keep holding for [REQUIRED_FRAMES] consecutive
 * frames. Any one failing resets the run to zero, because a reading that was briefly valid
 * is not a settled reading.
 *
 *  1. **Reference card usable.** Gains actually applied — not a blown, black or absent card.
 *     Without a white point the reading measures the room as much as the child.
 *  2. **Patch is mostly skin.** At least half the reticle survived rejection, and at least
 *     100 pixels remain.
 *  3. **Formula in range.** b\* above 0.5 and L\* within 15..92, so the arctan is defined and
 *     the exposure is neither crushed nor blown.
 *  4. **Stable.** Every ITA in the window sits within [STABILITY_TOLERANCE_DEG] of the
 *     window's mean.
 *
 * ## Choosing the tolerance
 *
 * 1.5 degrees, which is roughly a fifth of the narrowest band and comfortably inside the
 * measurement's own noise floor. Tighter would rarely lock on a moving child; looser would
 * let a drifting reading qualify as settled.
 *
 * ## Choosing the window
 *
 * 12 frames — about half a second at the analyser's 25-30 fps. Long enough that a single
 * clean frame cannot trigger a lock, short enough that an operator holding steady is not
 * left waiting.
 */
class SkinReadingLock {

    sealed interface State {
        /** Not yet acquiring: something is wrong with the frame. */
        data class Waiting(val reason: String) : State

        /** Conditions hold and the run is building. */
        data class Acquiring(val framesHeld: Int, val progress: Float) : State

        /** Settled. [measurement] is frozen and is what will be saved. */
        data class Locked(
            val measurement: SkinMeasurement,
            val itaDegrees: Double,
            /** Spread across the locking window — the reading's own repeatability. */
            val spreadDeg: Double,
        ) : State
    }

    private val window = ArrayDeque<Double>()
    private val frames = ArrayDeque<SkinMeasurement>()
    private var locked: State.Locked? = null

    var state: State = State.Waiting("Starting camera")
        private set

    /**
     * Feeds one frame.
     *
     * Once locked, later frames are ignored until [reset]. A lock that could be silently
     * replaced by a subsequent frame would not be a lock.
     */
    fun offer(m: SkinMeasurement): State {
        locked?.let { return it }

        val blocker = blockingReason(m)
        if (blocker != null) {
            window.clear()
            frames.clear()
            state = State.Waiting(blocker)
            return state
        }

        window.addLast(m.reading.itaDegrees)
        frames.addLast(m)
        while (window.size > REQUIRED_FRAMES) {
            window.removeFirst()
            frames.removeFirst()
        }

        val mean = window.average()
        val spread = window.maxOf { abs(it - mean) }

        // Drifting rather than settling: keep the newest frame and start the run again from
        // it, so a slow pan does not accumulate toward a false lock.
        if (spread > STABILITY_TOLERANCE_DEG) {
            val last = frames.last()
            val lastIta = window.last()
            window.clear(); window.addLast(lastIta)
            frames.clear(); frames.addLast(last)
            state = State.Acquiring(1, 1f / REQUIRED_FRAMES)
            return state
        }

        state = if (window.size >= REQUIRED_FRAMES) {
            // The median frame of the window, not the last: the last is the one most likely
            // to catch the start of a movement.
            val median = frames.sortedBy { it.reading.itaDegrees }[frames.size / 2]
            State.Locked(
                measurement = median,
                itaDegrees = median.reading.itaDegrees,
                spreadDeg = spread,
            ).also { locked = it }
        } else {
            State.Acquiring(window.size, window.size.toFloat() / REQUIRED_FRAMES)
        }
        return state
    }

    /** Clears the lock so the operator can take another reading. */
    fun reset() {
        locked = null
        window.clear()
        frames.clear()
        state = State.Waiting("Ready")
    }

    /** Why this frame cannot contribute, in the operator's words, or null if it can. */
    private fun blockingReason(m: SkinMeasurement): String? = when {
        !m.usedCard -> "Put the grey card in the right-hand box"
        !m.skinPatch.usable -> "Fill the left box with bare skin"
        !m.reading.reliable -> m.reading.note ?: "Move into better light"
        else -> null
    }

    companion object {
        /** Consecutive good frames required. ~0.5 s at the analyser's frame rate. */
        const val REQUIRED_FRAMES = 12

        /** Maximum spread, in degrees of ITA, for the window to count as settled. */
        const val STABILITY_TOLERANCE_DEG = 1.5
    }
}
