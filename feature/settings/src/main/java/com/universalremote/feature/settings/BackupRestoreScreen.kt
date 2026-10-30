package com.universalremote.feature.settings

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.universalremote.core.designsystem.AppTopBar
import com.universalremote.data.BackupImportResult
import com.universalremote.data.BackupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class BackupUiState(
    val isExporting: Boolean = false,
    val isImporting: Boolean = false,
    val message: String? = null,
    val isError: Boolean = false
)

@HiltViewModel
class BackupRestoreViewModel @Inject constructor(
    private val backupRepository: BackupRepository
) : ViewModel() {

    private val mutableState = MutableStateFlow(BackupUiState())
    val state: StateFlow<BackupUiState> = mutableState.asStateFlow()

    fun exportToUri(context: Context, uri: Uri) {
        mutableState.update { it.copy(isExporting = true, message = "Exportando datos…", isError = false) }
        viewModelScope.launch {
            val success = withContext(Dispatchers.IO) {
                try {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        backupRepository.exportToJson(out)
                    } ?: false
                } catch (_: Exception) {
                    false
                }
            }
            if (success) {
                mutableState.update { it.copy(isExporting = false, message = "Respaldo exportado exitosamente", isError = false) }
            } else {
                mutableState.update { it.copy(isExporting = false, message = "Error exportando respaldo", isError = true) }
            }
        }
    }

    fun importFromUri(context: Context, uri: Uri) {
        mutableState.update { it.copy(isImporting = true, message = "Importando respaldo…", isError = false) }
        viewModelScope.launch {
            val result: BackupImportResult = withContext(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(uri)?.use { inStream ->
                        backupRepository.importFromJson(inStream)
                    } ?: BackupImportResult(false, errorMessage = "No se pudo abrir el archivo")
                } catch (e: Exception) {
                    BackupImportResult(false, errorMessage = e.message)
                }
            }

            if (result.success) {
                val msg = "Respaldo restaurado: ${result.devicesCount} dispositivos, ${result.locationsCount} ubicaciones, ${result.quickActionsCount} acciones rápidas"
                mutableState.update { it.copy(isImporting = false, message = msg, isError = false) }
            } else {
                val msg = "Error al importar: ${result.errorMessage ?: "Archivo no válido"}"
                mutableState.update { it.copy(isImporting = false, message = msg, isError = true) }
            }
        }
    }
}

@Composable
fun BackupRestoreScreen(
    viewModel: BackupRestoreViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            viewModel.exportToUri(context, uri)
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importFromUri(context, uri)
        }
    }

    Scaffold(
        topBar = { AppTopBar("Respaldo y Restauración", onBack) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Protección de privacidad y credenciales", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Los respaldos incluyen dispositivos, ubicaciones, diseños de controles y acciones rápidas. Por seguridad, se EXCLUYEN estrictamente contraseñas, tokens y claves de emparejamiento (almacenados en Android KeyStore).",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Button(
                onClick = { exportLauncher.launch("UniversalRemote_Backup.json") },
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
            ) {
                Icon(Icons.Default.Download, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Exportar respaldo JSON (SAF)")
            }

            OutlinedButton(
                onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
            ) {
                Icon(Icons.Default.Upload, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Restaurar desde archivo JSON")
            }

            if (state.message != null) {
                Text(
                    state.message!!,
                    color = if (state.isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
