package com.universalremote.domain

import com.universalremote.core.model.*
import com.universalremote.protocol.api.ProtocolRegistry

class DeviceCompatibilityService(
    private val resolver: DeviceCapabilityResolver
) {

    fun getSupportedBrands(type: DeviceType): List<DeviceBrand> {
        return DeviceCatalog.entries
            .filter { it.deviceType == type }
            .map { it.brand }
            .distinct()
    }

    fun getCompatibilityStatus(brand: DeviceBrand, type: DeviceType): CompatibilityStatus {
        val entry = DeviceCatalog.findEntry(brand, type)
        return entry?.compatibilityStatus ?: CompatibilityStatus.EXPERIMENTAL
    }

    fun getCatalogEntry(brand: DeviceBrand, type: DeviceType): BrandCatalogEntry? {
        return DeviceCatalog.findEntry(brand, type)
    }

    fun resolveCapabilities(device: Device): DeviceCapabilities {
        return resolver.resolve(device)
    }
}
