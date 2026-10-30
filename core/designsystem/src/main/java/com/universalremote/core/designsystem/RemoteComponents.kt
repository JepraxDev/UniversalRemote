package com.universalremote.core.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.universalremote.core.model.*

/**
 * Header displaying device name, location, favorite toggle, and options menu.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemoteHeader(
    title: String,
    subtitle: String?,
    isFavorite: Boolean,
    onBack: () -> Unit,
    onToggleFavorite: (() -> Unit)? = null,
    onOpenCustomize: (() -> Unit)? = null,
    onOpenQuickActions: (() -> Unit)? = null,
    onOpenLocations: (() -> Unit)? = null
) {
    var menuExpanded by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                if (!subtitle.isNullOrBlank()) {
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
            }
        },
        actions = {
            if (onToggleFavorite != null) {
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Favorito",
                        tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Más opciones")
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    if (onOpenCustomize != null) {
                        DropdownMenuItem(
                            text = { Text("Personalizar control") },
                            onClick = { menuExpanded = false; onOpenCustomize() },
                            leadingIcon = { Icon(Icons.Default.Tune, null) }
                        )
                    }
                    if (onOpenQuickActions != null) {
                        DropdownMenuItem(
                            text = { Text("Acciones rápidas") },
                            onClick = { menuExpanded = false; onOpenQuickActions() },
                            leadingIcon = { Icon(Icons.Default.FlashOn, null) }
                        )
                    }
                    if (onOpenLocations != null) {
                        DropdownMenuItem(
                            text = { Text("Gestionar ubicación") },
                            onClick = { menuExpanded = false; onOpenLocations() },
                            leadingIcon = { Icon(Icons.Default.LocationOn, null) }
                        )
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
    )
}

/**
 * Mode Selector: Modo Básico vs Modo Completo.
 */
@Composable
fun RemoteModeSelector(
    selectedMode: RemoteMode,
    onSelectMode: (RemoteMode) -> Unit
) {
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier.fillMaxWidth()
    ) {
        SegmentedButton(
            selected = selectedMode == RemoteMode.BASIC,
            onClick = { onSelectMode(RemoteMode.BASIC) },
            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
        ) {
            Text("Modo Básico")
        }
        SegmentedButton(
            selected = selectedMode == RemoteMode.FULL,
            onClick = { onSelectMode(RemoteMode.FULL) },
            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
        ) {
            Text("Modo Completo")
        }
    }
}

/**
 * Prominent circular Power button in Crimson Red.
 */
