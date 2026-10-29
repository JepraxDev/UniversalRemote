package com.universalremote.domain

import com.universalremote.core.model.*

enum class DeviceSort { NAME, FAVORITES }
data class DeviceFilter(val query: String = "", val type: DeviceType? = null,
                        val connection: ConnectionType? = null, val sort: DeviceSort = DeviceSort.FAVORITES)

fun filterDevices(devices: List<Device>, filter: DeviceFilter): List<Device> {
    val query = filter.query.trim()
    val filtered = devices.filter { device ->
        (filter.type == null || device.deviceType == filter.type) &&
            (filter.connection == null || device.connectionType == filter.connection) &&
            listOf(device.name, device.brand.id, device.model, device.roomName.orEmpty()).any { it.contains(query, ignoreCase = true) }
    }
    val byName = compareBy<Device, String>(String.CASE_INSENSITIVE_ORDER) { it.name }.thenBy { it.id }
    return filtered.sortedWith(if (filter.sort == DeviceSort.FAVORITES) compareByDescending<Device> { it.isFavorite }.then(byName) else byName)
}
