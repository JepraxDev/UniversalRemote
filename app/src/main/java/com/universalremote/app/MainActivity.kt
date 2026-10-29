package com.universalremote.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.universalremote.core.designsystem.*
import com.universalremote.core.model.ConnectionType
import com.universalremote.core.model.Device
import com.universalremote.core.model.DeviceType
import com.universalremote.core.model.ThemePreference
import com.universalremote.feature.home.*
import com.universalremote.feature.devices.*
import com.universalremote.feature.discovery.*
import com.universalremote.feature.remote.*
import com.universalremote.feature.settings.*
import dagger.hilt.android.AndroidEntryPoint

private enum class Destination { HOME, DEVICES, ADD, SETUP, IR_SETUP, REMOTE_TV, REMOTE_PROJECTOR, SETTINGS }
private fun NavHostController.open(destination: Destination) { navigate(destination.name) { launchSingleTop = true } }
private fun NavHostController.openTab(tab: MainTab) {
    navigate(tab.name) {
        popUpTo(Destination.HOME.name) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings: SettingsViewModel = viewModel()
            val home: HomeViewModel = viewModel()
            val devices: DevicesViewModel = viewModel()
            val add: AddDeviceViewModel = viewModel()
            val irSetup: IrSetupViewModel = viewModel()
            val remote: RemoteViewModel = viewModel()
            val appearance by settings.state.collectAsStateWithLifecycle()
            val dark = appearance.theme == ThemePreference.DARK ||
                (appearance.theme == ThemePreference.SYSTEM && isSystemInDarkTheme())
            SideEffect {
                val style = if (dark) SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    else SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            }
            UniversalRemoteTheme(appearance.theme) {
                val nav = rememberNavController()
                val entry by nav.currentBackStackEntryAsState()
                val selectedTab = MainTab.entries.firstOrNull { it.name == entry?.destination?.route }
                val back: () -> Unit = { nav.popBackStack() }
                val preview: (DeviceType) -> Unit = { type ->
                    remote.loadDevice(null)
                    nav.open(if (type == DeviceType.TV) Destination.REMOTE_TV else Destination.REMOTE_PROJECTOR)
                }
                val openDevice: (Device) -> Unit = { device ->
                    remote.loadDevice(device.id)
                    nav.open(if (device.deviceType == DeviceType.TV) Destination.REMOTE_TV else Destination.REMOTE_PROJECTOR)
                }
                Scaffold(contentWindowInsets = WindowInsets(0, 0, 0, 0), bottomBar = {
                    if (selectedTab != null) BottomNavigation(selectedTab, nav::openTab)
                }) { padding ->
                    NavHost(navController = nav, startDestination = Destination.HOME.name,
                        modifier = Modifier.padding(padding).consumeWindowInsets(padding),
                        enterTransition = { fadeIn(tween(180)) }, exitTransition = { fadeOut(tween(120)) },
                        popEnterTransition = { fadeIn(tween(180)) }, popExitTransition = { fadeOut(tween(120)) }) {
                        composable(Destination.HOME.name) {
                            HomeScreen(
                                viewModel = home,
                                onDevices = { nav.openTab(MainTab.DEVICES) },
                                onAdd = { nav.open(Destination.ADD) },
                                onRemote = preview,
                                onOpenDevice = openDevice
                            )
                        }
                        composable(Destination.DEVICES.name) {
                            DevicesScreen(
                                viewModel = devices,
                                onBack = back,
                                onAdd = { nav.open(Destination.ADD) },
                                onPreview = preview,
                                onOpenDevice = openDevice
                            )
                        }
                        composable(Destination.ADD.name) {
                            val state by add.state.collectAsStateWithLifecycle()
                            BackHandler(enabled = state.step > 0) { add.previous() }
                            AddDeviceScreen(add, back) {
                                if (state.connection == ConnectionType.INFRARED) {
                                    irSetup.initProfiles(state.type, state.brand)
                                    nav.open(Destination.IR_SETUP)
                                } else {
                                    nav.open(Destination.SETUP)
                                }
                            }
                        }
                        composable(Destination.SETUP.name) { SetupInfoScreen(add, back, preview) }
                        composable(Destination.IR_SETUP.name) {
                            IrSetupScreen(
                                viewModel = irSetup,
                                onBack = back,
                                onDeviceSaved = { deviceId, type ->
                                    remote.loadDevice(deviceId)
                                    nav.open(if (type == DeviceType.TV) Destination.REMOTE_TV else Destination.REMOTE_PROJECTOR)
                                }
                            )
                        }
                        composable(Destination.REMOTE_TV.name) { RemoteScreen(DeviceType.TV, remote, back) }
                        composable(Destination.REMOTE_PROJECTOR.name) { RemoteScreen(DeviceType.PROJECTOR, remote, back) }
                        composable(Destination.SETTINGS.name) { SettingsScreen(settings, BuildConfig.VERSION_NAME, BuildConfig.STAGE, back) }
                    }
                }
            }
        }
    }
}
