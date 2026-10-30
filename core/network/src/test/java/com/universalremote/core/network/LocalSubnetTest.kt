package com.universalremote.core.network

import org.junit.Assert.*
import org.junit.Test

class LocalSubnetTest {
    @Test fun usesRealPrefixAndExcludesSelfNetworkBroadcast() {
        val hosts = LocalSubnet.hosts("192.168.4.130", 25)
        assertEquals(125, hosts.size)
        assertEquals("192.168.4.129", hosts.first())
        assertEquals("192.168.4.254", hosts.last())
        assertFalse(hosts.contains("192.168.4.130"))
        assertFalse(hosts.contains("192.168.4.128"))
    }
    @Test fun doesNotAssume24BitNetwork() {
        val hosts = LocalSubnet.hosts("10.0.2.4", 23)
        assertEquals(509, hosts.size)
        assertTrue(hosts.contains("10.0.3.254"))
        assertFalse(hosts.contains("10.0.4.1"))
    }
    @Test(expected = IllegalArgumentException::class) fun rejectsUnboundedScan() { LocalSubnet.hosts("10.0.0.1", 8) }
}
