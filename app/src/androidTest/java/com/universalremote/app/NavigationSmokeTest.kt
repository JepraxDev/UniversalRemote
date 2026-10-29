package com.universalremote.app

import android.hardware.ConsumerIrManager
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.universalremote.core.model.ThemePreference
import com.universalremote.data.DataStorePreferencesRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationSmokeTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private fun click(text: String) = compose.onNodeWithText(text).performScrollTo().performClick()

    @Test fun bottomNavigationAndRealEmptyState() {
        compose.onNodeWithText("Universal Remote").assertIsDisplayed()
        compose.onNodeWithText("Dispositivos").performClick()
        compose.onNodeWithTag("devices_list").performScrollToNode(hasText("Tu primer control empieza aquí"))
        compose.onNodeWithText("Tu primer control empieza aquí").assertIsDisplayed()
        compose.onNodeWithText("Ajustes").performClick()
        compose.onNodeWithText("Apariencia").assertIsDisplayed()
        compose.onNodeWithText("Inicio").performClick()
        compose.onNodeWithText("Universal Remote").assertIsDisplayed()
        compose.onNodeWithText("Dispositivos").performClick()
        compose.onNodeWithContentDescription("Atrás").performClick()
        compose.onNodeWithText("Universal Remote").assertIsDisplayed()
    }

    @Test fun wizardRestoresSelectionsAndProjectorDemoDoesNotTransmit() {
        click("Agregar dispositivo")
        click("Proyector")
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("Proyector").assertIsSelected()
        click("Continuar")
        click("EPSON")
        click("Continuar")
        val ir = compose.activity.getSystemService(ConsumerIrManager::class.java)
        if (ir?.hasIrEmitter() != true) compose.onNodeWithText("Infrarrojo").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("Wi-Fi / Red local").performScrollTo().assertIsSelected()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("Paso 3 de 4").assertIsDisplayed()
        compose.onNodeWithContentDescription("Atrás").performClick()
        compose.onNodeWithText("EPSON").assertIsSelected()
        click("Continuar")
        click("Continuar")
        compose.onNodeWithText("Proyector · EPSON · Wi-Fi / Red local").assertIsDisplayed()
        click("Ver control de demostración")
        compose.onNodeWithText("Control de proyector").assertIsDisplayed()
        compose.onNodeWithText("Modo demostración").assertIsDisplayed()
        compose.onNodeWithContentDescription("Encendido").assertIsNotEnabled()
        compose.onNodeWithText("HDMI").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("Congelar imagen").performScrollTo().assertIsNotEnabled()
        compose.onNode(hasContentDescription("Atrás") and isEnabled()).performClick()
        compose.onNodeWithText("Configuración pendiente").assertIsDisplayed()
    }

    @Test fun tvRemoteRendersChannelsDpadAndDemoState() {
        click("Control de televisor")
        compose.onNodeWithText("Modo demostración").assertIsDisplayed()
        compose.onNodeWithText("OK").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithContentDescription("Subir canal").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithContentDescription("Silenciar").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithContentDescription("Home del televisor").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("HDMI").assertDoesNotExist()
    }

    @Test fun themeIsSavedInDataStoreAndSurvivesRecreation() {
        compose.onNodeWithText("Ajustes").performClick()
        click("Oscuro")
        compose.waitUntil(10_000) { compose.onAllNodes(hasText("Oscuro") and isSelected()).fetchSemanticsNodes().isNotEmpty() }
        val context = compose.activity.applicationContext
        val savedTheme = runBlocking { withTimeout(5_000) { DataStorePreferencesRepository(context).theme.first { it == ThemePreference.DARK } } }
        assertEquals(ThemePreference.DARK, savedTheme)
        compose.activityRule.scenario.recreate()
        compose.waitUntil(10_000) { compose.onAllNodes(hasText("Oscuro") and isSelected()).fetchSemanticsNodes().isNotEmpty() }
        click("Claro")
        compose.waitUntil(10_000) { compose.onAllNodes(hasText("Claro") and isSelected()).fetchSemanticsNodes().isNotEmpty() }
        click("Sistema")
    }
}
