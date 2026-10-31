package com.universalremote.feature.remote

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.universalremote.core.model.IrValidationStatus
import com.universalremote.core.model.RemoteCommand

/**
 * Commands that the loaded IR profile actually contains. Keys for anything
 * else render disabled so the UI never pretends an uncaptured button works.
 */
val LocalEpsonSupportedCommands = staticCompositionLocalOf<Set<RemoteCommand>> {
    RemoteCommand.entries.toSet()
}

/** Premium dark-navy palette requested for the Epson remote. */
object EpsonRemoteColors {
    val Navy = Color(0xFF071426)
    val NavyDeep = Color(0xFF040C18)
    val Surface = Color(0xFF12233A)
    val SurfaceRaised = Color(0xFF1A304C)
    val SurfaceSoft = Color(0xFF223D5E)
    val Accent = Color(0xFF4DA3FF)
    val AccentSoft = Color(0xFF1C4E80)
    val Text = Color(0xFFF2F7FF)
    val TextMuted = Color(0xFFA9BCD6)
    val Power = Color(0xFFE23B3B)
    val PowerDark = Color(0xFF8E1F1F)
    val Success = Color(0xFF57D6A0)
    val Warning = Color(0xFFFFC857)
    val Error = Color(0xFFFF7B7B)
}

@Composable
fun EpsonRemoteSection(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(EpsonRemoteColors.Surface, RoundedCornerShape(24.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = title,
            color = EpsonRemoteColors.Text,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.4.sp,
        )
        if (subtitle != null) {
            Text(subtitle, color = EpsonRemoteColors.TextMuted, fontSize = 12.sp, lineHeight = 17.sp)
        }
        content()
    }
}

@Composable
fun EpsonRemoteKey(
    label: String,
    command: RemoteCommand?,
    onCommand: (RemoteCommand) -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    accent: Boolean = false,
    pending: Boolean = false,
    minHeight: Dp = 56.dp,
) {
    val interaction = remember { MutableInteractionSource() }
    val supported = command != null && command in LocalEpsonSupportedCommands.current
    val active = enabled && supported
    val showPending = command != null && (pending || !supported)
    val background by animateColorAsState(
        if (accent) EpsonRemoteColors.AccentSoft else EpsonRemoteColors.SurfaceRaised,
        animationSpec = tween(180),
        label = "epsonKeyBackground",
    )
    Surface(
        modifier = modifier
            .heightIn(min = minHeight)
            .semantics {
                contentDescription = when {
                    supported && pending -> "$label, pendiente de verificar"
                    !supported -> "$label, sin código verificado"
                    else -> label
                }
            },
        shape = RoundedCornerShape(16.dp),
        color = if (active) background else EpsonRemoteColors.Surface,
        border = BorderStroke(
            1.dp,
            if (accent && active) EpsonRemoteColors.Accent.copy(alpha = 0.55f)
            else Color.White.copy(alpha = 0.06f),
        ),
        onClick = { command?.let(onCommand) },
        enabled = active,
        interactionSource = interaction,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 9.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            if (icon != null) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (active) EpsonRemoteColors.Text else EpsonRemoteColors.TextMuted.copy(alpha = 0.45f),
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.height(4.dp))
            }
            Text(
                text = label,
                color = if (active) EpsonRemoteColors.Text else EpsonRemoteColors.TextMuted.copy(alpha = 0.5f),
                fontSize = 12.sp,
                lineHeight = 14.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
            if (showPending) {
                Text(
                    if (supported) "sin confirmar" else "sin código",
                    color = if (supported) EpsonRemoteColors.Warning else EpsonRemoteColors.TextMuted.copy(alpha = 0.6f),
                    fontSize = 8.sp,
                    lineHeight = 10.sp,
                )
            }
        }
    }
}

