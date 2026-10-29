package com.universalremote.domain

import com.universalremote.core.model.*
import org.junit.Assert.*
import org.junit.Test

class FilterDevicesTest {
    private val tv = Device("1", "Sala", DeviceBrand.LG, "C3", DeviceType.TV, ConnectionType.WIFI, roomName = "Aula Norte")
    private val projector = Device("2", "Auditorio", DeviceBrand.EPSON, "EB-X", DeviceType.PROJECTOR, ConnectionType.INFRARED, isFavorite = true)
    @Test fun searchUsesLocationBrandAndModelIgnoringCaseAndWhitespace() {
        listOf(" norte ", "lg", "c3").forEach { query ->
            assertEquals(listOf(tv), filterDevices(listOf(tv, projector), DeviceFilter(query = query)))
        }
    }
    @Test fun filtersIntersectRatherThanUnion() {
        assertTrue(filterDevices(listOf(tv, projector), DeviceFilter(type = DeviceType.TV, connection = ConnectionType.INFRARED)).isEmpty())
        assertEquals(listOf(projector), filterDevices(listOf(tv, projector), DeviceFilter(type = DeviceType.PROJECTOR, connection = ConnectionType.INFRARED)))
    }
    @Test fun favoriteAndNameSortUseRealFields() {
        val favorite = projector.copy(name = "Zeta")
        assertEquals(listOf(favorite, tv), filterDevices(listOf(tv, favorite), DeviceFilter()))
        assertEquals(listOf(tv, favorite), filterDevices(listOf(tv, favorite), DeviceFilter(sort = DeviceSort.NAME)))
    }
    @Test fun emptyInputNeverCreatesSampleDevices() {
        assertTrue(filterDevices(emptyList(), DeviceFilter()).isEmpty())
    }
}
