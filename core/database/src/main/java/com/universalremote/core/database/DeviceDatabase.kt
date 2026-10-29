package com.universalremote.core.database

import androidx.room.*
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
    val irProfileId: String?,
    val roomName: String?,
    val isFavorite: Boolean,
)

@Entity(tableName = "ir_assessments")
data class IrAssessmentEntity(
    @PrimaryKey val id: String, // deviceId or profileId depending on context, usually deviceId
    val profileId: String,
    val status: String,
    val testedAt: Long,
    val testCommand: String,
    val supportedCommands: String,
)

fun DeviceEntity.toModel() = Device(id, name, DeviceBrand(brand), model,
    DeviceType.valueOf(deviceType), ConnectionType.valueOf(connectionType),
    ipAddress, port, irProfileId, roomName, isFavorite)
fun Device.toEntity() = DeviceEntity(id, name, brand.id, model,
    deviceType.name, connectionType.name, ipAddress, port, irProfileId, roomName, isFavorite)

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
interface IrAssessmentDao {
    @Query("SELECT * FROM ir_assessments WHERE id = :id")
    suspend fun find(id: String): IrAssessmentEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(assessment: IrAssessmentEntity)
    @Query("DELETE FROM ir_assessments WHERE id = :id")
    suspend fun delete(id: String)
}

@Database(
    entities = [DeviceEntity::class, IrAssessmentEntity::class],
    version = 2,
    exportSchema = true
)
abstract class DeviceDatabase : RoomDatabase() {
    abstract fun devices(): DeviceDao
    abstract fun assessments(): IrAssessmentDao

    companion object {
        val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `ir_assessments` (`id` TEXT NOT NULL, `profileId` TEXT NOT NULL, `status` TEXT NOT NULL, `testedAt` INTEGER NOT NULL, `testCommand` TEXT NOT NULL, `supportedCommands` TEXT NOT NULL, PRIMARY KEY(`id`))")
            }
        }
    }
}
