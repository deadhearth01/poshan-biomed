package com.teamposhan.biomed.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.teamposhan.biomed.ui.haptics.rememberAppHaptics

/**
 * One screen of a walkthrough.
 *
 * [doThis] and [dontDoThis] are separate from [body] on purpose. A caregiver who cannot read
 * fluently still recognises a green tick and a red cross, so the single most important
 * instruction is lifted out of the paragraph and paired with a symbol rather than left buried
 * in a sentence they may not finish.
 */
data class GuideStep(
    /** Drawable base name, resolved at runtime so a missing illustration is not a build break. */
    val artKey: String,
    val title: String,
    val body: String,
    val doThis: String? = null,
    val dontDoThis: String? = null,
)

/**
 * Resolves an illustration by name.
 *
 * By name rather than by `R.drawable.x` so that step content can be written and shipped
 * before the artwork exists: a step with no picture yet shows a neutral placeholder and the
 * app still runs. Wiring each step to a generated `R` constant would mean the whole module
 * fails to compile until the last image lands.
 */
@Composable
fun rememberArtRes(artKey: String): Int {
    val ctx = LocalContext.current
    return remember(artKey) {
        // Tries the exact name first, then any stand-in registered for that slot, so a
        // batch delivered under the real name always takes precedence automatically.
        ArtAliases.candidates(artKey).firstNotNullOfOrNull { name ->
            runCatching {
                ctx.resources.getIdentifier(name, "drawable", ctx.packageName)
            }.getOrNull()?.takeIf { it != 0 }
        } ?: 0
    }
}

/** A step illustration, or a calm placeholder when that image has not been drawn yet. */
@Composable
fun GuideArt(
    artKey: String,
    modifier: Modifier = Modifier,
    ratio: Float = 4f / 3f,
    /**
     * Whether a missing image should still occupy its space.
     *
     * True for a walkthrough step, where the picture is load-bearing and a visible gap is
     * the honest signal that artwork is outstanding. **False for decoration**, where a grey
     * rectangle is worse than nothing: it reads as a broken image rather than as an absent
     * one, and that is exactly how the home banner appeared — a blank box, every time its
     * state had no artwork.
     */
    placeholderWhenMissing: Boolean = true,
) {
    val res = rememberArtRes(artKey)
    when {
        res != 0 -> Image(
            painter = painterResource(res),
            contentDescription = null,
            modifier = modifier.fillMaxWidth().aspectRatio(ratio).clip(RoundedCornerShape(24.dp)),
        )
        placeholderWhenMissing -> Box(
            modifier
                .fillMaxWidth()
                .aspectRatio(ratio)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        )
        // Otherwise draw nothing at all, and take up no space.
    }
}

/**
 * A full-screen, one-instruction-at-a-time walkthrough.
 *
 * Deliberately not a scrolling list of steps. A list lets the reader skim, and skimming is
 * exactly what produces a bad measurement — the phone at the wrong distance, the light behind
 * the child, the card missing from frame. One step per screen, with a picture, forces each
 * instruction to be looked at once.
 */
