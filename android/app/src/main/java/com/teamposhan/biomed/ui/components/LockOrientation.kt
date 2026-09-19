package com.teamposhan.biomed.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext

/**
 * Forces a screen into one orientation for as long as it is composed.
 *
 * Used by the module 2 session, which is landscape-only.
 *
 * ## Why module 2 is landscape
 *
 * The paired-preference measurement asks which of two simultaneous options the child chooses,
 * so each option has to be physically large enough to process. In portrait, each half of a
 * 6.1" screen subtends **4.9 degrees** at 40 cm and a face inside it about **3.4** — below
 * the roughly 5 to 8 degrees infant face processing needs. Landscape roughly doubles it, to
 * **10.8 degrees** per half. That is also close to what the Duke tablet setup achieves with
 * an iPad at 60 cm, which is the configuration the published numbers come from.
 *
 * The orientation is restored on the way out, so the rest of the app stays portrait.
 */
@Composable
fun LockLandscape() = LockOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)

@Composable
private fun LockOrientation(orientation: Int) {
    val context = LocalContext.current
    DisposableEffect(orientation) {
        val activity = context.findActivity()
        val previous = activity?.requestedOrientation
        activity?.requestedOrientation = orientation
        onDispose {
            // Unspecified rather than portrait: the app's own manifest setting should decide
            // what "normal" is, not this screen.
            activity?.requestedOrientation =
                previous ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }
}

/** Walks the context chain to the hosting Activity, which Compose does not hand us directly. */
private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