@Composable
fun EpsonRemoteHeader(
    deviceName: String,
    profileFamily: String?,
    irAvailable: Boolean,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onBack: () -> Unit,
    onOpenWizard: () -> Unit,
    onOpenCustomize: () -> Unit,
    onOpenQuickActions: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(EpsonRemoteColors.NavyDeep)
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás", tint = EpsonRemoteColors.Text)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    deviceName,
                    color = EpsonRemoteColors.Text,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
                Text(
                    "Control por infrarrojo",
                    color = EpsonRemoteColors.TextMuted,
                    fontSize = 12.sp,
                    maxLines = 1,
                )
            }
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = if (isFavorite) "Quitar de favoritos" else "Añadir a favoritos",
                    tint = if (isFavorite) EpsonRemoteColors.Warning else EpsonRemoteColors.TextMuted,
                )
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Opciones", tint = EpsonRemoteColors.TextMuted)
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("Asistente de prueba Epson") },
                        leadingIcon = { Icon(Icons.Default.SettingsRemote, null) },
                        onClick = { menuOpen = false; onOpenWizard() },
                    )
                    DropdownMenuItem(
                        text = { Text("Personalizar control") },
                        leadingIcon = { Icon(Icons.Default.Tune, null) },
                        onClick = { menuOpen = false; onOpenCustomize() },
                    )
                    DropdownMenuItem(
                        text = { Text("Acciones rápidas") },
                        leadingIcon = { Icon(Icons.Default.Bolt, null) },
                        onClick = { menuOpen = false; onOpenQuickActions() },
                    )
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            EpsonStatusChip(
                text = "Método: IR",
                color = EpsonRemoteColors.Accent,
                icon = Icons.Default.SettingsRemote,
            )
            EpsonStatusChip(
                text = if (irAvailable) "Emisor IR disponible" else "IR no disponible",
                color = if (irAvailable) EpsonRemoteColors.Success else EpsonRemoteColors.Error,
                icon = if (irAvailable) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
            )
        }
    }
    HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
}

