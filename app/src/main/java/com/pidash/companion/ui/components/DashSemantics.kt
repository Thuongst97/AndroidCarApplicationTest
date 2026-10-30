package com.pidash.companion.ui.components

import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver

/**
 * Custom semantics exposed by dashboard components. They let tests (and accessibility tooling)
 * read logical state that is otherwise only visible as pixels on a Canvas.
 */
object DashSemantics {
    /** Target speed shown by the gauge (not the in-flight animated value). */
    val SpeedKmh = SemanticsPropertyKey<Int>("SpeedKmh")

    /** True while the node holds focus from a rotary controller / D-pad. */
    val RotaryFocused = SemanticsPropertyKey<Boolean>("RotaryFocused")

    /** True when a control is in its active (glowing) state, e.g. Mute engaged. */
    val GlowActive = SemanticsPropertyKey<Boolean>("GlowActive")

    /** True when the speed exceeds the current limit and the gauge is in alert colors. */
    val SpeedAlert = SemanticsPropertyKey<Boolean>("SpeedAlert")
}

var SemanticsPropertyReceiver.speedKmh by DashSemantics.SpeedKmh
var SemanticsPropertyReceiver.rotaryFocused by DashSemantics.RotaryFocused
var SemanticsPropertyReceiver.glowActive by DashSemantics.GlowActive
var SemanticsPropertyReceiver.speedAlert by DashSemantics.SpeedAlert
