package com.universalremote.domain

import com.universalremote.core.model.*

interface InfraredTransmitter : IrAvailabilityProvider {
    suspend fun supportedFrequencies(): IrResult<List<CarrierFrequencyRange>>
    suspend fun transmit(carrierHz: Int, patternMicros: List<Int>): IrResult<Unit>
}
interface IrProfileCatalog {
    val profiles: List<IrProfile>
    val pendingBrands: List<PendingBrandInfo> get() = emptyList()
    fun find(id: String): IrProfile? = profiles.firstOrNull { it.id == id }
    fun profilesFor(brand: DeviceBrand, type: DeviceType): List<IrProfile> =
        profiles.filter { it.brand == brand && it.deviceType == type }
    fun pendingFor(brand: DeviceBrand, type: DeviceType): PendingBrandInfo? =
        pendingBrands.firstOrNull { it.brand == brand && it.deviceType == type }
}
interface InfraredRepository {
    val profiles: List<IrProfile>
    val pendingBrands: List<PendingBrandInfo> get() = emptyList()
    suspend fun test(profileId: String): IrResult<IrAssessment>
    suspend fun assessment(id: String): IrAssessment?
    suspend fun confirm(id: String, responded: Boolean): IrResult<IrAssessment>
    suspend fun save(id: String, name: String, room: String?): IrResult<Device>
    suspend fun send(deviceId: String, command: RemoteCommand, confirmed: Boolean = false): IrResult<Unit>
}
class SendRemoteCommand(private val repository: InfraredRepository) {
    suspend operator fun invoke(deviceId: String, command: RemoteCommand, confirmed: Boolean = false) =
        repository.send(deviceId, command, confirmed)
}

/** App limits are stricter than Android's two-second ceiling. Long sums prevent overflow. */
object IrSignalValidator {
    const val MAX_ENTRIES = 1024
    const val MAX_DURATION_MICROS = 1_000_000L
    fun validate(frequencyHz: Int, pattern: List<Int>): IrError? = when {
        frequencyHz !in 20_000..60_000 -> IrError.INVALID_FREQUENCY
        pattern.size !in 3..MAX_ENTRIES -> IrError.INVALID_PATTERN
        pattern.any { it !in 1..100_000 } -> IrError.INVALID_PATTERN
        pattern.sumOf { it.toLong() } > MAX_DURATION_MICROS -> IrError.INVALID_PATTERN
        else -> null
    }
}
