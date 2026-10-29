package com.universalremote.core.designsystem

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
fun DeviceCard(device: Device, onPreview: (() -> Unit)? = null, onFavorite: (() -> Unit)? = null, onDelete: (() -> Unit)? = null) {
    var expanded by rememberSaveable(device.id) { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth().animateContentSize(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.primaryContainer) {
                    Icon(if (device.deviceType == DeviceType.TV) Icons.Default.Tv else Icons.Default.Videocam,
                        deviceTypeText(device.deviceType), Modifier.padding(14.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                Column(Modifier.weight(1f)) {
                    Text(device.name, style = MaterialTheme.typography.titleMedium)
                    Text(listOf(brandText(device.brand), device.model).filter { it.isNotBlank() }.joinToString(" · "),
                        color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                }
                if (device.isFavorite) Icon(Icons.Default.Star, stringResource(R.string.favorite), tint = MaterialTheme.colorScheme.primary)
                if (onPreview != null || onFavorite != null || onDelete != null) Box {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, stringResource(R.string.device_actions, device.name)) }
                    DropdownMenu(menu, { menu = false }) {
                        if (onPreview != null) DropdownMenuItem(text = { Text(stringResource(R.string.preview_remote)) }, onClick = { menu = false; onPreview() })
                        if (onFavorite != null) DropdownMenuItem(text = { Text(stringResource(if (device.isFavorite) R.string.unfavorite else R.string.make_favorite)) }, onClick = { menu = false; onFavorite() })
                        if (onDelete != null) DropdownMenuItem(text = { Text(stringResource(R.string.delete_device)) }, onClick = { menu = false; onDelete() })
                    }
                }
            }
            Text(connectionText(device.connectionType), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            device.roomName?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            TextButton(onClick = { expanded = !expanded }) { Text(stringResource(if (expanded) R.string.less_detail else R.string.more_detail)) }
            if (expanded) Text(stringResource(R.string.configured_not_connected), style = MaterialTheme.typography.bodyMedium)
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
