package com.universalremote.core.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.universalremote.core.model.*

/**
 * Visual & interactive remote control button.
 * When [onClick] is null, it acts as a visual-only disabled preview (demo mode).
 */
@Composable
fun RemoteButton(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    supported: Boolean = true,
    accent: Boolean = false,
    enabled: Boolean = supported,
    onClick: (() -> Unit)? = null,
) {
    val isActionEnabled = onClick != null && supported && enabled
    val hapticHelper = rememberHapticFeedbackHelper()
    val description = stringResource(
        if (!supported) R.string.unsupported_command
        else if (onClick != null) R.string.command_available
        else R.string.demo_short
    )
    OutlinedIconButton(
        onClick = {
            hapticHelper.performClick()
            onClick?.invoke()
        },
        enabled = isActionEnabled,
        modifier = modifier.size(56.dp).semantics { stateDescription = description },
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = IconButtonDefaults.outlinedIconButtonColors(
            containerColor = if (accent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = if (accent) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
            disabledContainerColor = if (accent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
            disabledContentColor = if (supported) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Icon(icon, contentDescription = label, modifier = Modifier.size(26.dp))
    }
}

@Composable
fun RemoteDPad(
    capabilities: DeviceCapabilities? = null,
    onCommand: ((RemoteCommand) -> Unit)? = null,
    enabled: Boolean = true
) {
    fun supports(command: RemoteCommand) = capabilities == null || command in capabilities.supportedCommands
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 2.dp
    ) {
        Box(Modifier.size(252.dp).padding(12.dp), contentAlignment = Alignment.Center) {
            RemoteButton(
                Icons.Default.KeyboardArrowUp,
                stringResource(R.string.up),
                Modifier.align(Alignment.TopCenter),
                supported = supports(RemoteCommand.UP),
                enabled = enabled,
                onClick = onCommand?.let { { it(RemoteCommand.UP) } }
            )
            RemoteButton(
                Icons.Default.KeyboardArrowDown,
                stringResource(R.string.down),
                Modifier.align(Alignment.BottomCenter),
                supported = supports(RemoteCommand.DOWN),
                enabled = enabled,
                onClick = onCommand?.let { { it(RemoteCommand.DOWN) } }
            )
            RemoteButton(
                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                stringResource(R.string.left),
                Modifier.align(Alignment.CenterStart),
                supported = supports(RemoteCommand.LEFT),
                enabled = enabled,
                onClick = onCommand?.let { { it(RemoteCommand.LEFT) } }
            )
            RemoteButton(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                stringResource(R.string.right),
                Modifier.align(Alignment.CenterEnd),
                supported = supports(RemoteCommand.RIGHT),
                enabled = enabled,
                onClick = onCommand?.let { { it(RemoteCommand.RIGHT) } }
            )
            val okSupported = supports(RemoteCommand.OK)
            val okActionEnabled = onCommand != null && okSupported && enabled
            val description = stringResource(
                if (!okSupported) R.string.unsupported_command
                else if (onCommand != null) R.string.command_available
                else R.string.demo_short
            )
            val hapticHelper = rememberHapticFeedbackHelper()
            FilledTonalButton(
                onClick = {
                    hapticHelper.performClick()
                    onCommand?.invoke(RemoteCommand.OK)
                },
                enabled = okActionEnabled,
                modifier = Modifier.size(76.dp).semantics { stateDescription = description },
                shape = CircleShape,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    disabledContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    disabledContentColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f)
                )
            ) { Text(stringResource(R.string.ok)) }
        }
    }
}
