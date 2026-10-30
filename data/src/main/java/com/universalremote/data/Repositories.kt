package com.universalremote.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.universalremote.core.database.*
import com.universalremote.core.infrared.AndroidInfraredController
import com.universalremote.core.infrared.InfraredController
import com.universalremote.core.model.*
import com.universalremote.core.network.CompositeNetworkDeviceDiscovery
import com.universalremote.core.network.NetworkDeviceDiscovery
import com.universalremote.domain.*
import com.universalremote.protocol.androidtv.AndroidTvProtocol
import com.universalremote.protocol.api.ProtocolRegistry
import com.universalremote.protocol.api.RemoteProtocol
import com.universalremote.protocol.ir.VerifiedIrCatalog
import com.universalremote.protocol.lgwebos.LgWebOsProtocol
import com.universalremote.protocol.pjlink.PjLinkProtocol
import com.universalremote.protocol.samsung.SamsungWsProtocol
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Locale
import javax.inject.Singleton

class RoomDeviceRepository(private val dao: DeviceDao) : DeviceRepository {
    override fun observeDevices() = dao.observeAll().map { rows -> rows.map { it.toModel() } }
    override suspend fun find(id: String) = dao.find(id)?.toModel()
    override suspend fun insert(device: Device) = dao.insert(device.toEntity())
    override suspend fun update(device: Device) = dao.update(device.toEntity())
    override suspend fun delete(id: String) = dao.delete(id)
}

class RoomLocationRepository(private val dao: LocationDao) : LocationRepository {
    override fun observeLocations(): Flow<List<Location>> = dao.observeAll().map { rows -> rows.map { it.toModel() } }
    override suspend fun find(id: String): Location? = dao.find(id)?.toModel()
    override suspend fun insert(location: Location) = dao.insert(location.toEntity())
    override suspend fun delete(id: String) = dao.delete(id)
}

class RoomRemoteLayoutRepository(private val dao: RemoteLayoutDao) : RemoteLayoutRepository {
    override suspend fun getLayout(deviceId: String): RemoteLayoutPreference? = dao.find(deviceId)?.toModel()
    override suspend fun saveLayout(preference: RemoteLayoutPreference) = dao.insert(preference.toEntity())
    override suspend fun deleteLayout(deviceId: String) = dao.delete(deviceId)
}

class RoomQuickActionRepository(private val dao: QuickActionDao) : QuickActionRepository {
    override fun observeQuickActions(): Flow<List<QuickAction>> = dao.observeAll().map { rows ->
        rows.map { actionEntity ->
            val steps = dao.getStepsForAction(actionEntity.id).map { it.toModel() }
            QuickAction(actionEntity.id, actionEntity.name, actionEntity.deviceId, steps, actionEntity.createdAt)
        }
    }

    override fun observeForDevice(deviceId: String): Flow<List<QuickAction>> = dao.observeForDevice(deviceId).map { rows ->
        rows.map { actionEntity ->
            val steps = dao.getStepsForAction(actionEntity.id).map { it.toModel() }
            QuickAction(actionEntity.id, actionEntity.name, actionEntity.deviceId, steps, actionEntity.createdAt)
        }
    }

    override suspend fun find(id: String): QuickAction? {
        val actionEntity = dao.find(id) ?: return null
        val steps = dao.getStepsForAction(actionEntity.id).map { it.toModel() }
        return QuickAction(actionEntity.id, actionEntity.name, actionEntity.deviceId, steps, actionEntity.createdAt)
    }

    override suspend fun save(action: QuickAction) {
        dao.insertAction(QuickActionEntity(action.id, action.name, action.deviceId, action.createdAt))
        dao.deleteStepsForAction(action.id)
        dao.insertSteps(action.steps.map { it.toEntity() })
    }

    override suspend fun delete(id: String) {
        dao.deleteStepsForAction(id)
        dao.deleteAction(id)
    }
}

private val Context.preferences by preferencesDataStore(name = "preferences")
class DataStorePreferencesRepository(context: Context) : PreferencesRepository {
    private val store = context.preferences
    private val themeKey = stringPreferencesKey("theme")
    private val hapticKey = booleanPreferencesKey("haptic_enabled")
    private val animationsKey = booleanPreferencesKey("reduce_animations")
    private val remoteModeKey = stringPreferencesKey("remote_mode")

    override val theme = store.data.map { preferences ->
        ThemePreference.entries.firstOrNull { it.name == preferences[themeKey] } ?: ThemePreference.SYSTEM
    }
    override val hapticEnabled = store.data.map { preferences ->
        preferences[hapticKey] ?: true
    }
    override val reduceAnimations = store.data.map { preferences ->
        preferences[animationsKey] ?: false
    }
    override val remoteMode = store.data.map { preferences ->
        RemoteMode.entries.firstOrNull { it.name == preferences[remoteModeKey] } ?: RemoteMode.BASIC
    }

