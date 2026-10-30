package com.universalremote.domain

import com.universalremote.core.model.*

data class BrandCatalogEntry(
    val brand: DeviceBrand,
    val deviceType: DeviceType,
    val recommendedConnection: ConnectionType,
    val defaultProtocol: ProtocolType,
    val defaultIrProfileId: String?,
    val compatibilityStatus: CompatibilityStatus,
    val notes: String
)

object DeviceCatalog {

    val entries: List<BrandCatalogEntry> = listOf(
        // TVs
        BrandCatalogEntry(DeviceBrand.LG, DeviceType.TV, ConnectionType.WIFI, ProtocolType.LG_WEBOS, "lg-akb72915206-v1", CompatibilityStatus.VERIFIED_PHYSICAL, "LG Smart TV webOS y control infrarrojo AKB"),
        BrandCatalogEntry(DeviceBrand.SAMSUNG, DeviceType.TV, ConnectionType.WIFI, ProtocolType.SAMSUNG_WS, "samsung-aa59-v1", CompatibilityStatus.VERIFIED_PHYSICAL, "Samsung Smart TV 2016+ WebSocket e IR Samsung 32-bit"),
        BrandCatalogEntry(DeviceBrand.SONY, DeviceType.TV, ConnectionType.INFRARED, ProtocolType.UNKNOWN, "sony-bravia-v1", CompatibilityStatus.VERIFIED_PHYSICAL, "Sony Bravia SIRC 12-bit IR"),
        BrandCatalogEntry(DeviceBrand.TCL, DeviceType.TV, ConnectionType.INFRARED, ProtocolType.ANDROID_TV, "tcl-rc802n-v1", CompatibilityStatus.VERIFIED_PHYSICAL, "TCL Serie RC802N NEC IR y Android TV"),
        BrandCatalogEntry(DeviceBrand.HISENSE, DeviceType.TV, ConnectionType.INFRARED, ProtocolType.UNKNOWN, "hisense-en2b27-v1", CompatibilityStatus.VERIFIED_PHYSICAL, "Hisense Serie EN2B27 NEC IR"),
        BrandCatalogEntry(DeviceBrand.XIAOMI, DeviceType.TV, ConnectionType.INFRARED, ProtocolType.UNKNOWN, "xiaomi-mitv-v1", CompatibilityStatus.VERIFIED_PHYSICAL, "Xiaomi Mi TV NEC IR"),
        BrandCatalogEntry(DeviceBrand.PHILIPS, DeviceType.TV, ConnectionType.INFRARED, ProtocolType.UNKNOWN, "philips-rc5-v1", CompatibilityStatus.EXPERIMENTAL, "Philips TV RC-5 / RC-6 Biphase Manchester"),
        BrandCatalogEntry(DeviceBrand.PANASONIC, DeviceType.TV, ConnectionType.INFRARED, ProtocolType.UNKNOWN, "panasonic-kaseikyo-v1", CompatibilityStatus.EXPERIMENTAL, "Panasonic Viera Kaseikyo 48-bit IR"),

        // Projectors
        BrandCatalogEntry(DeviceBrand.EPSON, DeviceType.PROJECTOR, ConnectionType.WIFI, ProtocolType.PJLINK, "epson-emp822-v1", CompatibilityStatus.VERIFIED_PHYSICAL, "Proyectores Epson PJLink TCP 4352 e IR ESC/VP21"),
        BrandCatalogEntry(DeviceBrand.BENQ, DeviceType.PROJECTOR, ConnectionType.WIFI, ProtocolType.PJLINK, "benq-projector-v1", CompatibilityStatus.VERIFIED_PHYSICAL, "Proyectores BenQ PJLink TCP y NEC IR"),
        BrandCatalogEntry(DeviceBrand.VIEWSONIC, DeviceType.PROJECTOR, ConnectionType.WIFI, ProtocolType.PJLINK, "viewsonic-pjd-v1", CompatibilityStatus.VERIFIED_PHYSICAL, "Proyectores ViewSonic PJD PJLink TCP y NEC IR"),
        BrandCatalogEntry(DeviceBrand.OPTOMA, DeviceType.PROJECTOR, ConnectionType.WIFI, ProtocolType.PJLINK, "optoma-universal-v1", CompatibilityStatus.VERIFIED_PHYSICAL, "Proyectores Optoma PJLink TCP y NEC IR"),
        BrandCatalogEntry(DeviceBrand.NEC, DeviceType.PROJECTOR, ConnectionType.WIFI, ProtocolType.PJLINK, "nec-np-v1", CompatibilityStatus.VERIFIED_PHYSICAL, "Proyectores NEC NP Series PJLink TCP y NEC IR"),
        BrandCatalogEntry(DeviceBrand.SONY, DeviceType.PROJECTOR, ConnectionType.WIFI, ProtocolType.PJLINK, null, CompatibilityStatus.EXPERIMENTAL, "Proyectores Sony PJLink TCP y SIRC IR"),
        BrandCatalogEntry(DeviceBrand.PANASONIC, DeviceType.PROJECTOR, ConnectionType.WIFI, ProtocolType.PJLINK, null, CompatibilityStatus.EXPERIMENTAL, "Proyectores Panasonic PJLink TCP y Kaseikyo IR")
    )

    fun findEntry(brand: DeviceBrand, type: DeviceType): BrandCatalogEntry? =
        entries.firstOrNull { it.brand == brand && it.deviceType == type }
}
