package com.universalremote.core.database

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.universalremote.core.model.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "devices")
data class DeviceEntity(
    @PrimaryKey val id: String,
    val name: String,
    val brand: String,
    val model: String,
    val deviceType: String,
    val connectionType: String,
    val ipAddress: String?,
    val port: Int?,
    val protocolId: String?,
    val irProfileId: String?,
    val roomName: String?,
    val locationTag: String?,
    val locationId: String?,
    val compatibilityStatus: String,
    val isFavorite: Boolean,
    val lastConnectedAt: Long?,
    val lastUsedAt: Long?,
)

@Entity(tableName = "ir_assessments")
data class IrAssessmentEntity(
    @PrimaryKey val id: String,
    val profileId: String,
    val status: String,
    val testedAt: Long,
    val testCommand: String,
    val supportedCommands: String,
)

@Entity(tableName = "locations")
data class LocationEntity(
    @PrimaryKey val id: String,
    val name: String,
    val createdAt: Long
)

@Entity(tableName = "remote_layouts")
data class RemoteLayoutEntity(
    @PrimaryKey val deviceId: String,
    val layoutStyle: String,
    val hiddenCommands: String,
    val customOrder: String
)

@Entity(tableName = "quick_actions")
data class QuickActionEntity(
    @PrimaryKey val id: String,
    val name: String,
    val deviceId: String,
    val createdAt: Long
)

@Entity(tableName = "quick_action_steps")
data class QuickActionStepEntity(
    @PrimaryKey val id: String,
    val actionId: String,
    val stepOrder: Int,
    val command: String,
    val delayMs: Long
)

fun DeviceEntity.toModel() = Device(
    id = id,
    name = name,
    brand = DeviceBrand(brand),
    model = model,
    deviceType = DeviceType.valueOf(deviceType),
    connectionType = ConnectionType.valueOf(connectionType),
    ipAddress = ipAddress,
    port = port,
    protocolId = protocolId,
    irProfileId = irProfileId,
    roomName = roomName,
    locationTag = locationTag,
    locationId = locationId,
    compatibilityStatus = try { CompatibilityStatus.valueOf(compatibilityStatus) } catch (_: Exception) { CompatibilityStatus.VERIFIED_PROTOCOL },
    isFavorite = isFavorite,
    lastConnectedAt = lastConnectedAt,
    lastUsedAt = lastUsedAt
)

fun Device.toEntity() = DeviceEntity(
    id = id,
    name = name,
    brand = brand.id,
    model = model,
    deviceType = deviceType.name,
    connectionType = connectionType.name,
    ipAddress = ipAddress,
    port = port,
    protocolId = protocolId,
    irProfileId = irProfileId,
    roomName = roomName,
    locationTag = locationTag,
    locationId = locationId,
    compatibilityStatus = compatibilityStatus.name,
    isFavorite = isFavorite,
    lastConnectedAt = lastConnectedAt,
    lastUsedAt = lastUsedAt
)

fun LocationEntity.toModel() = Location(id, name, createdAt)
fun Location.toEntity() = LocationEntity(id, name, createdAt)

fun RemoteLayoutEntity.toModel() = RemoteLayoutPreference(
    deviceId = deviceId,
    layoutStyle = try { RemoteLayoutStyle.valueOf(layoutStyle) } catch (_: Exception) { RemoteLayoutStyle.CLASSIC },
    hiddenCommands = hiddenCommands.split(",").filter { it.isNotBlank() }.map { RemoteCommand.valueOf(it) }.toSet(),
    customOrder = customOrder.split(",").filter { it.isNotBlank() }.map { RemoteCommand.valueOf(it) }
)

fun RemoteLayoutPreference.toEntity() = RemoteLayoutEntity(
    deviceId = deviceId,
    layoutStyle = layoutStyle.name,
    hiddenCommands = hiddenCommands.joinToString(",") { it.name },
    customOrder = customOrder.joinToString(",") { it.name }
)

fun QuickActionStepEntity.toModel() = QuickActionStep(
    id = id,
    actionId = actionId,
    stepOrder = stepOrder,
    command = RemoteCommand.valueOf(command),
    delayMs = delayMs
)

fun QuickActionStep.toEntity() = QuickActionStepEntity(
    id = id,
    actionId = actionId,
    stepOrder = stepOrder,
    command = command.name,
    delayMs = delayMs
)

fun IrAssessmentEntity.toModel() = IrAssessment(
    id = id,
    profileId = profileId,
    status = IrValidationStatus.valueOf(status),
    testedAt = testedAt,
    testCommand = RemoteCommand.valueOf(testCommand),
    supportedCommands = supportedCommands.split(",").filter { it.isNotEmpty() }.map { RemoteCommand.valueOf(it) }.toSet()
)

