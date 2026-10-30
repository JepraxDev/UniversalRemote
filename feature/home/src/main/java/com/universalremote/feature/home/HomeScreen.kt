package com.universalremote.feature.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.universalremote.core.designsystem.*
import com.universalremote.core.designsystem.R
import com.universalremote.core.model.*
import com.universalremote.domain.*
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class HomeUiState(
    val loading: Boolean = true,
    val error: Boolean = false,
    val irAvailability: IrAvailability? = null,
    val query: String = "",
    val selectedTypeFilter: DeviceType? = null,
    val allDevices: List<Device> = emptyList(),
    val filteredDevices: List<Device> = emptyList()
)

@HiltViewModel
class HomeViewModel(
    private val devicesRepository: DeviceRepository,
    private val infrared: IrAvailabilityProvider,
    private val openRemoteUseCase: OpenRemoteUseCase,
    private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    @Inject constructor(
        devicesRepository: DeviceRepository,
        infrared: IrAvailabilityProvider,
        openRemoteUseCase: OpenRemoteUseCase
    ) : this(devicesRepository, infrared, openRemoteUseCase, Dispatchers.IO)

    private val mutableState = MutableStateFlow(HomeUiState())
    val state = mutableState.asStateFlow()

    private var loadJob: Job? = null

    init {
        refresh()
    }

    fun refresh() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            mutableState.update { it.copy(loading = true, error = false) }
            val ir = withContext(ioDispatcher) { infrared.availability() }
            mutableState.update { it.copy(irAvailability = ir) }

            devicesRepository.observeDevices().catch {
                mutableState.update { it.copy(loading = false, error = true) }
            }.collect { list ->
                mutableState.update { current ->
                    val filtered = filterDevices(list, current.query, current.selectedTypeFilter)
                    current.copy(
                        loading = false,
                        allDevices = list,
                        filteredDevices = filtered
                    )
                }
            }
        }
    }

    fun setQuery(q: String) {
        mutableState.update { current ->
            val filtered = filterDevices(current.allDevices, q, current.selectedTypeFilter)
            current.copy(query = q, filteredDevices = filtered)
        }
    }

    fun selectTypeFilter(type: DeviceType?) {
        mutableState.update { current ->
            val newFilter = if (current.selectedTypeFilter == type) null else type
            val filtered = filterDevices(current.allDevices, current.query, newFilter)
            current.copy(selectedTypeFilter = newFilter, filteredDevices = filtered)
        }
    }

    fun toggleFavorite(id: String) {
        viewModelScope.launch {
            val dev = devicesRepository.find(id) ?: return@launch
            devicesRepository.update(dev.copy(isFavorite = !dev.isFavorite))
        }
    }

    fun deleteDevice(id: String) {
        viewModelScope.launch {
            devicesRepository.delete(id)
        }
    }

    fun openDevice(id: String, onOpen: (Device) -> Unit) {
        viewModelScope.launch {
            val opened = openRemoteUseCase(id)
            if (opened != null) {
                onOpen(opened)
            }
        }
    }

    private fun filterDevices(list: List<Device>, query: String, type: DeviceType?): List<Device> {
        val q = query.trim().lowercase()
        return list.filter { dev ->
            val nameMatches = q.isEmpty() ||
                    dev.name.lowercase().contains(q) ||
                    dev.brand.id.lowercase().contains(q) ||
                    (dev.locationTag ?: "").lowercase().contains(q)
            val typeMatches = type == null || dev.deviceType == type
            nameMatches && typeMatches
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onDevicesNav: () -> Unit,
    onAddNav: () -> Unit,
    onDiscoveryNav: () -> Unit,
    onSettingsNav: () -> Unit,
    onRemotePreview: (DeviceType) -> Unit,
    onOpenDevice: (Device) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showGuideDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Universal Remote", style = MaterialTheme.typography.titleLarge)
                        Text("Controla tus televisores y proyectores", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = {
                    IconButton(onClick = onSettingsNav) {
                        Icon(Icons.Default.Settings, contentDescription = "Ajustes")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Search Bar
            item {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = viewModel::setQuery,
                    placeholder = { Text("Buscar dispositivo... (nombre, marca o ubicación)") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                )
            }

            // Large Type Filter Cards Row (Televisor vs Proyector)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TypeFilterCard(
                        title = "Televisor",
                        subtitle = "Smart TV, Android TV, TV Stick",
                        icon = Icons.Default.Tv,
                        isSelected = state.selectedTypeFilter == DeviceType.TV,
                        onClick = { viewModel.selectTypeFilter(DeviceType.TV) },
                        modifier = Modifier.weight(1f)
                    )
                    TypeFilterCard(
                        title = "Proyector",
                        subtitle = "Epson, BenQ, ViewSonic, etc.",
                        icon = Icons.Default.Videocam,
                        isSelected = state.selectedTypeFilter == DeviceType.PROJECTOR,
                        onClick = { viewModel.selectTypeFilter(DeviceType.PROJECTOR) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Dispositivos Disponibles Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Dispositivos disponibles", style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = onDiscoveryNav) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Buscar en la red")
                    }
                }
            }

            // Devices List or Empty State
            if (state.loading) {
                item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
            } else if (state.error) {
                item {
                    Text(stringResource(R.string.read_error), color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = viewModel::refresh) { Text(stringResource(R.string.retry)) }
                }
            } else if (state.filteredDevices.isEmpty()) {
                item {
                    EmptyState(
                        title = "Aún no tienes dispositivos",
                        detail = "Agrega un televisor o proyector para comenzar a controlarlo al instante.",
                        onAction = onAddNav
                    )
                }
            } else {
                items(state.filteredDevices, key = { it.id }) { device ->
                    DeviceCard(
                        device = device,
                        onOpenRemote = { viewModel.openDevice(device.id, onOpenDevice) },
                        onFavorite = { viewModel.toggleFavorite(device.id) },
                        onDelete = { viewModel.deleteDevice(device.id) }
                    )
                }
            }

            // Bottom Quick Action Cards (3 cards)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickActionCard(
                        icon = Icons.Default.QrCodeScanner,
                        title = "Buscar dispositivos",
                        subtitle = "En tu red local",
                        onClick = onDiscoveryNav,
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionCard(
                        icon = Icons.Default.AddCircle,
                        title = "Agregar dispositivo",
                        subtitle = "Manual o automático",
                        onClick = onAddNav,
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionCard(
                        icon = Icons.Default.Build,
                        title = "Guía rápida",
                        subtitle = "Cómo conectar",
                        onClick = { showGuideDialog = true },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    if (showGuideDialog) {
        AlertDialog(
            onDismissRequest = { showGuideDialog = false },
            title = { Text("Guía Rápida de Conexión") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("1. Conecta tu celular a la misma red Wi-Fi que tu Smart TV o proyector.", style = MaterialTheme.typography.bodyMedium)
                    Text("2. Presiona 'Buscar dispositivos' para escanear la red local vía mDNS o SSDP.", style = MaterialTheme.typography.bodyMedium)
                    Text("3. En celulares con emisor Infrarrojo, puedes seleccionar 'Infrarrojo' para control inmediato sin necesidad de Wi-Fi.", style = MaterialTheme.typography.bodyMedium)
                }
            },
            confirmButton = {
                TextButton(onClick = { showGuideDialog = false }) {
                    Text("Entendido")
                }
            }
        )
    }
}

@Composable
private fun TypeFilterCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.heightIn(min = 96.dp)
    ) {
        Box(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
                Column {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (isSelected) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.TopEnd).size(20.dp)
                ) {
                    Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.padding(2.dp))
                }
            }
        }
    }
}

@Composable
private fun QuickActionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier.heightIn(min = 100.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Text(title, style = MaterialTheme.typography.labelMedium, maxLines = 1)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
    }
}
