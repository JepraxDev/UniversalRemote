package com.universalremote.feature.discovery

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
import androidx.compose.animation.AnimatedContent
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
class AddDeviceViewModel(private val saved: SavedStateHandle, infrared: IrAvailabilityProvider, io: CoroutineDispatcher) : ViewModel() {
    @Inject constructor(saved: SavedStateHandle, infrared: IrAvailabilityProvider) : this(saved, infrared, Dispatchers.IO)
    private val mutableState = MutableStateFlow(AddDeviceUiState(
        type = DeviceType.entries.firstOrNull { it.name == saved.get<String>("type") } ?: DeviceType.TV,
        brand = DeviceBrand(saved.get<String>("brand") ?: DeviceBrand.LG.id),
        step = (saved.get<Int>("step") ?: 0).coerceIn(0, 2),
    ))
    val state = mutableState.asStateFlow()
    init {
        viewModelScope.launch {
            val ir = withContext(io) { infrared.availability() }
            mutableState.update { it.copy(ir = ir, connection =
                if (saved.get<String>("connection") == ConnectionType.INFRARED.name && ir == IrAvailability.Available)
                    ConnectionType.INFRARED else ConnectionType.WIFI) }
        }
    }
    fun selectType(type: DeviceType) { saved["type"] = type.name; mutableState.update { it.copy(type = type) } }
    fun next() { setStep((state.value.step + 1).coerceAtMost(2)) }
    fun previous() { setStep((state.value.step - 1).coerceAtLeast(0)) }
    private fun setStep(step: Int) { saved["step"] = step; mutableState.update { it.copy(step = step) } }
    fun selectBrand(brand: DeviceBrand) { saved["brand"] = brand.id; mutableState.update { it.copy(brand = brand) } }
    fun selectConnection(connection: ConnectionType) {
        if (connection !in GetConnectionMethods()(state.value.ir ?: IrAvailability.ServiceUnavailable)) return
        saved["connection"] = connection.name
        mutableState.update { it.copy(connection = connection) }
    }
}
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddDeviceScreen(viewModel: AddDeviceViewModel, onBack: () -> Unit, onContinue: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val back = { if (state.step > 0) viewModel.previous() else onBack() }
    Page(stringResource(R.string.add_device), back) {
        Text(stringResource(R.string.step, state.step + 1), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
        LinearProgressIndicator(progress = { (state.step + 1) / 4f }, modifier = Modifier.fillMaxWidth())
        AnimatedContent(state.step, label = "wizard") { step ->
            Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                Text(stringResource(when (step) { 0 -> R.string.choose_type; 1 -> R.string.choose_brand; else -> R.string.choose_connection }), style = MaterialTheme.typography.headlineMedium)
                when (step) {
                    0 -> DeviceTypeSelector(state.type, viewModel::selectType)
                    1 -> BrandSelector(state.brand, viewModel::selectBrand)
                    else -> {
                        ConnectionMethodSelector(state.connection, state.ir, viewModel::selectConnection)
                        Text(stringResource(R.string.network_detail), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        Button(onClick = { if (state.step < 2) viewModel.next() else onContinue() }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text(stringResource(R.string.continue_label)) }
    }
}
@Composable
fun SetupInfoScreen(viewModel: AddDeviceViewModel, onBack: () -> Unit, onRemote: (DeviceType) -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Page(stringResource(R.string.setup_title), onBack) {
        Text(stringResource(R.string.step, 4), color = MaterialTheme.colorScheme.primary)
        LinearProgressIndicator(progress = { 1f }, modifier = Modifier.fillMaxWidth())
        InfoCard(stringResource(R.string.setup_summary), listOf(deviceTypeText(state.type), brandText(state.brand), connectionText(state.connection)).joinToString(" · "))
        Text(stringResource(R.string.setup_detail))
        Button(onClick = { onRemote(state.type) }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text(stringResource(R.string.preview_remote)) }
    }
}
