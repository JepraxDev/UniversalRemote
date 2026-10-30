package com.universalremote.core.designsystem

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.universalremote.core.model.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(title: String, onBack: (() -> Unit)? = null) {
    TopAppBar(title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
        navigationIcon = {
            if (onBack != null) IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
            }
        })
}

enum class MainTab { HOME, DEVICES, SETTINGS }
@Composable
fun BottomNavigation(selected: MainTab, onSelect: (MainTab) -> Unit) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        MainTab.entries.forEach { tab ->
            val label = stringResource(when (tab) {
                MainTab.HOME -> R.string.home
                MainTab.DEVICES -> R.string.devices_tab
                MainTab.SETTINGS -> R.string.settings_tab
            })
            NavigationBarItem(selected = tab == selected, onClick = { onSelect(tab) },
                icon = { Icon(when (tab) {
                    MainTab.HOME -> Icons.Default.Home
                    MainTab.DEVICES -> Icons.Default.Devices
                    MainTab.SETTINGS -> Icons.Default.Settings
                }, null) }, label = { Text(label) })
        }
    }
}

@Composable
fun StatusIndicator(text: String, available: Boolean = false, icon: ImageVector = Icons.Default.Info) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = MaterialTheme.shapes.medium) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icon, null, tint = if (available) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
fun EmptyState(title: String = stringResource(R.string.empty_title), detail: String = stringResource(R.string.empty_detail),
               action: String = stringResource(R.string.add_device), onAction: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(28.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.primaryContainer) {
                Icon(Icons.Default.SettingsRemote, null, Modifier.padding(24.dp).size(48.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = onAction, modifier = Modifier.heightIn(min = 56.dp)) {
                Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text(action)
            }
        }
    }
}

@Composable
private fun ChoiceCard(title: String, detail: String?, icon: ImageVector, selected: Boolean, enabled: Boolean = true, onClick: () -> Unit) {
    Surface(shape = MaterialTheme.shapes.medium,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth().selectable(selected, enabled = enabled, role = Role.RadioButton, onClick = onClick)) {
        Row(Modifier.padding(20.dp).heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon(icon, null, Modifier.size(28.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                if (detail != null) Text(detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            RadioButton(selected, null, enabled = enabled)
        }
    }
}

@Composable
fun DeviceTypeSelector(selected: DeviceType, onSelect: (DeviceType) -> Unit) {
    Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        DeviceType.entries.forEach { type ->
            ChoiceCard(deviceTypeText(type), stringResource(if (type == DeviceType.TV) R.string.tv_hint else R.string.projector_hint),
                if (type == DeviceType.TV) Icons.Default.Tv else Icons.Default.Videocam, selected == type) { onSelect(type) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BrandSelector(selected: DeviceBrand, onSelect: (DeviceBrand) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        DeviceBrand.catalog.forEach { brand ->
            FilterChip(selected == brand, { onSelect(brand) }, { Text(brandText(brand), Modifier.padding(8.dp)) }, modifier = Modifier.heightIn(min = 48.dp))
        }
    }
}

@Composable
fun ConnectionMethodSelector(selected: ConnectionType, ir: IrAvailability?, onSelect: (ConnectionType) -> Unit) {
    Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ChoiceCard(stringResource(R.string.wifi), stringResource(R.string.wifi_hint), Icons.Default.Wifi,
            selected == ConnectionType.WIFI) { onSelect(ConnectionType.WIFI) }
        ChoiceCard(stringResource(R.string.infrared), irText(ir), Icons.Default.SettingsRemote,
            selected == ConnectionType.INFRARED, enabled = ir == IrAvailability.Available) { onSelect(ConnectionType.INFRARED) }
    }
}

@Composable
fun DeviceCard(
    device: Device,
    onOpenRemote: () -> Unit,
    onFavorite: (() -> Unit)? = null,
    onRename: (() -> Unit)? = null,
    onEditConnection: (() -> Unit)? = null,
    onChangeLocation: (() -> Unit)? = null,
    onShowDetails: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
) {
    var menu by remember { mutableStateOf(false) }
    val cleanName = DeviceDisplayNameResolver.resolveDisplayName(device)
    val subtitle = DeviceDisplayNameResolver.resolveSubtitle(device)
    val haptic = rememberHapticFeedbackHelper()

    Card(
        onClick = {
            haptic.performClick()
            onOpenRemote()
        },
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Device Icon Container
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            if (device.deviceType == DeviceType.TV) Icons.Default.Tv else Icons.Default.Videocam,
                            contentDescription = deviceTypeText(device.deviceType),
                            modifier = Modifier.size(28.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                // Name & Subtitle
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            cleanName,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1
                        )
                        if (device.isFavorite) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = "Favorito",
                                tint = Color(0xFFEAB308),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val loc = device.locationTag ?: device.roomName
                    if (!loc.isNullOrBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(loc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                // 3-dots Menu Button
                Box {
                    IconButton(onClick = { menu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Opciones")
                    }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        if (onFavorite != null) {
                            DropdownMenuItem(
                                text = { Text(if (device.isFavorite) "Quitar de favoritos" else "Marcar como favorito") },
                                onClick = { menu = false; onFavorite() },
                                leadingIcon = { Icon(if (device.isFavorite) Icons.Default.StarBorder else Icons.Default.Star, null) }
                            )
                        }
                        if (onRename != null) {
                            DropdownMenuItem(
                                text = { Text("Renombrar") },
                                onClick = { menu = false; onRename() },
                                leadingIcon = { Icon(Icons.Default.Edit, null) }
                            )
                        }
                        if (onEditConnection != null) {
                            DropdownMenuItem(
                                text = { Text("Editar conexión") },
                                onClick = { menu = false; onEditConnection() },
                                leadingIcon = { Icon(Icons.Default.SettingsRemote, null) }
                            )
                        }
                        if (onChangeLocation != null) {
                            DropdownMenuItem(
                                text = { Text("Cambiar ubicación") },
                                onClick = { menu = false; onChangeLocation() },
                                leadingIcon = { Icon(Icons.Default.LocationOn, null) }
                            )
                        }
                        if (onShowDetails != null) {
                            DropdownMenuItem(
                                text = { Text("Ver detalles") },
                                onClick = { menu = false; onShowDetails() },
                                leadingIcon = { Icon(Icons.Default.Info, null) }
                            )
                        }
                        if (onDelete != null) {
                            DropdownMenuItem(
                                text = { Text("Eliminar") },
                                onClick = { menu = false; onDelete() },
                                leadingIcon = { Icon(Icons.Default.Delete, null) }
                            )
                        }
                    }
                }
            }

            // Transport Pill Badge & Prominent "Abrir control" Action Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            if (device.connectionType == ConnectionType.WIFI) Icons.Default.Wifi else Icons.Default.SettingsRemote,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = if (device.connectionType == ConnectionType.WIFI) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = if (device.connectionType == ConnectionType.WIFI) {
                                "Wi-Fi · ${device.ipAddress ?: "IP no configurada"}"
                            } else {
                                "Infrarrojo · Disponible"
                            },
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }

                Button(
                    onClick = {
                        haptic.performClick()
                        onOpenRemote()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(
                        Icons.Default.SettingsRemote,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Abrir control", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
fun ConfirmationDialog(
    title: String,
    detail: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmLabel: String = stringResource(R.string.delete_device),
    icon: ImageVector = Icons.Default.Warning,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(icon, null) },
        title = { Text(title) },
        text = { Text(detail) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}
