package com.teamposhan.biomed.domain.attention

import androidx.annotation.DrawableRes
import com.teamposhan.biomed.R

/**
 * The stimulus images module 2 presents, and the rules about which one to show when.
 *
 * Kept out of the UI layer because the choice is a protocol decision, not a styling one: a
 * 2-month-old and a 2-year-old need different stimuli for reasons of visual acuity, not
 * taste, and the reason belongs next to the choice.
 */
object StimulusAssets {

    /**
     * Attention-getters, shown before each trial to pull fixation to a known point.
     *
     * Six variants rather than one, cycled rather than repeated: a target that varies in
     * colour, form and sound between appearances measurably improves infant eye-tracking
     * data quality, while an identical repeat stops working after a few presentations.
     */
    private val getters = intArrayOf(
        R.drawable.ag_star,
        R.drawable.ag_bullseye,
        R.drawable.ag_pinwheel,
        R.drawable.ag_sun,
        R.drawable.ag_flower,
        R.drawable.ag_ball,
    )

    @DrawableRes
    fun getter(index: Int): Int = getters[index.mod(getters.size)]

    val getterCount: Int get() = getters.size

    /**
     * Infant stimuli, split at 3 months.
     *
     * Newborns prefer top-heavy face-*like* patterns — three high-contrast blobs in a
     * triangle — but that generalised preference disappears by about 3 months, after which
     * only real faces hold attention. Showing a schematic pattern to a 6-month-old, or a
     * subtle photograph to a 3-week-old whose acuity is around 20/400, measures nothing.
     */
    @DrawableRes
    fun infantStimulus(ageMonths: Float, index: Int): Int = if (ageMonths < 3f) {
        when (index.mod(3)) {
            0 -> R.drawable.stim_a1_face_pattern
            1 -> R.drawable.stim_a1_checkerboard
            else -> R.drawable.stim_a1_bullseye_bw
        }
    } else {
        R.drawable.stim_a2_mother_face
    }

    /** The non-social half of a paired trial: dynamic geometric motion. */
    @DrawableRes
    val geometric: Int = R.drawable.stim_geopref_geometric

    /**
     * The geometric half, matched to the social half it is paired against.
     *
     * There has to be one per condition, not one shared between both. Matching is a property
     * of a *pair* — same background, same element count, same luminance — so a single
     * geometric image cannot simultaneously match a one-face simple scene and a two-face
     * complex one. Sharing it means at least one of the two conditions is unmatched, and an
     * unmatched pair measures salience rather than social preference.
     *
     * Both currently resolve to the same drawable because the complex partner has not been
     * produced yet; see `design/MASTER_PROMPT_MODULE2_PAIRS.md`. When
     * `stim_geopref_geometric_complex` lands, point the COMPLEX_SOCIAL branch at it — that
     * one line is the whole change.
     */
    @DrawableRes
    fun geometric(condition: AttentionSegment.PairCondition): Int = when (condition) {
        AttentionSegment.PairCondition.SIMPLE_SOCIAL -> R.drawable.stim_geopref_geometric
        AttentionSegment.PairCondition.COMPLEX_SOCIAL -> R.drawable.stim_geopref_geometric
    }

    /**
     * The social half. The complex condition pairs geometry against a busier scene with
     * characters interacting; running both conditions rather than the simple one alone is
     * what lifts the paradigm's sensitivity.
     */
    @DrawableRes
    fun social(condition: AttentionSegment.PairCondition): Int = when (condition) {
        AttentionSegment.PairCondition.SIMPLE_SOCIAL -> R.drawable.stim_geopref_social_simple
        AttentionSegment.PairCondition.COMPLEX_SOCIAL -> R.drawable.stim_geopref_social_complex
    }

    /**
     * Gaze-following cue. Both target sides are visible in the image and the actor looks to
     * one of them, so the cued side is a property of the asset rather than of the layout.
     */
    @DrawableRes
    fun gazeCue(cueOnLeft: Boolean): Int =
        if (cueOnLeft) R.drawable.stim_gaze_cue_left else R.drawable.stim_gaze_cue_right

    /**
     * Backdrop for response-to-name trials.
     *
     * Deliberately low-demand: anything competing for attention makes a failure to turn
     * uninterpretable, because it cannot be distinguished from simple absorption.
     */
    @DrawableRes
    val responseToName: Int = R.drawable.stim_response_to_name
}
