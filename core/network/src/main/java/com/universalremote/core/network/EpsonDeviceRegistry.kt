package com.universalremote.core.network

import android.content.Context
import org.json.JSONArray

data class EpsonDeviceProfile(
    val id: String, val brand: String, val model: String, val family: String,
    val connections: List<String>, val protocols: List<String>, val commands: List<String>,
    val capabilities: List<String>, val sources: List<String>, val authentication: String,
    val verification: String, val references: List<String>, val irProfileIds: List<String>, val escVp21: String
)

/** Model data is packaged separately from protocol engines. No family-wide fallback. */
class EpsonDeviceRegistry(context: Context) {
    val profiles: List<EpsonDeviceProfile> = context.assets.open("epson/devices.json").bufferedReader().use {
        val array = JSONArray(it.readText())
        (0 until array.length()).map { index ->
            val row = array.getJSONObject(index)
            fun strings(key: String): List<String> = row.getJSONArray(key).let { values ->
                (0 until values.length()).map(values::getString)
            }
            EpsonDeviceProfile(row.getString("id"), row.getString("brand"), row.getString("model"), row.getString("family"),
                strings("connections"), strings("protocols"), strings("commands"), strings("capabilities"), strings("sources"),
                row.getString("authentication"), row.getString("verification"), strings("references"), strings("irProfileIds"), row.getString("escVp21"))
        }.also { profiles -> require(profiles.map { it.id }.distinct().size == profiles.size) }
    }
    fun find(model: String) = profiles.firstOrNull { it.model.equals(model, true) }
}