@Composable
private fun EpsonStatusChip(text: String, color: Color, icon: ImageVector) {
    Surface(
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.35f)),
    ) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(14.dp))
            Text(text, color = color, fontSize = 10.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun EpsonPowerButton(onPower: () -> Unit, enabled: Boolean) {
    val supported = RemoteCommand.POWER in LocalEpsonSupportedCommands.current
    Box(contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(94.dp)
                .background(EpsonRemoteColors.Power.copy(alpha = 0.16f), CircleShape)
        )
        Surface(
            onClick = onPower,
            enabled = enabled && supported,
            shape = CircleShape,
            color = if (supported) EpsonRemoteColors.Power else EpsonRemoteColors.PowerDark,
            border = BorderStroke(2.dp, Color.White.copy(alpha = 0.22f)),
            modifier = Modifier
                .size(74.dp)
                .semantics { contentDescription = "Power del proyector" },
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(Icons.Default.PowerSettingsNew, null, tint = Color.White, modifier = Modifier.size(30.dp))
                Text("Power", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun EpsonSourcePanel(
    enabled: Boolean,
    onCommand: (RemoteCommand) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            EpsonRemoteKey(
                label = "Source Search",
                command = RemoteCommand.INPUT,
                onCommand = onCommand,
                icon = Icons.Default.Search,
                enabled = enabled,
                modifier = Modifier.weight(1f),
                accent = true,
            )
            EpsonRemoteKey(
                label = "HDMI",
                command = RemoteCommand.HDMI,
                onCommand = onCommand,
                icon = Icons.Default.Input,
                enabled = enabled,
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            EpsonRemoteKey(
                label = "Computer",
                command = RemoteCommand.COMPUTER,
                onCommand = onCommand,
                icon = Icons.Default.Computer,
                enabled = enabled,
                modifier = Modifier.weight(1f),
            )
            EpsonRemoteKey(
                label = "USB",
                command = RemoteCommand.USB,
                onCommand = onCommand,
                icon = Icons.Default.Usb,
                enabled = enabled,
                modifier = Modifier.weight(1f),
            )
            EpsonRemoteKey(
                label = "LAN",
                command = RemoteCommand.LAN,
                onCommand = onCommand,
                icon = Icons.Default.Lan,
                enabled = enabled,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
fun EpsonNumericKeypad(
    enabled: Boolean,
    onCommand: (RemoteCommand) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(9.dp)) {
        listOf(
            listOf(RemoteCommand.NUM_1 to "1", RemoteCommand.NUM_2 to "2", RemoteCommand.NUM_3 to "3"),
            listOf(RemoteCommand.NUM_4 to "4", RemoteCommand.NUM_5 to "5", RemoteCommand.NUM_6 to "6"),
            listOf(RemoteCommand.NUM_7 to "7", RemoteCommand.NUM_8 to "8", RemoteCommand.NUM_9 to "9"),
        ).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { (command, label) ->
                    EpsonRemoteKey(
                        label = label,
                        command = command,
                        onCommand = onCommand,
                        enabled = enabled,
                        modifier = Modifier.weight(1f),
                        minHeight = 52.dp,
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.fillMaxWidth()) {
            EpsonRemoteKey(
                label = "Num",
                command = null,
                onCommand = onCommand,
                enabled = false,
                modifier = Modifier.weight(1f),
                minHeight = 52.dp,
            )
            EpsonRemoteKey(
                label = "0",
                command = RemoteCommand.NUM_0,
                onCommand = onCommand,
                enabled = enabled,
                modifier = Modifier.weight(1f),
                minHeight = 52.dp,
            )
            EpsonRemoteKey(
                label = "Auto",
                command = RemoteCommand.AUTO_SETUP,
                onCommand = onCommand,
                enabled = enabled,
                modifier = Modifier.weight(1f),
                minHeight = 52.dp,
            )
            EpsonRemoteKey(
                label = "0 / All",
                command = RemoteCommand.NUM_0,
                onCommand = onCommand,
                enabled = enabled,
                modifier = Modifier.weight(1f),
                minHeight = 52.dp,
                pending = true,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.fillMaxWidth()) {
            EpsonRemoteKey(
                label = "ID",
                command = RemoteCommand.ID,
                onCommand = onCommand,
                enabled = enabled,
                modifier = Modifier.weight(1f),
                minHeight = 52.dp,
                pending = true,
            )
            Text(
                "Num es un modificador: no emite una señal propia. ID muestra el ID actual; mantén ID y pulsa un número para asignarlo. 0/All permite operar todos los proyectores. Num, ID y 0/All no tienen código IR verificado para este modelo y aparecen deshabilitados.",
                color = EpsonRemoteColors.TextMuted,
                fontSize = 10.sp,
                lineHeight = 14.sp,
                modifier = Modifier.weight(2.2f),
            )
        }
    }
}

@Composable
fun EpsonCircularDPad(
    enabled: Boolean,
    onCommand: (RemoteCommand) -> Unit,
    modifier: Modifier = Modifier,
) {
    val size = 224.dp
    Box(
        modifier = modifier
            .size(size)
            .background(EpsonRemoteColors.SurfaceRaised, CircleShape)
            .padding(14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(EpsonRemoteColors.Surface, CircleShape)
        )
        DPadButton(
            command = RemoteCommand.UP,
            label = "Arriba",
            icon = Icons.Default.KeyboardArrowUp,
            enabled = enabled,
            onCommand = onCommand,
            modifier = Modifier.align(Alignment.TopCenter),
        )
        DPadButton(
            command = RemoteCommand.DOWN,
            label = "Abajo",
            icon = Icons.Default.KeyboardArrowDown,
            enabled = enabled,
            onCommand = onCommand,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
        DPadButton(
            command = RemoteCommand.LEFT,
            label = "Izquierda",
            icon = Icons.Default.KeyboardArrowLeft,
            enabled = enabled,
            onCommand = onCommand,
            modifier = Modifier.align(Alignment.CenterStart),
        )
        DPadButton(
            command = RemoteCommand.RIGHT,
            label = "Derecha",
            icon = Icons.Default.KeyboardArrowRight,
            enabled = enabled,
            onCommand = onCommand,
            modifier = Modifier.align(Alignment.CenterEnd),
        )
        val enterSupported = RemoteCommand.OK in LocalEpsonSupportedCommands.current
        Surface(
            onClick = { onCommand(RemoteCommand.OK) },
            enabled = enabled && enterSupported,
            shape = CircleShape,
            color = if (enterSupported) EpsonRemoteColors.Accent else EpsonRemoteColors.SurfaceSoft,
            border = BorderStroke(2.dp, Color.White.copy(alpha = 0.18f)),
            modifier = Modifier
                .size(78.dp)
                .semantics { contentDescription = "Enter" },
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(26.dp))
                Text("Enter", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun DPadButton(
    command: RemoteCommand,
    label: String,
    icon: ImageVector,
    enabled: Boolean,
    onCommand: (RemoteCommand) -> Unit,
    modifier: Modifier,
) {
    val supported = command in LocalEpsonSupportedCommands.current
    Surface(
        onClick = { onCommand(command) },
        enabled = enabled && supported,
        shape = CircleShape,
        color = EpsonRemoteColors.SurfaceSoft,
        modifier = modifier
            .size(52.dp)
            .semantics { contentDescription = if (supported) label else "$label, sin código verificado" },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                icon,
                null,
                tint = if (supported) EpsonRemoteColors.Text else EpsonRemoteColors.TextMuted.copy(alpha = 0.45f),
                modifier = Modifier.size(30.dp),
            )
        }
    }
}

@Composable
fun EpsonNavigationKeys(
    enabled: Boolean,
    onCommand: (RemoteCommand) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.fillMaxWidth()) {
            EpsonRemoteKey("Menu", RemoteCommand.MENU, onCommand, Modifier.weight(1f), Icons.Default.Menu, enabled)
            EpsonRemoteKey("Esc", RemoteCommand.BACK, onCommand, Modifier.weight(1f), Icons.AutoMirrored.Filled.ArrowBack, enabled, accent = true)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.fillMaxWidth()) {
            EpsonRemoteKey("User", RemoteCommand.USER, onCommand, Modifier.weight(1f), Icons.Default.Person, enabled)
            EpsonRemoteKey("Default", RemoteCommand.DEFAULT, onCommand, Modifier.weight(1f), Icons.Default.RestartAlt, enabled)
        }
    }
}

@Composable
fun EpsonImageControls(
    enabled: Boolean,
    onCommand: (RemoteCommand) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.fillMaxWidth()) {
            EpsonRemoteKey("Color Mode", RemoteCommand.COLOR_MODE, onCommand, Modifier.weight(1f), Icons.Default.Palette, enabled)
            EpsonRemoteKey("Aspect", RemoteCommand.ASPECT_RATIO, onCommand, Modifier.weight(1f), Icons.Default.AspectRatio, enabled)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.fillMaxWidth()) {
            EpsonRemoteKey("E-Zoom +", RemoteCommand.E_ZOOM_IN, onCommand, Modifier.weight(1f), Icons.Default.ZoomIn, enabled)
            EpsonRemoteKey("E-Zoom −", RemoteCommand.E_ZOOM_OUT, onCommand, Modifier.weight(1f), Icons.Default.ZoomOut, enabled)
        }
        Text(
            "E-Zoom amplía la imagen digitalmente; no ajusta el enfoque ni el zoom físico de la lente.",
            color = EpsonRemoteColors.TextMuted,
            fontSize = 10.sp,
            lineHeight = 14.sp,
        )
    }
}

@Composable
fun EpsonAudioControls(
    enabled: Boolean,
    onCommand: (RemoteCommand) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.fillMaxWidth()) {
            EpsonRemoteKey("Volume +", RemoteCommand.VOLUME_UP, onCommand, Modifier.weight(1f), Icons.AutoMirrored.Filled.VolumeUp, enabled)
            EpsonRemoteKey("Volume −", RemoteCommand.VOLUME_DOWN, onCommand, Modifier.weight(1f), Icons.AutoMirrored.Filled.VolumeDown, enabled)
        }
        Text(
            "A/V Mute oculta imagen y sonido temporalmente y está en Funciones; no se añade un mute de audio separado sin código documentado.",
            color = EpsonRemoteColors.TextMuted,
            fontSize = 10.sp,
            lineHeight = 14.sp,
        )
    }
}

@Composable
fun EpsonFunctionPanel(
    enabled: Boolean,
    onCommand: (RemoteCommand) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.fillMaxWidth()) {
            EpsonRemoteKey("A/V Mute", RemoteCommand.AV_MUTE, onCommand, Modifier.weight(1f), Icons.Default.VisibilityOff, enabled, accent = true)
            EpsonRemoteKey("Freeze", RemoteCommand.FREEZE, onCommand, Modifier.weight(1f), Icons.Default.PauseCircle, enabled)
            EpsonRemoteKey("Split", RemoteCommand.SPLIT, onCommand, Modifier.weight(1f), Icons.Default.VerticalSplit, enabled, pending = true)
            EpsonRemoteKey("Home", RemoteCommand.HOME, onCommand, Modifier.weight(1f), Icons.Default.Home, enabled, pending = true)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.fillMaxWidth()) {
            EpsonRemoteKey("Información", RemoteCommand.INFO, onCommand, Modifier.weight(1f), Icons.Default.Info, enabled)
            EpsonRemoteKey("Ajustes", RemoteCommand.SETTINGS_KEY, onCommand, Modifier.weight(1f), Icons.Default.Settings, enabled)
        }
        Text(
            "Split solo funciona si el proyector admite pantalla dividida. A/V Mute oculta imagen y sonido; no es un mute de audio independiente. " +
                "Ajustes abre el menú del proyector. Información se muestra deshabilitado porque no hay código IR verificado para ese botón.",
            color = EpsonRemoteColors.TextMuted,
            fontSize = 10.sp,
            lineHeight = 14.sp,
        )
    }
}

@Composable
fun EpsonIrStatus(
    state: EpsonIrRemoteUiState,
    onConfirm: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val verification = when (state.validationStatus) {
        IrValidationStatus.CONFIRMED_BY_USER -> "Perfil confirmado por ti" to EpsonRemoteColors.Success
        IrValidationStatus.INCOMPATIBLE -> "Sin respuesta: revisar perfil" to EpsonRemoteColors.Error
        IrValidationStatus.UNVERIFIED -> "Pendiente de verificación física" to EpsonRemoteColors.Warning
    }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            EpsonStatusChip(
                text = if (state.profileLoaded) "Perfil cargado" else "Perfil no cargado",
                color = if (state.profileLoaded) EpsonRemoteColors.Accent else EpsonRemoteColors.Error,
                icon = Icons.Default.Description,
            )
            EpsonStatusChip(
                text = verification.first,
                color = verification.second,
                icon = Icons.Default.FactCheck,
            )
        }
        if (state.isDemo && state.profileLoaded) {
            Text(
                "Perfil disponible, pero el emisor IR o el dispositivo guardado no están listos.",
                color = EpsonRemoteColors.Warning,
                fontSize = 11.sp,
            )
        }
        AnimatedVisibility(state.lastResultText != null) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (state.isError) EpsonRemoteColors.Error.copy(alpha = 0.14f)
                else EpsonRemoteColors.Accent.copy(alpha = 0.14f),
                border = BorderStroke(
                    1.dp,
                    if (state.isError) EpsonRemoteColors.Error.copy(alpha = 0.4f)
                    else EpsonRemoteColors.Accent.copy(alpha = 0.4f),
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (state.isTransmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = EpsonRemoteColors.Accent,
                        )
                    } else {
                        Icon(
                            if (state.isError) Icons.Default.ErrorOutline else Icons.Default.Info,
                            null,
                            tint = if (state.isError) EpsonRemoteColors.Error else EpsonRemoteColors.Accent,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Text(
                        state.lastResultText ?: "",
                        color = EpsonRemoteColors.Text,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                    )
                }
            }
        }
        if (state.canSaveConfirmation && state.lastTransmitted != null) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = EpsonRemoteColors.SurfaceRaised,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "¿El proyector respondió a este comando?",
                        color = EpsonRemoteColors.Text,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { onConfirm(true) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = EpsonRemoteColors.AccentSoft),
                        ) { Text("Sí, funcionó") }
                        OutlinedButton(
                            onClick = { onConfirm(false) },
                            modifier = Modifier.weight(1f),
                            border = BorderStroke(1.dp, EpsonRemoteColors.TextMuted.copy(alpha = 0.4f)),
                        ) { Text("No funcionó", color = EpsonRemoteColors.TextMuted) }
                    }
                }
            }
        }
        Text(
            "IR es unidireccional: \"transmitido\" significa que Android aceptó la señal, no que el proyector la recibió. Nunca se muestra \"proyector conectado\" por IR.",
            color = EpsonRemoteColors.TextMuted,
            fontSize = 10.sp,
            lineHeight = 14.sp,
        )
    }
}

