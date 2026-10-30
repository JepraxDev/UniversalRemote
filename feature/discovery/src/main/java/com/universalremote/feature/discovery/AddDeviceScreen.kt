package com.universalremote.feature.discovery

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.universalremote.core.designsystem.*
import com.universalremote.core.designsystem.R
import com.universalremote.core.model.*
import com.universalremote.domain.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class AddDeviceUiState(
    val type: DeviceType = DeviceType.TV,
    val brand: DeviceBrand = DeviceBrand.LG,
    val connection: ConnectionType = ConnectionType.WIFI,
    val ir: IrAvailability? = null,
    val step: Int = 0,
)

@HiltViewModel
class AddDeviceViewModel(
    private val saved: SavedStateHandle,
    infrared: IrAvailabilityProvider,
    io: CoroutineDispatcher
) : ViewModel() {

    @Inject constructor(saved: SavedStateHandle, infrared: IrAvailabilityProvider) : this(saved, infrared, Dispatchers.IO)

    private val mutableState = MutableStateFlow(AddDeviceUiState(
        type = DeviceType.entries.firstOrNull { it.name == saved.get<String>("type") } ?: DeviceType.TV,
        brand = DeviceBrand(saved.get<String>("brand") ?: DeviceBrand.LG.id),
        step = (saved.get<Int>("step") ?: 0).coerceIn(0, 1),
    ))
    val state = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            val ir = withContext(io) { infrared.availability() }
            mutableState.update {
                it.copy(
                    ir = ir,
                    connection = if (saved.get<String>("connection") == ConnectionType.INFRARED.name && ir == IrAvailability.Available)
                        ConnectionType.INFRARED else ConnectionType.WIFI
                )
            }
        }
    }

    fun selectType(type: DeviceType) {
        saved["type"] = type.name
        val defaultBrand = if (type == DeviceType.PROJECTOR) DeviceBrand.EPSON else DeviceBrand.LG
        saved["brand"] = defaultBrand.id
        mutableState.update { it.copy(type = type, brand = defaultBrand) }
    }

    fun selectConnection(connection: ConnectionType) {
        if (connection !in GetConnectionMethods()(state.value.ir ?: IrAvailability.ServiceUnavailable)) return
        saved["connection"] = connection.name
        mutableState.update { it.copy(connection = connection) }
    }

    fun selectBrand(brand: DeviceBrand) {
        saved["brand"] = brand.id
        mutableState.update { it.copy(brand = brand) }
    }

    fun next() { setStep((state.value.step + 1).coerceAtMost(1)) }
    fun previous() { setStep((state.value.step - 1).coerceAtLeast(0)) }

    private fun setStep(step: Int) {
        saved["step"] = step
        mutableState.update { it.copy(step = step) }
    }
}

@Composable
fun AddDeviceScreen(
    viewModel: AddDeviceViewModel,
    onBack: () -> Unit,
    onSearchWifi: () -> Unit,
    onSetupIr: (DeviceType) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val back = { if (state.step > 0) viewModel.previous() else onBack() }

    Page(stringResource(R.string.add_device), back) {
        Text("Paso ${state.step + 1} de 2", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
        LinearProgressIndicator(progress = { (state.step + 1) / 2f }, modifier = Modifier.fillMaxWidth())

        AnimatedContent(state.step, label = "add_wizard") { step ->
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Text(
                    text = if (step == 0) "¿Qué deseas controlar?" else "¿Cómo quieres conectarlo?",
                    style = MaterialTheme.typography.headlineMedium
                )

                if (step == 0) {
                    DeviceTypeSelector(state.type, viewModel::selectType)
                } else {
                    ConnectionMethodSelector(state.connection, state.ir, viewModel::selectConnection)
                    Text(
                        text = if (state.connection == ConnectionType.WIFI) {
                            "Buscará automáticamente televisores y proyectores compatibles en tu red Wi-Fi local."
                        } else {
                            "Te permitirá elegir la marca y transmitir códigos infrarrojos directamente desde el emisor de tu celular."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Button(
            onClick = {
                if (state.step == 0) {
                    viewModel.next()
                } else {
                    if (state.connection == ConnectionType.WIFI) {
                        onSearchWifi()
                    } else {
                        onSetupIr(state.type)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
        ) {
            Text(if (state.step == 0) "Siguiente" else "Comenzar conexión")
        }
    }
}

@Composable
fun SetupInfoScreen(viewModel: AddDeviceViewModel, onBack: () -> Unit, onRemote: (DeviceType) -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Page(stringResource(R.string.setup_title), onBack) {
        Text("Resumen de Configuración", color = MaterialTheme.colorScheme.primary)
        LinearProgressIndicator(progress = { 1f }, modifier = Modifier.fillMaxWidth())
        InfoCard(stringResource(R.string.setup_summary), listOf(deviceTypeText(state.type), brandText(state.brand), connectionText(state.connection)).joinToString(" · "))
        Text(stringResource(R.string.setup_detail))
        Button(onClick = { onRemote(state.type) }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text(stringResource(R.string.preview_remote)) }
    }
}
