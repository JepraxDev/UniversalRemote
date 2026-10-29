package com.universalremote.core.infrared

import com.universalremote.core.model.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class AndroidInfraredControllerTest {
    @Test fun detectsEmitter() {
        assertEquals(IrAvailability.Available, AndroidInfraredController(hasEmitter = { true }).availability())
    }
    @Test fun noEmitterDoesNotThrow() {
        assertEquals(IrAvailability.NoEmitter, AndroidInfraredController(hasEmitter = { false }).availability())
    }
    @Test fun missingServiceDoesNotThrow() {
        assertEquals(IrAvailability.ServiceUnavailable, AndroidInfraredController(hasEmitter = { null }).availability())
    }
    @Test fun platformFailureIsExplicit() {
        assertEquals(IrAvailability.PlatformError, AndroidInfraredController(hasEmitter = { throw SecurityException() }).availability())
    }
    @Test fun transmissionSucceedsWithValidSignal() = runTest {
        var transmitted = false
        val controller = AndroidInfraredController(
            hasEmitter = { true },
            frequencies = { listOf(CarrierFrequencyRange(30_000, 60_000)) },
            transmit = { freq, pattern ->
                assertEquals(38_000, freq)
                assertEquals(3, pattern.size)
                transmitted = true
            }
        )
        val result = controller.transmit(38_000, listOf(9000, 4500, 560))
        assertEquals(IrResult.Success(Unit), result)
        assertTrue(transmitted)
    }
    @Test fun missingEmitterReturnsCompatibilityError() = runTest {
        assertEquals(
            IrResult.Unavailable(IrAvailability.NoEmitter),
            AndroidInfraredController(hasEmitter = { false }).transmit(38_000, listOf(9000, 4500, 560))
        )
    }
    @Test fun invalidFrequencyReturnsFailure() = runTest {
        val controller = AndroidInfraredController(hasEmitter = { true })
        assertEquals(
            IrResult.Failure(IrError.INVALID_FREQUENCY),
            controller.transmit(10_000, listOf(9000, 4500, 560))
        )
    }
    @Test fun unsupportedFrequencyReturnsFailure() = runTest {
        val controller = AndroidInfraredController(
            hasEmitter = { true },
            frequencies = { listOf(CarrierFrequencyRange(36_000, 40_000)) }
        )
        assertEquals(
            IrResult.Failure(IrError.UNSUPPORTED_FREQUENCY),
            controller.transmit(56_000, listOf(9000, 4500, 560))
        )
    }
    @Test fun unknownFrequencyReturnsFailure() = runTest {
        val controller = AndroidInfraredController(
            hasEmitter = { true },
            frequencies = { emptyList() }
        )
        assertEquals(
            IrResult.Failure(IrError.FREQUENCY_UNKNOWN),
            controller.transmit(38_000, listOf(9000, 4500, 560))
        )
    }
    @Test fun invalidPatternReturnsFailure() = runTest {
        val controller = AndroidInfraredController(hasEmitter = { true })
        assertEquals(
            IrResult.Failure(IrError.INVALID_PATTERN),
            controller.transmit(38_000, listOf(100))
        )
    }
    @Test fun platformExceptionReturnsFailure() = runTest {
        val controller = AndroidInfraredController(
            hasEmitter = { true },
            frequencies = { listOf(CarrierFrequencyRange(30_000, 60_000)) },
            transmit = { _, _ -> throw RuntimeException("Hardware failure") }
        )
        assertEquals(
            IrResult.Failure(IrError.PLATFORM_FAILURE),
            controller.transmit(38_000, listOf(9000, 4500, 560))
        )
    }
    @Test fun rapidTransmissionsAreRejectedWithBusy() = runTest {
        var time = 1_000_000_000L
        val controller = AndroidInfraredController(
            hasEmitter = { true },
            frequencies = { listOf(CarrierFrequencyRange(30_000, 60_000)) },
            nanoTime = { time }
        )
        assertEquals(IrResult.Success(Unit), controller.transmit(38_000, listOf(9000, 4500, 560)))
        assertEquals(IrResult.Failure(IrError.BUSY), controller.transmit(38_000, listOf(9000, 4500, 560)))
        time += 300_000_000L
        assertEquals(IrResult.Success(Unit), controller.transmit(38_000, listOf(9000, 4500, 560)))
    }
}
