package com.teamposhan.biomed.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A picture for a child who has no photograph.
 *
 * The app never takes or stores a photo of a child, so every list and header was showing the
 * same generic icon. That makes a page of children indistinguishable and makes one child's
 * page feel like nobody's. An illustrated stand-in fixes that without a camera ever being
 * involved.
 *
 * Two rules govern the choice:
 *
 * - **Matched to the child.** Sex as recorded, and the right age — a five-year-old must not
 *   be represented by a swaddled newborn.
 * - **Stable for the life of the child.** Picked from the child's own id, not at random per
 *   render. A face that changes every time the screen redraws is unsettling and makes the
 *   list useless for recognising anybody.
 */
object ChildAvatar {

    /** How many variants exist per stage and sex. Raise this when more artwork arrives. */
    private const val VARIANTS = 3

    /** Coarser than the milestone bands: a picture cannot usefully distinguish 7 from 9 months. */
    private fun stageFor(ageMonths: Float): String = when {
        ageMonths < 6f -> "baby"
        ageMonths < 18f -> "infant"
        ageMonths < 36f -> "toddler"
        else -> "child"
    }

    private fun sexFor(sex: String?): String = when (sex?.lowercase()?.firstOrNull()) {
        'm' -> "boy"
        'f' -> "girl"
        // Neither recorded nor guessable. A neutral set rather than defaulting to one sex,
        // which would misrepresent roughly half of the children it was applied to.
        else -> "child"
    }

    /**
     * The drawable name for this child, e.g. `avatar_girl_toddler_2`.
     *
     * Derived from a hash of the child id so it is stable across sessions, devices and
     * reinstalls, while still spreading a group of children across the available artwork.
     */
    fun artKey(childId: String, sex: String?, ageMonths: Float): String {
        val sexKey = sexFor(sex)
        // The neutral set has a single variant per stage, so hashing across three would
        // point two children in every three at a file that does not exist and silently drop
        // them back to the initial.
        val variants = if (sexKey == "child") 1 else VARIANTS
        val variant = Math.floorMod(childId.hashCode(), variants) + 1
        return "avatar_${sexKey}_${stageFor(ageMonths)}_$variant"
    }
}

/**
 * The child's avatar, falling back to their initial while the artwork does not exist.
 *
 * The initial fallback is not a placeholder box on purpose: a letter in a coloured circle is
 * already a usable way to tell two children apart, so the screen is complete before any
 * illustration is delivered.
 */
@Composable
fun ChildAvatarImage(
    childId: String,
    name: String,
    sex: String?,
    ageMonths: Float,
    modifier: Modifier = Modifier,
    size: Int = 48,
) {
    val res = rememberArtRes(ChildAvatar.artKey(childId, sex, ageMonths))
    val shape = CircleShape

    Box(
        modifier.size(size.dp).clip(shape),
        contentAlignment = Alignment.Center,
    ) {
        if (res != 0) {
            Image(
                painter = painterResource(res),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(size.dp).clip(shape),
            )
        } else {
            Box(
                Modifier
                    .size(size.dp)
                    .clip(shape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    name.trim().firstOrNull()?.uppercase() ?: "?",
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold,
                    fontSize = (size * 0.42f).sp,
                )
            }
        }
    }
}
