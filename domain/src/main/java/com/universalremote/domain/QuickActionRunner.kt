package com.universalremote.domain

import com.universalremote.core.model.Device
import com.universalremote.core.model.QuickAction
import com.universalremote.core.model.QuickActionStep
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

interface QuickActionRepository {
    fun observeQuickActions(): Flow<List<QuickAction>>
    fun observeForDevice(deviceId: String): Flow<List<QuickAction>>
    suspend fun find(id: String): QuickAction?
    suspend fun save(action: QuickAction)
    suspend fun delete(id: String)
}

sealed interface QuickActionExecutionState {
    data object Idle : QuickActionExecutionState
    data class Running(val actionName: String, val currentStep: Int, val totalSteps: Int, val stepDescription: String) : QuickActionExecutionState
    data object Completed : QuickActionExecutionState
    data class Failed(val error: String) : QuickActionExecutionState
    data object Cancelled : QuickActionExecutionState
}

class QuickActionRunner {
    private val mutableState = MutableStateFlow<QuickActionExecutionState>(QuickActionExecutionState.Idle)
    val state: StateFlow<QuickActionExecutionState> = mutableState

    private var cancelled = false

    fun cancel() {
        cancelled = true
        mutableState.value = QuickActionExecutionState.Cancelled
    }

    suspend fun execute(action: QuickAction, sendCommandFunc: suspend (QuickActionStep) -> Boolean) {
        cancelled = false
        val total = action.steps.size
        if (total == 0) {
            mutableState.value = QuickActionExecutionState.Completed
            return
        }

        for ((index, step) in action.steps.sortedBy { it.stepOrder }.withIndex()) {
            if (cancelled) return
            mutableState.value = QuickActionExecutionState.Running(
                actionName = action.name,
                currentStep = index + 1,
                totalSteps = total,
                stepDescription = "Ejecutando ${step.command.name} (${step.delayMs}ms)"
            )

            val success = sendCommandFunc(step)
            if (!success) {
                mutableState.value = QuickActionExecutionState.Failed("Error ejecutando paso ${index + 1}: ${step.command.name}")
                return
            }

            if (step.delayMs > 0) {
                delay(step.delayMs)
            }
        }

        if (!cancelled) {
            mutableState.value = QuickActionExecutionState.Completed
        }
    }
}
