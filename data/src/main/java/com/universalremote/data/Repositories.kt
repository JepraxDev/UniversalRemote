package com.universalremote.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.universalremote.core.database.*
import com.universalremote.core.infrared.AndroidInfraredController
import com.universalremote.core.infrared.InfraredController
import com.universalremote.core.model.*
import com.universalremote.domain.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.map
import javax.inject.Singleton

class RoomDeviceRepository(private val dao: DeviceDao) : DeviceRepository {
    override fun observeDevices() = dao.observeAll().map { rows -> rows.map { it.toModel() } }
    override suspend fun find(id: String) = dao.find(id)?.toModel()
    override suspend fun insert(device: Device) = dao.insert(device.toEntity())
    override suspend fun update(device: Device) = dao.update(device.toEntity())
    override suspend fun delete(id: String) = dao.delete(id)
}
private val Context.preferences by preferencesDataStore(name = "preferences")
class DataStorePreferencesRepository(context: Context) : PreferencesRepository {
    private val store = context.preferences
    private val themeKey = stringPreferencesKey("theme")
    override val theme = store.data.map { preferences ->
        ThemePreference.entries.firstOrNull { it.name == preferences[themeKey] } ?: ThemePreference.SYSTEM
    }
    override suspend fun setTheme(theme: ThemePreference) {
        store.edit { it[themeKey] = theme.name }
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

@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    @Provides @Singleton
    fun database(@ApplicationContext context: Context): DeviceDatabase =
        Room.databaseBuilder(context, DeviceDatabase::class.java, "universal-remote.db")
            .addMigrations(DeviceDatabase.MIGRATION_1_2)
            .build()
    @Provides @Singleton
    fun devices(database: DeviceDatabase): DeviceRepository = RoomDeviceRepository(database.devices())
    @Provides @Singleton
    fun preferences(@ApplicationContext context: Context): PreferencesRepository = DataStorePreferencesRepository(context)
    @Provides @Singleton
    fun infrared(@ApplicationContext context: Context): InfraredController = AndroidInfraredController(context)
    @Provides
    fun availability(controller: InfraredController): IrAvailabilityProvider = controller
    @Provides @Singleton
    fun catalog(): IrProfileCatalog = com.universalremote.protocol.ir.VerifiedIrCatalog()
    @Provides @Singleton
    fun infraredRepository(
        controller: InfraredController,
        catalog: IrProfileCatalog,
        database: DeviceDatabase
    ): InfraredRepository = DefaultInfraredRepository(controller, catalog, database.devices(), database.assessments())
    @Provides
    fun sendRemoteCommand(repository: InfraredRepository): SendRemoteCommand = SendRemoteCommand(repository)
}