@Composable
fun PowerButton(
    enabled: Boolean,
    supported: Boolean = true,
    onClick: (() -> Unit)?
) {
    val hapticHelper = rememberHapticFeedbackHelper()
    val powerRed = Color(0xFFEF4444)
    val onPowerWhite = Color.White

    Surface(
        shape = CircleShape,
        color = if (supported && enabled && onClick != null) powerRed else powerRed.copy(alpha = 0.4f),
        shadowElevation = 6.dp,
        modifier = Modifier.size(68.dp)
    ) {
        IconButton(
            onClick = {
                hapticHelper.performLongPress()
                onClick?.invoke()
            },
            enabled = enabled && onClick != null && supported,
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(
                Icons.Default.PowerSettingsNew,
                contentDescription = "Encendido / Apagado",
                tint = onPowerWhite,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

/**
 * Circular D-Pad with vibrant Blue OK center button.
 */
@Composable
fun CircularDPad(
    capabilities: DeviceCapabilities? = null,
    enabled: Boolean = true,
    onCommand: ((RemoteCommand) -> Unit)? = null
) {
    fun supports(command: RemoteCommand) = capabilities == null || command in capabilities.supportedCommands
    val hapticHelper = rememberHapticFeedbackHelper()

    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 6.dp,
        modifier = Modifier.size(250.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            contentAlignment = Alignment.Center
        ) {
            DPadDirectionButton(
                icon = Icons.Default.KeyboardArrowUp,
                label = "Arriba",
                supported = supports(RemoteCommand.UP),
                enabled = enabled,
                modifier = Modifier.align(Alignment.TopCenter),
                onClick = onCommand?.let { { it(RemoteCommand.UP) } }
            )
            DPadDirectionButton(
                icon = Icons.Default.KeyboardArrowDown,
                label = "Abajo",
                supported = supports(RemoteCommand.DOWN),
                enabled = enabled,
                modifier = Modifier.align(Alignment.BottomCenter),
                onClick = onCommand?.let { { it(RemoteCommand.DOWN) } }
            )
            DPadDirectionButton(
                icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                label = "Izquierda",
                supported = supports(RemoteCommand.LEFT),
                enabled = enabled,
                modifier = Modifier.align(Alignment.CenterStart),
                onClick = onCommand?.let { { it(RemoteCommand.LEFT) } }
            )
            DPadDirectionButton(
                icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                label = "Derecha",
                supported = supports(RemoteCommand.RIGHT),
                enabled = enabled,
                modifier = Modifier.align(Alignment.CenterEnd),
                onClick = onCommand?.let { { it(RemoteCommand.RIGHT) } }
            )

            val okSupported = supports(RemoteCommand.OK)
            val okEnabled = enabled && onCommand != null && okSupported
            Surface(
                shape = CircleShape,
                color = if (okEnabled) Color(0xFF3B82F6) else Color(0xFF1E3A8A).copy(alpha = 0.5f),
                shadowElevation = 4.dp,
                modifier = Modifier.size(76.dp)
            ) {
                IconButton(
                    onClick = {
                        hapticHelper.performClick()
                        onCommand?.invoke(RemoteCommand.OK)
                    },
                    enabled = okEnabled
                ) {
                    Text(
                        "OK",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun DPadDirectionButton(
    icon: ImageVector,
    label: String,
    supported: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val hapticHelper = rememberHapticFeedbackHelper()
    val isActionEnabled = onClick != null && supported && enabled

    IconButton(
        onClick = {
            hapticHelper.performClick()
            onClick?.invoke()
        },
        enabled = isActionEnabled,
        modifier = modifier.size(52.dp)
    ) {
        Icon(
            icon,
            contentDescription = label,
            modifier = Modifier.size(30.dp),
            tint = if (isActionEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
        )
    }
}

/**
 * Vertical Rocker Control for Volume / Channels.
 */
@Composable
fun VerticalRocker(
    title: String,
    upIcon: ImageVector,
    upLabel: String,
    downIcon: ImageVector,
    downLabel: String,
    upSupported: Boolean,
    downSupported: Boolean,
    enabled: Boolean,
    onUp: (() -> Unit)?,
    onDown: (() -> Unit)?,
    centerIcon: ImageVector? = null,
    centerLabel: String? = null,
    centerSupported: Boolean = true,
    onCenter: (() -> Unit)? = null
) {
    val hapticHelper = rememberHapticFeedbackHelper()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shadowElevation = 2.dp
        ) {
            Column(
                modifier = Modifier.padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = {
                        hapticHelper.performClick()
                        onUp?.invoke()
                    },
                    enabled = enabled && onUp != null && upSupported,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(upIcon, contentDescription = upLabel, modifier = Modifier.size(24.dp))
                }

                if (centerIcon != null && onCenter != null) {
                    HorizontalDivider(Modifier.width(44.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    IconButton(
                        onClick = {
                            hapticHelper.performClick()
                            onCenter.invoke()
                        },
                        enabled = enabled && centerSupported,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(centerIcon, contentDescription = centerLabel, modifier = Modifier.size(20.dp))
                    }
                }

                HorizontalDivider(Modifier.width(44.dp), color = MaterialTheme.colorScheme.outlineVariant)

                IconButton(
                    onClick = {
                        hapticHelper.performClick()
                        onDown?.invoke()
                    },
                    enabled = enabled && onDown != null && downSupported,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(downIcon, contentDescription = downLabel, modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}

/**
 * Section E — Mandatory Numeric Keypad Grid:
 * 1 2 3
 * 4 5 6
 * 7 8 9
 * * 0 #
 */
@Composable
fun NumericKeypad(
    capabilities: DeviceCapabilities?,
    enabled: Boolean,
    onCommand: ((RemoteCommand) -> Unit)?
) {
    fun supports(command: RemoteCommand) = capabilities == null || command in capabilities.supportedCommands
    val hapticHelper = rememberHapticFeedbackHelper()

    val keys = listOf(
        listOf(RemoteCommand.NUM_1 to "1", RemoteCommand.NUM_2 to "2", RemoteCommand.NUM_3 to "3"),
        listOf(RemoteCommand.NUM_4 to "4", RemoteCommand.NUM_5 to "5", RemoteCommand.NUM_6 to "6"),
        listOf(RemoteCommand.NUM_7 to "7", RemoteCommand.NUM_8 to "8", RemoteCommand.NUM_9 to "9"),
        listOf(RemoteCommand.STAR to "*", RemoteCommand.NUM_0 to "0", RemoteCommand.HASH to "#")
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        keys.forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                row.forEach { (cmd, label) ->
                    val isSupported = supports(cmd)
                    val isClickable = enabled && isSupported && onCommand != null

                    Surface(
                        shape = CircleShape,
                        color = if (isClickable) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainer,
                        border = BorderStroke(1.dp, if (isClickable) MaterialTheme.colorScheme.outlineVariant else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                        modifier = Modifier.size(56.dp)
                    ) {
                        IconButton(
                            onClick = {
                                hapticHelper.performClick()
                                onCommand?.invoke(cmd)
                            },
                            enabled = isClickable
                        ) {
                            Text(
                                label,
                                style = MaterialTheme.typography.titleLarge,
                                color = if (isClickable) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Section F — Media Control Panel: Play, Pause, Stop, Previous, Next, Rewind, Fast Forward.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MediaControlPanel(
    capabilities: DeviceCapabilities?,
    enabled: Boolean,
    onCommand: ((RemoteCommand) -> Unit)?
) {
    fun supports(command: RemoteCommand) = capabilities == null || command in capabilities.supportedCommands

    val mediaItems = listOf(
        Triple(RemoteCommand.PLAY, Icons.Default.PlayArrow, "Play"),
        Triple(RemoteCommand.PAUSE, Icons.Default.Pause, "Pausa"),
        Triple(RemoteCommand.STOP, Icons.Default.Stop, "Detener"),
        Triple(RemoteCommand.PREVIOUS, Icons.Default.SkipPrevious, "Anterior"),
        Triple(RemoteCommand.NEXT, Icons.Default.SkipNext, "Siguiente"),
        Triple(RemoteCommand.REWIND, Icons.Default.FastRewind, "Retroceder"),
        Triple(RemoteCommand.FAST_FORWARD, Icons.Default.FastForward, "Avanzar")
    )

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        mediaItems.forEach { (cmd, icon, label) ->
            RemoteButton(
                icon = icon,
                label = label,
                supported = supports(cmd),
                enabled = enabled,
                onClick = onCommand?.let { { it(cmd) } }
            )
        }
    }
}

/**
 * Section G — Projector Input Sources Grid.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProjectorInputGrid(
    capabilities: DeviceCapabilities?,
    enabled: Boolean,
    onCommand: ((RemoteCommand) -> Unit)?
) {
    fun supports(command: RemoteCommand) = capabilities == null || command in capabilities.supportedCommands

    val sources = listOf(
        RemoteCommand.HDMI_1 to "HDMI 1",
        RemoteCommand.HDMI_2 to "HDMI 2",
        RemoteCommand.HDMI to "HDMI",
        RemoteCommand.VGA to "VGA / Computer",
        RemoteCommand.LAN to "LAN / Network",
        RemoteCommand.INPUT to "Auto Source"
    )

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        sources.forEach { (cmd, label) ->
            val isSupported = supports(cmd)
            OutlinedButton(
                onClick = { onCommand?.invoke(cmd) },
                enabled = enabled && onCommand != null && isSupported,
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text(if (isSupported) label else "$label (N/A)")
            }
        }
    }
}

/**
 * Section H — Projector Image & Projection Settings Controls.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProjectorImageControls(
    capabilities: DeviceCapabilities?,
    enabled: Boolean,
    onCommand: ((RemoteCommand) -> Unit)?
) {
    fun supports(command: RemoteCommand) = capabilities == null || command in capabilities.supportedCommands

    val imageCmds = listOf(
        RemoteCommand.ASPECT_RATIO to "Aspecto",
        RemoteCommand.COLOR_MODE to "Modo Color",
        RemoteCommand.ECO_MODE to "Modo ECO",
        RemoteCommand.AUTO_SETUP to "Ajuste Auto",
        RemoteCommand.FREEZE to "Congelar",
        RemoteCommand.AV_MUTE to "A/V Mute"
    )

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        imageCmds.forEach { (cmd, label) ->
            val isSupported = supports(cmd)
            OutlinedButton(
                onClick = { onCommand?.invoke(cmd) },
                enabled = enabled && onCommand != null && isSupported,
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text(if (isSupported) label else "$label (N/A)")
            }
        }
    }
}

/**
 * Section Container with title.
 */
@Composable
fun RemoteSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        content()
    }
}