    override suspend fun setTheme(theme: ThemePreference) {
        store.edit { it[themeKey] = theme.name }
    }
    override suspend fun setHapticEnabled(enabled: Boolean) {
        store.edit { it[hapticKey] = enabled }
    }
    override suspend fun setReduceAnimations(reduced: Boolean) {
        store.edit { it[animationsKey] = reduced }
    }
    override suspend fun setRemoteMode(mode: RemoteMode) {
        store.edit { it[remoteModeKey] = mode.name }
    }
}

class DefaultInfraredRepository(
    private val controller: InfraredController,
    private val catalog: IrProfileCatalog,
    private val deviceDao: DeviceDao,
    private val assessmentDao: IrAssessmentDao,
    private val clock: () -> Long = { System.currentTimeMillis() }
) : InfraredRepository {
    override val profiles: List<IrProfile> get() = catalog.profiles
    override val pendingBrands: List<PendingBrandInfo> get() = catalog.pendingBrands

    override suspend fun test(profileId: String): IrResult<IrAssessment> {
        val profile = catalog.find(profileId) ?: return IrResult.Failure(IrError.PROFILE_MISSING)
        val signal = profile.commands[profile.testCommand] ?: return IrResult.Failure(IrError.COMMAND_UNSUPPORTED)

        return when (val result = controller.transmit(signal.carrierHz, signal.patternMicros)) {
            is IrResult.Success -> {
                val assessment = IrAssessment(
                    id = profileId,
                    profileId = profileId,
                    status = IrValidationStatus.UNVERIFIED,
                    testedAt = clock(),
                    testCommand = profile.testCommand,
                    supportedCommands = profile.commands.keys
                )
                assessmentDao.insert(assessment.toEntity())
                IrResult.Success(assessment)
            }
            is IrResult.Failure -> result
            is IrResult.Unavailable -> result
        }
    }

    override suspend fun assessment(id: String): IrAssessment? = assessmentDao.find(id)?.toModel()

    override suspend fun confirm(id: String, responded: Boolean): IrResult<IrAssessment> {
        val current = assessment(id) ?: run {
            val profile = catalog.find(id) ?: deviceDao.find(id)?.irProfileId?.let { catalog.find(it) }
            profile?.let {
                IrAssessment(
                    id = id,
                    profileId = it.id,
                    status = IrValidationStatus.UNVERIFIED,
                    testedAt = clock(),
                    testCommand = it.testCommand,
                    supportedCommands = it.commands.keys
                )
            }
        } ?: return IrResult.Failure(IrError.NOT_CONFIGURED)

        val updated = current.copy(
            status = if (responded) IrValidationStatus.CONFIRMED_BY_USER else IrValidationStatus.INCOMPATIBLE,
            testedAt = clock()
        )
        assessmentDao.insert(updated.toEntity())
        return IrResult.Success(updated)
    }

    override suspend fun save(id: String, name: String, room: String?): IrResult<Device> {
        val entity = deviceDao.find(id) ?: return IrResult.Failure(IrError.STORAGE_FAILURE)
        val updated = entity.copy(name = name, roomName = room)
        deviceDao.update(updated)
        return IrResult.Success(updated.toModel())
    }

    override suspend fun send(deviceId: String, command: RemoteCommand, confirmed: Boolean): IrResult<Unit> {
        val device = deviceDao.find(deviceId)?.toModel() ?: return IrResult.Failure(IrError.NOT_CONFIGURED)
        if (device.connectionType != ConnectionType.INFRARED) return IrResult.Failure(IrError.PROFILE_INCOMPATIBLE)
        val profileId = device.irProfileId ?: return IrResult.Failure(IrError.PROFILE_MISSING)
        val profile = catalog.find(profileId) ?: return IrResult.Failure(IrError.PROFILE_MISSING)

        val assessment = assessment(deviceId) ?: assessment(profileId)
        if (!confirmed && command.requiresConfirmation() && assessment?.status != IrValidationStatus.CONFIRMED_BY_USER) {
            return IrResult.Failure(IrError.CONFIRMATION_REQUIRED)
        }

        val signal = profile.commands[command] ?: return IrResult.Failure(IrError.COMMAND_UNSUPPORTED)
        return controller.transmit(signal.carrierHz, signal.patternMicros)
    }
}

