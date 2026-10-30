package com.universalremote.core.model

data class CarrierFrequencyRange(val minHz: Int, val maxHz: Int)
data class IrSignal(val carrierHz: Int, val patternMicros: List<Int>)
enum class IrError {
    INVALID_FREQUENCY, INVALID_PATTERN, FREQUENCY_UNKNOWN, UNSUPPORTED_FREQUENCY,
    PLATFORM_FAILURE, PERMISSION_DENIED, BUSY, PROFILE_MISSING, PROFILE_INCOMPATIBLE,
    COMMAND_UNSUPPORTED, NOT_CONFIGURED, CONFIRMATION_REQUIRED, STORAGE_FAILURE,
}
sealed interface IrResult<out T> {
    data class Success<T>(val value: T) : IrResult<T>
    data class Unavailable(val availability: IrAvailability) : IrResult<Nothing>
    data class Failure(val error: IrError) : IrResult<Nothing>
}
data class IrProfile(
    val id: String,
    val brand: DeviceBrand,
    val deviceType: DeviceType,
    val family: String,
    val protocol: String,
    val carrierHz: Int,
    val commands: Map<RemoteCommand, IrSignal>,
    val source: String,
    val testCommand: RemoteCommand,
    val commandInfo: Map<RemoteCommand, IrCommandInfo> = emptyMap(),
    /** Extra explanation when the requested first test command has no verified capture. */
    val testNote: String? = null,
) {
    val capabilities get() = DeviceCapabilities(commands.keys.toSet())
}

/** Provenance of one command. Physical confirmation is always per projector/user. */
enum class IrCommandVerification {
    OFFICIAL_DOCUMENTED,
    COMMUNITY_CAPTURED,
    CONFIRMED_BY_USER,
}

data class IrCommandInfo(
    val function: String,
    val protocol: String,
    val source: String,
    val verification: IrCommandVerification,
    val note: String? = null,
)
/** This is a user assertion for one target, not manufacturer-wide compatibility. */
enum class IrValidationStatus { UNVERIFIED, CONFIRMED_BY_USER, INCOMPATIBLE }
data class IrAssessment(
    val id: String,
    val profileId: String,
    val status: IrValidationStatus,
    val testedAt: Long,
    val testCommand: RemoteCommand,
    val supportedCommands: Set<RemoteCommand>,
)
data class PendingBrandInfo(
    val brand: DeviceBrand,
    val deviceType: DeviceType,
    val reason: String,
    val missingDetails: String,
)

fun RemoteCommand.requiresConfirmation(): Boolean = this in setOf(
    RemoteCommand.POWER, RemoteCommand.POWER_ON, RemoteCommand.POWER_OFF, RemoteCommand.AV_MUTE,
)
