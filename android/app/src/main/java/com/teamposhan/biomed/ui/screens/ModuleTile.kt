package com.teamposhan.biomed.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** Visual state of a module tile on the home plan. */
enum class TileState { DONE, CURRENT, AVAILABLE, LOCKED }

fun tileState(step: Int, currentStep: Int, done: Boolean, unlocked: Boolean): TileState = when {
    done -> TileState.DONE
    step == currentStep -> TileState.CURRENT
    unlocked -> TileState.AVAILABLE
    else -> TileState.LOCKED
}

/**
 * Square module tile used in the "Your screening plan" row. The step the caregiver is
 * currently on is filled and prominent; finished steps get a tick; later steps stay quiet
 * so the sequence Module 1 -> 2 -> 3 is obvious at a glance.
 */
@Composable
fun ModuleTile(
    step: Int,
    title: String,
    subtitle: String,
    state: TileState,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @androidx.annotation.DrawableRes art: Int? = null,
) {
    val container = when (state) {
        TileState.CURRENT -> accent
        TileState.DONE -> MaterialTheme.colorScheme.surfaceContainerHigh
        else -> MaterialTheme.colorScheme.surfaceContainerLow
    }
    val dim = state == TileState.LOCKED

    Column(
        modifier = modifier
            .aspectRatio(0.86f)
            .alpha(if (dim) 0.6f else 1f)
            // Clip first so the press ripple follows the tile's corners.
            .clip(RoundedCornerShape(24.dp))
            .background(container)
            .clickable(enabled = state != TileState.LOCKED, onClick = onClick)
            .padding(12.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(26.dp).background(
                    if (state == TileState.CURRENT) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f)
                    else accent,
                    CircleShape,
                ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    step.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (state == TileState.CURRENT) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface,
                )
            }
            when (state) {
                TileState.DONE -> Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                TileState.LOCKED -> Icon(
                    Icons.Filled.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.outline,
                )
                else -> Spacer(Modifier.width(1.dp))
            }
        }
        Column {
            if (art != null) {
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(art),
                    contentDescription = null,
                    modifier = Modifier.size(46.dp),
                )
                Spacer(Modifier.height(4.dp))
            }
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Start,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Start,
            )
        }
    }
}
