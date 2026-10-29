package com.universalremote.app

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.universalremote.core.designsystem.*
import com.universalremote.core.model.*
import com.universalremote.feature.remote.RemoteScreen
import org.junit.Rule
import org.junit.Test

class DesignComponentsTest {
    @get:Rule val compose = createComposeRule()
    @Test fun connectionSelectorExplainsMissingIrAndAllowsWifi() {
        compose.setContent {
            UniversalRemoteTheme(ThemePreference.LIGHT) {
                ConnectionMethodSelector(ConnectionType.WIFI, IrAvailability.NoEmitter) {}
            }
        }
        compose.onNodeWithText("Infrarrojo").assertIsNotEnabled()
        compose.onNodeWithText("Wi-Fi / Red local").assertIsEnabled().assertIsSelected()
        compose.onNodeWithText("Este celular no tiene emisor IR.", substring = true).assertExists()
    }
    @Test fun availableIrCanBeSelectedWithoutClaimingConnection() {
        compose.setContent {
            var selected by remember { mutableStateOf(ConnectionType.WIFI) }
            UniversalRemoteTheme(ThemePreference.DARK) {
                ConnectionMethodSelector(selected, IrAvailability.Available) { selected = it }
            }
        }
        compose.onNodeWithText("Infrarrojo").performClick().assertIsSelected()
        compose.onNodeWithText("Wi-Fi / Red local").assertIsNotSelected().performClick().assertIsSelected()
    }
    @Test fun projectorHidesAdditionalCommandsOutsideCapabilities() {
        compose.setContent {
            UniversalRemoteTheme(ThemePreference.DARK) {
                RemoteScreen(DeviceType.PROJECTOR, {}, DeviceCapabilities(setOf(RemoteCommand.POWER, RemoteCommand.HDMI)))
            }
        }
        compose.onNodeWithContentDescription("Encendido").assertIsNotEnabled()
        compose.onNodeWithText("HDMI").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("Computer / VGA").assertDoesNotExist()
        compose.onNodeWithText("LAN").assertDoesNotExist()
        compose.onNodeWithText("Congelar imagen").assertDoesNotExist()
    }
}