@Composable
fun GuidedStepsScreen(
    title: String,
    steps: List<GuideStep>,
    onFinish: () -> Unit,
    onExit: () -> Unit,
    why: WhyContent? = null,
    finishLabel: String = "I am ready",
) {
    var index by remember { mutableIntStateOf(0) }
    val haptics = rememberAppHaptics()
    val step = steps.getOrNull(index) ?: return
    val last = index == steps.lastIndex

    Column(Modifier.fillMaxSize()) {
        // --- header: where am I, and how do I get out ---------------------------
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    // Clip first, then clickable: the ripple must stay inside the circle.
                    .clip(CircleShape)
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .clickable {
                        haptics.back()
                        if (index == 0) onExit() else index--
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (index == 0) Icons.Filled.Close else Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = if (index == 0) "Close" else "Back",
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.labelLarge)
                Text(
                    "Step ${index + 1} of ${steps.size}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        StepDots(count = steps.size, current = index)

        // --- the step ------------------------------------------------------------
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        ) {
            Gap(8)
            AnimatedContent(
                targetState = index,
                transitionSpec = {
                    val forward = targetState > initialState
                    val shift = if (forward) 1 else -1
                    (slideInHorizontally(tween(260)) { it / 4 * shift } + fadeIn(tween(200)))
                        .togetherWith(
                            slideOutHorizontally(tween(260)) { -it / 4 * shift } + fadeOut(tween(160))
                        )
                },
                label = "guide-step",
            ) { i ->
                val s = steps[i]
                Column {
                    GuideArt(s.artKey)
                    Gap(18)
                    Text(
                        s.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Gap(8)
                    Text(
                        s.body,
                        // One size up from body default: this is the text that decides
                        // whether the measurement is valid, read in an Anganwadi courtyard.
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    s.doThis?.let { Gap(14); DoRow(it, isDo = true) }
                    s.dontDoThis?.let { Gap(8); DoRow(it, isDo = false) }
                }
            }
            if (why != null) {
                Gap(18)
                WhyButton(why, label = "Why does this matter?")
            }
            Gap(24)
        }

        // --- move on -------------------------------------------------------------
        Row(
            Modifier.padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Both buttons must be weighted.
            //
            // PoshanCard and PoshanOutlinedButton both apply fillMaxWidth internally, so an
            // unweighted one placed in a Row claims the entire width and starves whatever
            // follows it. That is exactly what happened here: from step two onward Back
            // appeared and Next was squeezed to zero width, leaving the walkthrough with no
            // way forward. Weights make the split explicit, and Next keeps the larger share
            // because it is the action the screen is asking for.
            if (index > 0) {
                PoshanOutlinedButton(
                    "Back",
                    modifier = Modifier.weight(1f),
                    onClick = { haptics.back(); index-- },
                )
            }
            Box(Modifier.weight(1.6f)) {
                PoshanPrimaryButton(
                    if (last) finishLabel else "Next",
                    icon = if (last) Icons.Filled.Check else null,
                ) {
                    // Dynamic, not a fixed buzz: each Next is firmer than the last, so the
                    // hand tracks how far through the walkthrough it is without the eye
                    // leaving the picture. The finish is a distinct rising completion.
                    if (last) {
                        haptics.completion(1f)
                        onFinish()
                    } else {
                        haptics.progressTick((index + 1).toFloat() / steps.size)
                        index++
                    }
                }
            }
        }
    }

    // Back-gesture parity with the header button, so leaving never means losing the module.
    androidx.activity.compose.BackHandler(enabled = true) {
        haptics.back()
        if (index == 0) onExit() else index--
    }
}

/** Progress dots. Small, but the only thing telling a reader how much is left. */
@Composable
private fun StepDots(count: Int, current: Int) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        repeat(count) { i ->
            Box(
                Modifier
                    .weight(1f)
                    .height(5.dp)
                    .clip(CircleShape)
                    .background(
                        if (i <= current) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHighest
                        },
                    ),
            )
        }
    }
}

/** A single do-this / not-this line, carried by its symbol as much as its words. */
@Composable
private fun DoRow(text: String, isDo: Boolean) {
    val tint = if (isDo) Color(0xFF2E7D32) else Color(0xFFC62828)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(tint.copy(alpha = 0.10f))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(26.dp).clip(CircleShape).background(tint),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (isDo) Icons.Filled.Check else Icons.Filled.Close,
                contentDescription = if (isDo) "Do this" else "Do not do this",
                tint = Color.White,
                modifier = Modifier.size(16.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

/**
 * A generated micro-element, falling back to a Material icon until the artwork exists.
 *
 * The fallback is the point. Swapping an icon for a picture is an upgrade, not a dependency:
 * every call site keeps working with its old icon and quietly becomes warmer the moment the
 * PNG is dropped in. Nothing has to be re-wired when a batch of artwork lands.
 */
@Composable
fun MicroIcon(
    artKey: String,
    fallback: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    size: Int = 24,
    tint: Color = MaterialTheme.colorScheme.primary,
    contentDescription: String? = null,
) {
    val res = rememberArtRes(artKey)
    if (res != 0) {
        Image(
            painter = painterResource(res),
            contentDescription = contentDescription,
            modifier = modifier.size(size.dp),
        )
    } else {
        Icon(
            fallback,
            contentDescription = contentDescription,
            tint = tint,
            modifier = modifier.size(size.dp),
        )
    }
}