@Composable
fun EpsonIrTestWizard(
    irAvailable: Boolean,
    profileLoaded: Boolean,
    onOpenWizard: () -> Unit,
    modifier: Modifier = Modifier,
    deviceName: String = "Epson",
    testCommandLabel: String = "Menu",
    testNote: String? = null,
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = EpsonRemoteColors.SurfaceRaised,
        border = BorderStroke(1.dp, EpsonRemoteColors.Accent.copy(alpha = 0.32f)),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.SettingsRemote, null, tint = EpsonRemoteColors.Accent)
                Text("Configurar control Epson", color = EpsonRemoteColors.Text, fontWeight = FontWeight.SemiBold)
            }
            Text(
                "1. Comprobar emisor IR · 2. Elegir $deviceName · 3. Apuntar al proyector · 4. Probar $testCommandLabel · 5. Confirmar si respondió. No se prueba Power.",
                color = EpsonRemoteColors.TextMuted,
                fontSize = 11.sp,
                lineHeight = 16.sp,
            )
            if (testNote != null) {
                Text(
                    testNote,
                    color = EpsonRemoteColors.Warning,
                    fontSize = 10.sp,
                    lineHeight = 15.sp,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EpsonStatusChip(
                    if (irAvailable) "Emisor listo" else "Sin emisor",
                    if (irAvailable) EpsonRemoteColors.Success else EpsonRemoteColors.Error,
                    Icons.Default.SettingsRemote,
                )
                EpsonStatusChip(
                    if (profileLoaded) "Perfil listo" else "Perfil pendiente",
                    if (profileLoaded) EpsonRemoteColors.Accent else EpsonRemoteColors.Warning,
                    Icons.Default.Description,
                )
            }
            OutlinedButton(
                onClick = onOpenWizard,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                border = BorderStroke(1.dp, EpsonRemoteColors.Accent.copy(alpha = 0.5f)),
            ) {
                Text("Abrir asistente de prueba", color = EpsonRemoteColors.Accent)
            }
        }
    }
}
