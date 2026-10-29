package com.universalremote.feature.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SettingsRemote
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class SettingsUiState(val theme: ThemePreference = ThemePreference.SYSTEM, val ir: IrAvailability? = null, val readError: Boolean = false, val saveError: Boolean = false)
@HiltViewModel
class SettingsViewModel @Inject constructor(private val preferences: PreferencesRepository, infrared: IrAvailabilityProvider) : ViewModel() {
    private val mutableState = MutableStateFlow(SettingsUiState())
    val state = mutableState.asStateFlow()
    init {
        viewModelScope.launch {
            val ir = withContext(Dispatchers.IO) { infrared.availability() }
            mutableState.update { it.copy(ir = ir) }
        }
        viewModelScope.launch {
            preferences.theme.catch { mutableState.update { it.copy(readError = true) } }
                .collect { theme -> mutableState.update { it.copy(theme = theme, readError = false) } }
        }
    }
    fun setTheme(theme: ThemePreference) {
        viewModelScope.launch {
            try {
                preferences.setTheme(theme)
                mutableState.update { it.copy(saveError = false) }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { mutableState.update { it.copy(saveError = true) } }
        }
    }
}
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel, version: String, stage: Int, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Page(stringResource(R.string.settings), onBack) {
        Text(stringResource(R.string.theme), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.appearance_detail), color = MaterialTheme.colorScheme.onSurfaceVariant)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemePreference.entries.forEach { theme ->
                FilterChip(state.theme == theme, { viewModel.setTheme(theme) }, { Text(stringResource(when (theme) {
                    ThemePreference.SYSTEM -> R.string.theme_system
                    ThemePreference.LIGHT -> R.string.theme_light
                    ThemePreference.DARK -> R.string.theme_dark
                })) }, modifier = Modifier.heightIn(min = 56.dp))
            }
        }
        if (state.readError) Text(stringResource(R.string.read_error))
        if (state.saveError) Text(stringResource(R.string.save_error))
        StatusIndicator(irText(state.ir), state.ir == IrAvailability.Available, Icons.Default.SettingsRemote)
        InfoCard(stringResource(R.string.compatibility), stringResource(R.string.compatibility_detail))
        InfoCard(stringResource(R.string.about), stringResource(R.string.version, version, stage))
    }
}
