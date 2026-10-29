package com.universalremote.domain

import com.universalremote.core.model.*
import org.junit.Assert.*
import org.junit.Test

class GetConnectionMethodsTest {
    @Test fun infraredRequiresAnAvailableEmitter() {
        assertEquals(setOf(ConnectionType.WIFI, ConnectionType.INFRARED), GetConnectionMethods()(IrAvailability.Available))
    }
    @Test fun wifiRemainsSelectableForAllInfraredFailures() {
        listOf(IrAvailability.NoEmitter, IrAvailability.ServiceUnavailable, IrAvailability.PlatformError).forEach {
            assertEquals(setOf(ConnectionType.WIFI), GetConnectionMethods()(it))
        }
    }
}
