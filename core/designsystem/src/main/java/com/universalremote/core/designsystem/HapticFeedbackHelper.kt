package com.universalremote.core.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

class HapticFeedbackHelper(
    private val haptic: HapticFeedback,
    private val isEnabled: Boolean = true
) {
    fun performClick() {
        if (isEnabled) {
            try {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            } catch (_: Exception) {}
        }
    }

    fun performLongPress() {
        if (isEnabled) {
            try {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            } catch (_: Exception) {}
        }
    }
}

@Composable
fun rememberHapticFeedbackHelper(isEnabled: Boolean = true): HapticFeedbackHelper {
    val haptic = LocalHapticFeedback.current
    return remember(haptic, isEnabled) { HapticFeedbackHelper(haptic, isEnabled) }
}