fun IrAssessment.toEntity() = IrAssessmentEntity(
    id = id,
    profileId = profileId,
    status = status.name,
    testedAt = testedAt,
    testCommand = testCommand.name,
    supportedCommands = supportedCommands.joinToString(",") { it.name }
)

@Dao
interface DeviceDao {
    @Query("SELECT * FROM devices ORDER BY isFavorite DESC, name COLLATE NOCASE")
    fun observeAll(): Flow<List<DeviceEntity>>
    @Query("SELECT * FROM devices WHERE id = :id")
    suspend fun find(id: String): DeviceEntity?
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(device: DeviceEntity)
    @Update
    suspend fun update(device: DeviceEntity)
    @Query("DELETE FROM devices WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface LocationDao {
    @Query("SELECT * FROM locations ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<LocationEntity>>
    @Query("SELECT * FROM locations WHERE id = :id")
    suspend fun find(id: String): LocationEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(location: LocationEntity)
    @Query("DELETE FROM locations WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface RemoteLayoutDao {
    @Query("SELECT * FROM remote_layouts WHERE deviceId = :deviceId")
    suspend fun find(deviceId: String): RemoteLayoutEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(layout: RemoteLayoutEntity)
    @Query("DELETE FROM remote_layouts WHERE deviceId = :deviceId")
    suspend fun delete(deviceId: String)
}

@Dao
interface QuickActionDao {
    @Query("SELECT * FROM quick_actions ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<QuickActionEntity>>
    @Query("SELECT * FROM quick_actions WHERE deviceId = :deviceId")
    fun observeForDevice(deviceId: String): Flow<List<QuickActionEntity>>
    @Query("SELECT * FROM quick_actions WHERE id = :id")
    suspend fun find(id: String): QuickActionEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAction(action: QuickActionEntity)
    @Query("DELETE FROM quick_actions WHERE id = :id")
    suspend fun deleteAction(id: String)

    @Query("SELECT * FROM quick_action_steps WHERE actionId = :actionId ORDER BY stepOrder ASC")
    suspend fun getStepsForAction(actionId: String): List<QuickActionStepEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSteps(steps: List<QuickActionStepEntity>)
    @Query("DELETE FROM quick_action_steps WHERE actionId = :actionId")
    suspend fun deleteStepsForAction(actionId: String)
}

@Dao
interface IrAssessmentDao {
    @Query("SELECT * FROM ir_assessments WHERE id = :id")
    suspend fun find(id: String): IrAssessmentEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(assessment: IrAssessmentEntity)
    @Query("DELETE FROM ir_assessments WHERE id = :id")
    suspend fun delete(id: String)
}

@Database(
    entities = [
        DeviceEntity::class,
        IrAssessmentEntity::class,
        LocationEntity::class,
        RemoteLayoutEntity::class,
        QuickActionEntity::class,
        QuickActionStepEntity::class
    ],
    version = 5,
    exportSchema = true
)
abstract class DeviceDatabase : RoomDatabase() {
    abstract fun devices(): DeviceDao
    abstract fun assessments(): IrAssessmentDao
    abstract fun locations(): LocationDao
    abstract fun remoteLayouts(): RemoteLayoutDao
    abstract fun quickActions(): QuickActionDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `ir_assessments` (`id` TEXT NOT NULL, `profileId` TEXT NOT NULL, `status` TEXT NOT NULL, `testedAt` INTEGER NOT NULL, `testCommand` TEXT NOT NULL, `supportedCommands` TEXT NOT NULL, PRIMARY KEY(`id`))")
            }
        }
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `devices` ADD COLUMN `protocolId` TEXT DEFAULT NULL")
            }
        }
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `devices` ADD COLUMN `locationTag` TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE `devices` ADD COLUMN `compatibilityStatus` TEXT NOT NULL DEFAULT 'VERIFIED_PROTOCOL'")
            }
        }
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `devices` ADD COLUMN `locationId` TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE `devices` ADD COLUMN `lastConnectedAt` INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE `devices` ADD COLUMN `lastUsedAt` INTEGER DEFAULT NULL")

                db.execSQL("CREATE TABLE IF NOT EXISTS `locations` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")
                db.execSQL("CREATE TABLE IF NOT EXISTS `remote_layouts` (`deviceId` TEXT NOT NULL, `layoutStyle` TEXT NOT NULL, `hiddenCommands` TEXT NOT NULL, `customOrder` TEXT NOT NULL, PRIMARY KEY(`deviceId`))")
                db.execSQL("CREATE TABLE IF NOT EXISTS `quick_actions` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `deviceId` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")
                db.execSQL("CREATE TABLE IF NOT EXISTS `quick_action_steps` (`id` TEXT NOT NULL, `actionId` TEXT NOT NULL, `stepOrder` INTEGER NOT NULL, `command` TEXT NOT NULL, `delayMs` INTEGER NOT NULL, PRIMARY KEY(`id`))")
            }
        }
    }
}
