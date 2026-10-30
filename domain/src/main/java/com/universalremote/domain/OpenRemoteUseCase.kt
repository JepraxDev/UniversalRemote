package com.universalremote.domain

import com.universalremote.core.model.Device

class OpenRemoteUseCase(
    private val deviceRepository: DeviceRepository
) {
    suspend operator fun invoke(deviceId: String): Device? {
        val device = deviceRepository.find(deviceId) ?: return null
        val updated = device.copy(lastUsedAt = System.currentTimeMillis())
        deviceRepository.update(updated)
        return updated
    }
}
