package com.universalremote.core.infrared

import android.content.Context
import android.hardware.ConsumerIrManager
import android.util.Log
import com.universalremote.core.model.*
import com.universalremote.domain.InfraredTransmitter
import com.universalremote.domain.IrSignalValidator
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex

// Preserve the original contract name for callers of stage 1.
typealias InfraredController = InfraredTransmitter

interface IrHardware {
    fun hasEmitter(): Boolean?
    fun frequencies(): List<CarrierFrequencyRange>?
    fun transmit(frequencyHz: Int, pattern: IntArray)
}
private class AndroidIrHardware(private val context: Context) : IrHardware {
    private fun manager() = context.getSystemService(ConsumerIrManager::class.java)
    override fun hasEmitter() = manager()?.hasIrEmitter()
    override fun frequencies() = manager()?.carrierFrequencies?.map { CarrierFrequencyRange(it.minFrequency, it.maxFrequency) }
    override fun transmit(frequencyHz: Int, pattern: IntArray) {
        checkNotNull(manager()).transmit(frequencyHz, pattern)
    }
}

class AndroidInfraredController(
    private val hardware: IrHardware,
    private val io: CoroutineDispatcher = Dispatchers.IO,
    private val nanoTime: () -> Long = System::nanoTime,
    private val logError: (RuntimeException) -> Unit = {},
) : InfraredTransmitter {
    constructor(context: Context) : this(AndroidIrHardware(context), logError = {
        // Never log device names, addresses, patterns or platform exception messages.
        Log.w("UniversalRemoteIR", "IR platform operation failed: ${it.javaClass.simpleName}")
    })

    constructor(
        hasEmitter: () -> Boolean?,
        frequencies: () -> List<CarrierFrequencyRange>? = { listOf(CarrierFrequencyRange(30_000, 60_000)) },
        transmit: (Int, IntArray) -> Unit = { _, _ -> },
        io: CoroutineDispatcher = Dispatchers.Unconfined,
        nanoTime: () -> Long = System::nanoTime,
        logError: (RuntimeException) -> Unit = {},
    ) : this(
        object : IrHardware {
            override fun hasEmitter() = hasEmitter()
            override fun frequencies() = frequencies()
            override fun transmit(frequencyHz: Int, pattern: IntArray) = transmit(frequencyHz, pattern)
        },
        io = io,
        nanoTime = nanoTime,
        logError = logError
    )
    private val gate = Mutex()
    private var lastFinished: Long? = null

    override fun availability(): IrAvailability = try {
        when (hardware.hasEmitter()) {
            true -> IrAvailability.Available
            false -> IrAvailability.NoEmitter
            null -> IrAvailability.ServiceUnavailable
        }
    } catch (error: RuntimeException) {
        logError(error)
        IrAvailability.PlatformError
    }

    override suspend fun supportedFrequencies(): IrResult<List<CarrierFrequencyRange>> = withContext(io) {
        val available = availability()
        if (available != IrAvailability.Available) return@withContext IrResult.Unavailable(available)
        platformCall {
            val ranges = hardware.frequencies()
            if (ranges.isNullOrEmpty()) IrResult.Failure(IrError.FREQUENCY_UNKNOWN)
            else if (ranges.any { it.minHz <= 0 || it.maxHz < it.minHz }) IrResult.Failure(IrError.PLATFORM_FAILURE)
            else IrResult.Success(ranges.toList())
        }
    }

    override suspend fun transmit(carrierHz: Int, patternMicros: List<Int>): IrResult<Unit> {
        // Bound size before copying untrusted patterns; freeze data before dispatch.
        if (patternMicros.size !in 3..IrSignalValidator.MAX_ENTRIES) return IrResult.Failure(IrError.INVALID_PATTERN)
        val pattern = patternMicros.toList()
        IrSignalValidator.validate(carrierHz, pattern)?.let { return IrResult.Failure(it) }
        val available = availability()
        if (available != IrAvailability.Available) return IrResult.Unavailable(available)
        if (!gate.tryLock()) return IrResult.Failure(IrError.BUSY)
        try {
            return withContext(io) {
                ensureActive()
                if (lastFinished?.let { nanoTime() - it < 250_000_000L } == true) return@withContext IrResult.Failure(IrError.BUSY)
                when (val ranges = supportedFrequencies()) {
                    is IrResult.Unavailable -> return@withContext ranges
                    is IrResult.Failure -> return@withContext ranges
                    is IrResult.Success -> if (ranges.value.none { carrierHz in it.minHz..it.maxHz })
                        return@withContext IrResult.Failure(IrError.UNSUPPORTED_FREQUENCY)
                }
                ensureActive()
                try {
                    platformCall {
                        hardware.transmit(carrierHz, pattern.toIntArray())
                        IrResult.Success(Unit) // local completion only, NOT receiver acknowledgement.
                    }
                } finally { lastFinished = nanoTime() }
            }
        } finally {
            // ConsumerIrManager is synchronous/non-cancellable. Keep the lock until it returns.
            // Cancellation before dispatch prevents sending; no queued commands or auto-retry.
            gate.unlock()
        }
    }

    private inline fun <T> platformCall(block: () -> IrResult<T>): IrResult<T> = try { block() }
    catch (cancelled: CancellationException) { throw cancelled }
    catch (error: SecurityException) { logError(error); IrResult.Failure(IrError.PERMISSION_DENIED) }
    catch (error: RuntimeException) { logError(error); IrResult.Failure(IrError.PLATFORM_FAILURE) }
}
