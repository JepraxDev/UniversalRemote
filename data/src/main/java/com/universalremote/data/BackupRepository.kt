package com.universalremote.data

import com.universalremote.core.database.DeviceDatabase
import com.universalremote.core.database.QuickActionEntity
import com.universalremote.core.database.toEntity
import com.universalremote.core.database.toModel
import com.universalremote.core.model.*
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.io.OutputStream

data class BackupImportResult(
    val success: Boolean,
    val devicesCount: Int = 0,
    val locationsCount: Int = 0,
    val quickActionsCount: Int = 0,
    val errorMessage: String? = null
)

class BackupRepository(
    private val database: DeviceDatabase
) {

    suspend fun exportToJson(outputStream: OutputStream): Boolean {
        return try {
            val devices = database.devices().observeAll().first().map { it.toModel() }
            val locations = database.locations().observeAll().first().map { it.toModel() }
            val quickActions = database.quickActions().observeAll().first().map { actionEntity ->
                val steps = database.quickActions().getStepsForAction(actionEntity.id).map { it.toModel() }
                QuickAction(
                    id = actionEntity.id,
                    name = actionEntity.name,
                    deviceId = actionEntity.deviceId,
                    steps = steps,
                    createdAt = actionEntity.createdAt
                )
            }

            val root = JSONObject()
            root.put("version", 6)
            root.put("exportedAt", System.currentTimeMillis())

            val devicesArray = JSONArray()
            for (d in devices) {
                val obj = JSONObject()
                obj.put("id", d.id)
                obj.put("name", d.name)
                obj.put("brand", d.brand.id)
                obj.put("model", d.model)
                obj.put("deviceType", d.deviceType.name)
                obj.put("connectionType", d.connectionType.name)
                obj.put("ipAddress", d.ipAddress ?: "")
                obj.put("port", d.port ?: 0)
                obj.put("protocolId", d.protocolId ?: "")
                obj.put("irProfileId", d.irProfileId ?: "")
                obj.put("roomName", d.roomName ?: "")
                obj.put("locationTag", d.locationTag ?: "")
                obj.put("locationId", d.locationId ?: "")
                obj.put("compatibilityStatus", d.compatibilityStatus.name)
                obj.put("isFavorite", d.isFavorite)
                devicesArray.put(obj)
            }
            root.put("devices", devicesArray)

            val locationsArray = JSONArray()
            for (l in locations) {
                val obj = JSONObject()
                obj.put("id", l.id)
                obj.put("name", l.name)
                obj.put("createdAt", l.createdAt)
                locationsArray.put(obj)
            }
            root.put("locations", locationsArray)

            val actionsArray = JSONArray()
            for (a in quickActions) {
                val obj = JSONObject()
                obj.put("id", a.id)
                obj.put("name", a.name)
                obj.put("deviceId", a.deviceId)
                obj.put("createdAt", a.createdAt)

                val stepsArray = JSONArray()
                for (s in a.steps) {
                    val sObj = JSONObject()
                    sObj.put("id", s.id)
                    sObj.put("actionId", s.actionId)
                    sObj.put("stepOrder", s.stepOrder)
                    sObj.put("command", s.command.name)
                    sObj.put("delayMs", s.delayMs)
                    stepsArray.put(sObj)
                }
                obj.put("steps", stepsArray)
                actionsArray.put(obj)
            }
            root.put("quickActions", actionsArray)

            outputStream.write(root.toString(2).toByteArray(Charsets.UTF_8))
            outputStream.flush()
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun importFromJson(inputStream: InputStream): BackupImportResult {
        return try {
            val jsonText = inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val root = JSONObject(jsonText)

            val version = root.optInt("version", 0)
            if (version == 0) {
                return BackupImportResult(false, errorMessage = "Formato de archivo inválido")
            }

            var devicesCount = 0
            var locationsCount = 0
            var quickActionsCount = 0

            if (root.has("locations")) {
                val locationsArray = root.getJSONArray("locations")
                for (i in 0 until locationsArray.length()) {
                    val obj = locationsArray.getJSONObject(i)
                    val loc = Location(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                    database.locations().insert(loc.toEntity())
                    locationsCount++
                }
            }

            if (root.has("devices")) {
                val devicesArray = root.getJSONArray("devices")
                for (i in 0 until devicesArray.length()) {
                    val obj = devicesArray.getJSONObject(i)
                    val dev = Device(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        brand = DeviceBrand(obj.getString("brand")),
                        model = obj.optString("model", ""),
                        deviceType = DeviceType.valueOf(obj.getString("deviceType")),
                        connectionType = ConnectionType.valueOf(obj.getString("connectionType")),
                        ipAddress = obj.optString("ipAddress", "").takeIf { it.isNotBlank() },
                        port = obj.optInt("port", 0).takeIf { it > 0 },
                        protocolId = obj.optString("protocolId", "").takeIf { it.isNotBlank() },
                        irProfileId = obj.optString("irProfileId", "").takeIf { it.isNotBlank() },
                        roomName = obj.optString("roomName", "").takeIf { it.isNotBlank() },
                        locationTag = obj.optString("locationTag", "").takeIf { it.isNotBlank() },
                        locationId = obj.optString("locationId", "").takeIf { it.isNotBlank() },
                        compatibilityStatus = try { CompatibilityStatus.valueOf(obj.optString("compatibilityStatus", "VERIFIED_PROTOCOL")) } catch (_: Exception) { CompatibilityStatus.VERIFIED_PROTOCOL },
                        isFavorite = obj.optBoolean("isFavorite", false)
                    )
                    database.devices().insert(dev.toEntity())
                    devicesCount++
                }
            }

            if (root.has("quickActions")) {
                val actionsArray = root.getJSONArray("quickActions")
                for (i in 0 until actionsArray.length()) {
                    val obj = actionsArray.getJSONObject(i)
                    val actionId = obj.getString("id")
                    val actionName = obj.getString("name")
                    val deviceId = obj.getString("deviceId")
                    val createdAt = obj.optLong("createdAt", System.currentTimeMillis())

                    val stepsList = mutableListOf<QuickActionStep>()
                    if (obj.has("steps")) {
                        val stepsArray = obj.getJSONArray("steps")
                        for (j in 0 until stepsArray.length()) {
                            val sObj = stepsArray.getJSONObject(j)
                            stepsList.add(
                                QuickActionStep(
                                    id = sObj.getString("id"),
                                    actionId = sObj.getString("actionId"),
                                    stepOrder = sObj.getInt("stepOrder"),
                                    command = RemoteCommand.valueOf(sObj.getString("command")),
                                    delayMs = sObj.optLong("delayMs", 500L)
                                )
                            )
                        }
                    }

                    val quickAction = QuickAction(actionId, actionName, deviceId, stepsList, createdAt)
                    database.quickActions().insertAction(QuickActionEntity(quickAction.id, quickAction.name, quickAction.deviceId, quickAction.createdAt))
                    database.quickActions().insertSteps(stepsList.map { it.toEntity() })
                    quickActionsCount++
                }
            }

            BackupImportResult(true, devicesCount, locationsCount, quickActionsCount)
        } catch (e: Exception) {
            BackupImportResult(false, errorMessage = e.message ?: "Error procesando archivo de respaldo")
        }
    }
}