class ProtocolRegistryImpl(
    secureStorage: SecureStorageManager
) : ProtocolRegistry {

    private val lg = LgWebOsProtocol(
        tokenStorage = { secureStorage.getToken(it) },
        tokenSaver = { deviceId, token -> secureStorage.saveToken(deviceId, token) }
    )
    private val pjlink = PjLinkProtocol(
        passwordStorage = { secureStorage.getToken(it) }
    )
    private val samsung = SamsungWsProtocol(
        tokenStorage = { secureStorage.getToken(it) },
        tokenSaver = { deviceId, token -> secureStorage.saveToken(deviceId, token) }
    )
    private val androidTv = AndroidTvProtocol()

    override fun getProtocol(protocolType: ProtocolType): RemoteProtocol? {
        return when (protocolType) {
            ProtocolType.LG_WEBOS -> lg
            ProtocolType.PJLINK -> pjlink
            ProtocolType.SAMSUNG_WS -> samsung
            ProtocolType.ANDROID_TV -> androidTv
            else -> null
        }
    }

    override fun getProtocolForDevice(device: Device): RemoteProtocol? {
        val type = if (!device.protocolId.isNullOrBlank() && device.protocolId != "UNKNOWN") {
            ProtocolType.fromId(device.protocolId)
        } else {
            val fullName = (device.name + " " + device.model).uppercase(Locale.ROOT)
            when {
                device.brand == DeviceBrand.LG -> ProtocolType.LG_WEBOS
                device.brand == DeviceBrand.SAMSUNG -> ProtocolType.SAMSUNG_WS
                device.brand in listOf(DeviceBrand.EPSON, DeviceBrand.BENQ, DeviceBrand.VIEWSONIC, DeviceBrand.OPTOMA, DeviceBrand.NEC) -> ProtocolType.PJLINK
                device.brand in listOf(DeviceBrand.XIAOMI, DeviceBrand.SONY, DeviceBrand.TCL, DeviceBrand.HISENSE, DeviceBrand.PHILIPS) ||
                        fullName.contains("ANDROID") || fullName.contains("MITV") || fullName.contains("GOOGLE") -> ProtocolType.ANDROID_TV
                else -> ProtocolType.ANDROID_TV
            }
        }
        return getProtocol(type)
    }
}

@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    @Provides @Singleton
    fun database(@ApplicationContext context: Context): DeviceDatabase =
        Room.databaseBuilder(context, DeviceDatabase::class.java, "universal-remote.db")
            .addMigrations(
                DeviceDatabase.MIGRATION_1_2,
                DeviceDatabase.MIGRATION_2_3,
                DeviceDatabase.MIGRATION_3_4,
                DeviceDatabase.MIGRATION_4_5
            )
            .build()

    @Provides @Singleton
    fun devices(database: DeviceDatabase): DeviceRepository = RoomDeviceRepository(database.devices())

    @Provides @Singleton
    fun locations(database: DeviceDatabase): LocationRepository = RoomLocationRepository(database.locations())

    @Provides @Singleton
    fun remoteLayouts(database: DeviceDatabase): RemoteLayoutRepository = RoomRemoteLayoutRepository(database.remoteLayouts())

    @Provides @Singleton
    fun quickActions(database: DeviceDatabase): QuickActionRepository = RoomQuickActionRepository(database.quickActions())

    @Provides @Singleton
    fun backupRepository(database: DeviceDatabase): BackupRepository = BackupRepository(database)

    @Provides @Singleton
    fun preferences(@ApplicationContext context: Context): PreferencesRepository = DataStorePreferencesRepository(context)

    @Provides @Singleton
    fun infrared(@ApplicationContext context: Context): InfraredController = AndroidInfraredController(context)

    @Provides
    fun availability(controller: InfraredController): IrAvailabilityProvider = controller

    @Provides @Singleton
    fun catalog(): IrProfileCatalog = VerifiedIrCatalog()

    @Provides @Singleton
    fun infraredRepository(
        controller: InfraredController,
        catalog: IrProfileCatalog,
        database: DeviceDatabase
    ): InfraredRepository = DefaultInfraredRepository(controller, catalog, database.devices(), database.assessments())

    @Provides @Singleton
    fun secureStorageManager(@ApplicationContext context: Context): SecureStorageManager =
        SecureStorageManager(context)

    @Provides @Singleton
    fun protocolRegistry(secureStorageManager: SecureStorageManager): ProtocolRegistry =
        ProtocolRegistryImpl(secureStorageManager)

    @Provides @Singleton
    fun capabilityResolver(
        catalog: IrProfileCatalog,
        protocolRegistry: ProtocolRegistry
    ): DeviceCapabilityResolver = DeviceCapabilityResolver(catalog, protocolRegistry)

    @Provides @Singleton
    fun compatibilityService(resolver: DeviceCapabilityResolver): DeviceCompatibilityService =
        DeviceCompatibilityService(resolver)

    @Provides @Singleton
    fun quickActionRunner(): QuickActionRunner = QuickActionRunner()

    @Provides @Singleton
    fun networkDiscovery(@ApplicationContext context: Context): NetworkDeviceDiscovery =
        CompositeNetworkDeviceDiscovery(context)

    @Provides
    fun sendRemoteCommand(repository: InfraredRepository): SendRemoteCommand = SendRemoteCommand(repository)

    @Provides
    fun sendNetworkCommand(registry: ProtocolRegistry): SendNetworkCommand = SendNetworkCommand(registry)

    @Provides
    fun openRemoteUseCase(repository: DeviceRepository): OpenRemoteUseCase = OpenRemoteUseCase(repository)
}
