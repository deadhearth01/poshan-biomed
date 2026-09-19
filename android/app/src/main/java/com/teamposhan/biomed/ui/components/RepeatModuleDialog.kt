package com.teamposhan.biomed.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.FontWeight
import com.teamposhan.biomed.ui.haptics.rememberAppHaptics
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Asked when someone opens a module this child has already completed.
 *
 * Without it, reopening a finished module silently resumed the stored session and any new
 * answers overwrote the old ones in place — the earlier result simply disappeared from the
 * child's history with nothing to show it had ever existed.
 *
 * The two outcomes are deliberately not "cancel / continue":
 *
 *  - **See the last result** is the common case. Most reopenings are somebody checking what
 *    was found, not wanting to redo twenty minutes of work.
 *  - **Do it again** starts a genuinely separate assessment. The earlier one is kept, with
 *    its own date, and both appear in the child's timeline. A repeat is a second data point
 *    — often the interesting one, since change over time is what a screening programme is
 *    actually looking for — not a correction of the first.
 */
@Composable
fun RepeatModuleDialog(
    moduleName: String,
    completedAt: Long,
    onViewResult: () -> Unit,
    onRepeat: () -> Unit,
    onDismiss: () -> Unit,
) {
    val haptics = rememberAppHaptics()
    val date = remember(completedAt) {
        Instant.ofEpochMilli(completedAt)
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("d MMM yyyy"))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Already done",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column {
                Text(
                    "$moduleName was completed on $date.",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Gap(10)
                Text(
                    "You can look at what was found, or do it again. Doing it again keeps " +
                        "the earlier result — both are saved, each with its own date.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { haptics.click(); onViewResult() }) {
                Text("See the last result")
            }
        },
        dismissButton = {
            TextButton(onClick = { haptics.confirm(); onRepeat() }) {
                Text("Do it again")
            }
        },
    )
}
