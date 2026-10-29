package com.universalremote.core.designsystem

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.universalremote.core.model.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Page(title: String, onBack: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    Scaffold(containerColor = MaterialTheme.colorScheme.background, topBar = { AppTopBar(title, onBack) }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier.widthIn(max = 840.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp), content = content,
            )
        }
    }
}
@Composable
fun InfoCard(title: String, detail: String) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(detail, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
@Composable
fun irText(state: IrAvailability?): String = stringResource(when (state) {
    IrAvailability.Available -> R.string.ir_available
    IrAvailability.NoEmitter -> R.string.ir_absent
    IrAvailability.ServiceUnavailable -> R.string.ir_service
    IrAvailability.PlatformError -> R.string.ir_error
    null -> R.string.checking
})
@Composable
fun deviceTypeText(type: DeviceType) = stringResource(if (type == DeviceType.TV) R.string.tv else R.string.projector)
@Composable
fun connectionText(type: ConnectionType) = stringResource(if (type == ConnectionType.WIFI) R.string.wifi else R.string.infrared)
@Composable
fun brandText(brand: DeviceBrand) = if (brand == DeviceBrand.OTHER) stringResource(R.string.other_brand) else brand.id
