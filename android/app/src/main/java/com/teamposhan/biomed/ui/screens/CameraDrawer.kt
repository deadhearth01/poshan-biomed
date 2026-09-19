package com.teamposhan.biomed.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.teamposhan.biomed.domain.attention.CaptureQuality
import com.teamposhan.biomed.ui.haptics.rememberAppHaptics
import com.teamposhan.biomed.ui.theme.riskColors

/**
 * A small, dismissible camera panel shown over the stimulus.
 *
 * The stimulus fills the screen, which is correct — the child is meant to look at it, and
 * any spare area is a distractor. But that left the operator with no way to see whether the
 * camera had actually found the child's face, and a session that silently tracks nothing
 * looks exactly like a session where the child never looked.
 *
 * So the preview lives in a drawer: open by default so a problem is visible immediately,
 * collapsible to a status pill once the operator is satisfied, and out of the child's line
 * of sight either way.
 */
@Composable
fun CameraDrawer(
    expanded: Boolean,
    quality: CaptureQuality,
    faceDetected: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    preview: @Composable () -> Unit,
) {
    val haptics = rememberAppHaptics()
    val c = riskColors
    val statusColor = when {
        !faceDetected -> c.priority
        quality.usableRatio >= 0.5f -> c.low
        else -> c.monitor
    }

    Column(modifier, horizontalAlignment = Alignment.End) {
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(spring()) + fadeIn(),
            exit = shrinkVertically(spring()) + fadeOut(),
        ) {
            Column(horizontalAlignment = Alignment.End) {
                Box(
                    Modifier
                        .size(width = 132.dp, height = 176.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.Black)
                        .border(2.dp, statusColor, RoundedCornerShape(18.dp)),
                ) {
                    preview()

                    // Status strip along the bottom of the preview: the one thing the
                    // operator needs at a glance is whether tracking is working.
                    Row(
                        Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(Color(0xCC000000))
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(7.dp).background(statusColor, CircleShape))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (faceDetected) "%.0f%%".format(quality.usableRatio * 100) else "No face",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    // Collapse control, inside the panel so it never overlaps the stimulus.
                    Box(
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(Color(0xAA000000))
                            .clickable { haptics.back(); onToggle() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Hide camera preview",
                            tint = Color.White,
                            modifier = Modifier.size(15.dp),
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }

        if (!expanded) {
            // Collapsed: a pill that still carries the tracking state, so hiding the
            // preview never hides a failure.
            Row(
                Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xCC000000))
                    .clickable { haptics.tick(); onToggle() }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    Icons.Filled.Videocam,
                    contentDescription = "Show camera preview",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp),
                )
                Box(Modifier.size(7.dp).background(statusColor, CircleShape))
            }
        }
    }
}

/**
 * The developer tracking view, as a sheet that rises from the bottom.
 *
 * Taking over the whole screen for this was wrong: the stimulus stops playing from the
 * child's point of view, so what is being inspected is no longer the thing that runs during
 * a real session. As a bottom sheet the stimulus keeps running above it, and the layer
 * toggles sit with the view they control rather than under the session buttons.
 */
@Composable
fun TrackingSheet(
    visible: Boolean,
    layers: MeshLayers,
    onToggleLayer: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    preview: @Composable () -> Unit,
) {
    val haptics = rememberAppHaptics()
    AnimatedVisibility(
        visible = visible,
        enter = androidx.compose.animation.slideInVertically(spring()) { it } + fadeIn(),
        exit = androidx.compose.animation.slideOutVertically(spring()) { it } + fadeOut(),
        modifier = modifier,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .padding(16.dp),
        ) {
            // Grab handle, so the sheet reads as a sheet.
            Box(
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.outlineVariant),
            )
            Spacer(Modifier.height(14.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Face tracking",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                Box(
                    Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .clickable { haptics.back(); onClose() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Close tracking view",
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            Spacer(Modifier.height(12.dp))

            // The live mesh, at a size where the tessellation is actually legible.
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black),
            ) {
                preview()
            }

            Spacer(Modifier.height(14.dp))
            Text(
                "Overlay layers",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LayerChip("Mesh", layers.tessellation) { haptics.tick(); onToggleLayer("mesh") }
                LayerChip("Contours", layers.contours) { haptics.tick(); onToggleLayer("contours") }
                LayerChip("Iris", layers.iris) { haptics.tick(); onToggleLayer("iris") }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LayerChip("Pose", layers.poseAxes) { haptics.tick(); onToggleLayer("pose") }
                LayerChip("Gaze", layers.gaze) { haptics.tick(); onToggleLayer("gaze") }
                LayerChip("Stats", layers.hud) { haptics.tick(); onToggleLayer("hud") }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun LayerChip(label: String, active: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (active) Color(0xE600E5FF) else MaterialTheme.colorScheme.surfaceContainerHighest
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
    ) {
        Text(
            label,
            color = if (active) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}
