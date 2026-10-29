package com.universalremote.domain

import com.universalremote.core.model.IrError
import org.junit.Assert.*
import org.junit.Test

class IrSignalValidatorTest {
    @Test
    fun validPatternReturnsNull() {
        val error = IrSignalValidator.validate(38_000, listOf(9000, 4500, 560, 560))
        assertNull(error)
    }

    @Test
    fun frequencyBelowLimitReturnsInvalidFrequency() {
        assertEquals(IrError.INVALID_FREQUENCY, IrSignalValidator.validate(19_999, listOf(9000, 4500, 560)))
    }

    @Test
    fun frequencyAboveLimitReturnsInvalidFrequency() {
        assertEquals(IrError.INVALID_FREQUENCY, IrSignalValidator.validate(60_001, listOf(9000, 4500, 560)))
    }

    @Test
    fun patternTooShortReturnsInvalidPattern() {
        assertEquals(IrError.INVALID_PATTERN, IrSignalValidator.validate(38_000, listOf(9000, 4500)))
    }

    @Test
    fun patternTooLongReturnsInvalidPattern() {
        val longPattern = List(1025) { 500 }
        assertEquals(IrError.INVALID_PATTERN, IrSignalValidator.validate(38_000, longPattern))
    }

    @Test
    fun zeroOrNegativeEntryReturnsInvalidPattern() {
        assertEquals(IrError.INVALID_PATTERN, IrSignalValidator.validate(38_000, listOf(9000, 0, 560)))
        assertEquals(IrError.INVALID_PATTERN, IrSignalValidator.validate(38_000, listOf(9000, -100, 560)))
    }

    @Test
    fun pulseExceedingEntryLimitReturnsInvalidPattern() {
        assertEquals(IrError.INVALID_PATTERN, IrSignalValidator.validate(38_000, listOf(9000, 100_001, 560)))
    }

    @Test
    fun totalDurationExceedingOneSecondReturnsInvalidPattern() {
        val pattern = listOf(500_000, 400_000, 100_001) // Sum > 1_000_000
        assertEquals(IrError.INVALID_PATTERN, IrSignalValidator.validate(38_000, pattern))
    }
}
