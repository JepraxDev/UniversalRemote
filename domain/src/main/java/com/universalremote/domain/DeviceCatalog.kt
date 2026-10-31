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

/** Model-level entry used by the Epson IR wizard and the network catalogue. */
data class ModelCatalogEntry(
    val brand: DeviceBrand,
    val deviceType: DeviceType,
    val model: String,
    val defaultIrProfileId: String?,
    val connection: ConnectionType,
    val compatibilityStatus: CompatibilityStatus,
    val notes: String,
)

object DeviceCatalog {

    val modelEntries: List<ModelCatalogEntry> = listOf(
        ModelCatalogEntry(
            DeviceBrand.EPSON, DeviceType.PROJECTOR, "PowerLite 118",
            "EPSON_POWERLITE_118_ORIGINAL_IR", ConnectionType.WIFI, CompatibilityStatus.EXPERIMENTAL,
            "PJLink/Web Control documentados y perfil NECext 83 55 del control 219863500.",
        ),
        ModelCatalogEntry(
            DeviceBrand.EPSON, DeviceType.PROJECTOR, "EB-L260F",
            "EPSON_EB_L260F", ConnectionType.WIFI, CompatibilityStatus.EXPERIMENTAL,
            "Perfil IR propio a 38 kHz: 15 capturas raw y capturas de referencia pendientes de prueba física " +
                "(Menu, Esc, Aspect, Color Mode, Computer, HDMI, USB/LAN, User, Auto, Default, Ajustes y 1–9). " +
                "PJLink/Web Control documentados. 0/All, ID, Información y Num sin código verificado.",
        ),
        ModelCatalogEntry(
            DeviceBrand.EPSON, DeviceType.PROJECTOR, "EMP822H",
            "epson-emp822h-v1", ConnectionType.WIFI, CompatibilityStatus.EXPERIMENTAL,
            "Captura NECext comunitaria; no se reutiliza en otros modelos.",
        ),
    )

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
        BrandCatalogEntry(DeviceBrand.EPSON, DeviceType.PROJECTOR, ConnectionType.WIFI, ProtocolType.UNKNOWN, "EPSON_POWERLITE_118_ORIGINAL_IR", CompatibilityStatus.EXPERIMENTAL, "PowerLite 118 y EB-L260F: PJLink/Web Control documentados. Perfiles IR independientes: PowerLite 118 (NECext 83 55) y EB-L260F (capturas raw propias y de referencia); pendientes de confirmación física."),
        BrandCatalogEntry(DeviceBrand.BENQ, DeviceType.PROJECTOR, ConnectionType.WIFI, ProtocolType.PJLINK, "benq-projector-v1", CompatibilityStatus.VERIFIED_PHYSICAL, "Proyectores BenQ PJLink TCP y NEC IR"),
        BrandCatalogEntry(DeviceBrand.VIEWSONIC, DeviceType.PROJECTOR, ConnectionType.WIFI, ProtocolType.PJLINK, "viewsonic-pjd-v1", CompatibilityStatus.VERIFIED_PHYSICAL, "Proyectores ViewSonic PJD PJLink TCP y NEC IR"),
        BrandCatalogEntry(DeviceBrand.OPTOMA, DeviceType.PROJECTOR, ConnectionType.WIFI, ProtocolType.PJLINK, "optoma-universal-v1", CompatibilityStatus.VERIFIED_PHYSICAL, "Proyectores Optoma PJLink TCP y NEC IR"),
        BrandCatalogEntry(DeviceBrand.NEC, DeviceType.PROJECTOR, ConnectionType.WIFI, ProtocolType.PJLINK, "nec-np-v1", CompatibilityStatus.VERIFIED_PHYSICAL, "Proyectores NEC NP Series PJLink TCP y NEC IR"),
        BrandCatalogEntry(DeviceBrand.SONY, DeviceType.PROJECTOR, ConnectionType.WIFI, ProtocolType.PJLINK, null, CompatibilityStatus.EXPERIMENTAL, "Proyectores Sony PJLink TCP y SIRC IR"),
        BrandCatalogEntry(DeviceBrand.PANASONIC, DeviceType.PROJECTOR, ConnectionType.WIFI, ProtocolType.PJLINK, null, CompatibilityStatus.EXPERIMENTAL, "Proyectores Panasonic PJLink TCP y Kaseikyo IR")
    )

    fun findEntry(brand: DeviceBrand, type: DeviceType): BrandCatalogEntry? =
        entries.firstOrNull { it.brand == brand && it.deviceType == type }

    fun findModel(brand: DeviceBrand, type: DeviceType, model: String): ModelCatalogEntry? =
        modelEntries.firstOrNull {
            it.brand == brand && it.deviceType == type && it.model.equals(model, ignoreCase = true)
        }
}
