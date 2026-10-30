package com.universalremote.domain

import com.universalremote.core.model.Device
import com.universalremote.core.model.DeviceType
import java.util.Locale

object DeviceDisplayNameResolver {

    private val hexUuidRegex = Regex("""-[0-9a-f]{8,}""", RegexOption.IGNORE_CASE)

    fun resolveDisplayName(device: Device): String {
        val rawName = device.name.trim()
        if (rawName.isBlank()) {
            return defaultGenericName(device)
        }

        if (hexUuidRegex.containsMatchIn(rawName) || rawName.length > 32) {
            val upper = rawName.uppercase(Locale.ROOT)
            return when {
                upper.contains("MITV") || upper.contains("TV STICK") || upper.contains("XIAOMI") -> "Android TV (TV Stick Mi)"
                upper.contains("WEBOS") || upper.contains("LG") -> "LG Smart TV"
                upper.contains("SAMSUNG") -> "Samsung Smart TV"
                upper.contains("SONY") || upper.contains("BRAVIA") -> "Sony Bravia TV"
                upper.contains("EPSON") -> "Proyector Epson"
                upper.contains("BENQ") -> "Proyector BenQ"
                upper.contains("VIEWSONIC") -> "Proyector ViewSonic"
                upper.contains("OPTOMA") -> "Proyector Optoma"
                upper.contains("NEC") -> "Proyector NEC"
                device.deviceType == DeviceType.PROJECTOR -> "Proyector ${device.brand.id}"
                else -> "Android TV"
            }
        }

        return rawName
    }

    fun resolveSubtitle(device: Device): String {
        val brand = device.brand.id
        val model = device.model.takeIf { it.isNotBlank() && !it.equals("Network Device", ignoreCase = true) } ?: ""
        return if (model.isNotBlank()) "$brand · $model" else brand
    }

    private fun defaultGenericName(device: Device): String {
        return if (device.deviceType == DeviceType.TV) {
            "${device.brand.id} Smart TV"
        } else {
            "Proyector ${device.brand.id}"
        }
    }
}
