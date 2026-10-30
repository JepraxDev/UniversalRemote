package com.universalremote.feature.remote

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.universalremote.core.designsystem.AppTopBar
import com.universalremote.core.model.*
import com.universalremote.domain.*
import com.universalremote.protocol.api.CommandResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class QuickActionsViewModel @Inject constructor(
    private val saved: SavedStateHandle,
    private val deviceRepository: DeviceRepository,
    private val quickActionRepository: QuickActionRepository,
    private val sendRemoteCommand: SendRemoteCommand,
    private val sendNetworkCommand: SendNetworkCommand,
    private val runner: QuickActionRunner
) : ViewModel() {

    val deviceId: String? = saved.get<String>("deviceId")

    val quickActions: StateFlow<List<QuickAction>> = if (deviceId != null) {
        quickActionRepository.observeForDevice(deviceId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    } else {
        MutableStateFlow(emptyList())
    }

    val executionState: StateFlow<QuickActionExecutionState> = runner.state

    fun createQuickAction(name: String, steps: List<Pair<RemoteCommand, Long>>) {
        val devId = deviceId ?: return
        if (name.isBlank() || steps.isEmpty()) return

        viewModelScope.launch {
            val actionId = "act_${UUID.randomUUID()}"
            val actionSteps = steps.mapIndexed { idx, pair ->
                QuickActionStep(
                    id = "step_${UUID.randomUUID()}",
                    actionId = actionId,
                    stepOrder = idx,
                    command = pair.first,
                    delayMs = pair.second
                )
            }
            val action = QuickAction(
                id = actionId,
                name = name.trim(),
                deviceId = devId,
                steps = actionSteps
            )
            quickActionRepository.save(action)
        }
    }

    fun deleteQuickAction(id: String) {
        viewModelScope.launch {
            quickActionRepository.delete(id)
        }
    }

    fun executeQuickAction(action: QuickAction) {
        val devId = deviceId ?: return
        viewModelScope.launch {
            val device = deviceRepository.find(devId) ?: return@launch
            runner.execute(action) { step ->
                if (device.connectionType == ConnectionType.WIFI) {
                    val res = sendNetworkCommand(device, step.command)
                    res is CommandResult.Sent
                } else {
                    val res = sendRemoteCommand(device.id, step.command, confirmed = true)
                    res is IrResult.Success
                }
            }
        }
    }

    fun cancelExecution() {
        runner.cancel()
    }
}

@Composable
fun QuickActionsScreen(
    viewModel: QuickActionsViewModel,
    onBack: () -> Unit
) {
    val actions by viewModel.quickActions.collectAsStateWithLifecycle()
    val executionState by viewModel.executionState.collectAsStateWithLifecycle()

    var showCreateDialog by remember { mutableStateOf(false) }
    var actionName by remember { mutableStateOf("") }
    val stepsList = remember { mutableStateListOf<Pair<RemoteCommand, Long>>() }

    Scaffold(
        topBar = { AppTopBar("Acciones Rápidas", onBack) },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                actionName = ""
                stepsList.clear()
                stepsList.add(RemoteCommand.POWER to 500L)
                stepsList.add(RemoteCommand.HDMI to 500L)
                showCreateDialog = true
            }) {
                Icon(Icons.Default.Add, "Nueva Acción Rápida")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text("Secuencias de comandos (${actions.size})", style = MaterialTheme.typography.titleMedium)

            Spacer(modifier = Modifier.height(12.dp))

            when (val currentExec = executionState) {
                is QuickActionExecutionState.Running -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Ejecutando: ${currentExec.actionName}", style = MaterialTheme.typography.titleMedium)
                            Text("Paso ${currentExec.currentStep} de ${currentExec.totalSteps}: ${currentExec.stepDescription}", style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { currentExec.currentStep / currentExec.totalSteps.toFloat() },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(onClick = viewModel::cancelExecution) {
                                Text("Cancelar Ejecución")
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
                is QuickActionExecutionState.Failed -> {
                    Text("Error: ${currentExec.error}", color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                is QuickActionExecutionState.Completed -> {
                    Text("Secuencia completada con éxito", color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                else -> {}
            }

            if (actions.isEmpty()) {
                Text("No hay acciones rápidas creadas. Pulsa + para crear una secuencia personalizada.")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(actions, key = { it.id }) { action ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(action.name, style = MaterialTheme.typography.titleMedium)
                                    Text("${action.steps.size} pasos: ${action.steps.joinToString(" → ") { it.command.name }}", style = MaterialTheme.typography.bodySmall)
                                }
                                Row {
                                    IconButton(onClick = { viewModel.executeQuickAction(action) }) {
                                        Icon(Icons.Default.PlayArrow, "Ejecutar")
                                    }
                                    IconButton(onClick = { viewModel.deleteQuickAction(action.id) }) {
                                        Icon(Icons.Default.Delete, "Eliminar")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Crear Acción Rápida") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = actionName,
                        onValueChange = { actionName = it },
                        label = { Text("Nombre de la acción (ej. Encender y HDMI)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Secuencia predeterminada: POWER (500ms) → HDMI (500ms)", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.createQuickAction(actionName, stepsList)
                    showCreateDialog = false
                }) {
                    Text("Guardar Secuencia")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("Cancelar") }
            }
        )
    }
}
