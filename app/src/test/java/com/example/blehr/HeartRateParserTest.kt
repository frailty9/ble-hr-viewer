package com.example.blehr

import com.example.blehr.ble.DiagnosticCode
import com.example.blehr.ble.HeartRateParseException
import com.example.blehr.ble.HeartRateParser
import com.example.blehr.ble.SensorContact
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class HeartRateParserTest {

    @Test
    fun emptyPacketIsRejected() {
        assertThrows(HeartRateParseException::class.java) {
            HeartRateParser.parse(byteArrayOf())
        }
    }

    @Test
    fun flagsOnlyIsTruncated() {
        val error = assertThrows(HeartRateParseException::class.java) {
            HeartRateParser.parse(byteArrayOf(0x00))
        }
        assertEquals(DiagnosticCode.TRUNCATED_HEART_RATE, error.reason.code)
        assertEquals(listOf(2, 1), error.reason.arguments)
    }

    @Test
    fun uint8ValueIsParsed() {
        val sample = HeartRateParser.parse(byteArrayOf(0x00, 72))
        assertEquals(72, sample.bpm)
        assertEquals(SensorContact.NOT_SUPPORTED, sample.sensorContact)
        assertNull(sample.energyExpendedKj)
        assertTrue(sample.rrIntervalsRaw.isEmpty())
        assertTrue(sample.warnings.isEmpty())
    }

    @Test
    fun uint16ValueIsLittleEndian() {
        val sample = HeartRateParser.parse(byteArrayOf(0x01, 0x2C, 0x01))
        assertEquals(300, sample.bpm)
        assertTrue(sample.warnings.any { it.code == DiagnosticCode.IMPLAUSIBLE_HEART_RATE && it.arguments == listOf(300) })
    }

    @Test
    fun contactNotDetected() {
        val sample = HeartRateParser.parse(byteArrayOf(0x04, 80))
        assertEquals(SensorContact.NOT_DETECTED, sample.sensorContact)
    }

    @Test
    fun contactDetected() {
        val sample = HeartRateParser.parse(byteArrayOf(0x06, 80))
        assertEquals(SensorContact.DETECTED, sample.sensorContact)
    }

    @Test
    fun reservedContactCombinationIsWarned() {
        val sample = HeartRateParser.parse(byteArrayOf(0x02, 80))
        assertEquals(SensorContact.NOT_SUPPORTED, sample.sensorContact)
        assertTrue(sample.warnings.any { it.code == DiagnosticCode.RESERVED_CONTACT })
    }

    @Test
    fun energyExpendedIsParsed() {
        val sample = HeartRateParser.parse(byteArrayOf(0x08, 80, 0x34, 0x12))
        assertEquals(0x1234, sample.energyExpendedKj)
    }

    @Test
    fun truncatedEnergyIsRejected() {
        assertThrows(HeartRateParseException::class.java) {
            HeartRateParser.parse(byteArrayOf(0x08, 80, 0x01))
        }
    }

    @Test
    fun rrIntervalsAreConvertedToMilliseconds() {
        val sample = HeartRateParser.parse(
            byteArrayOf(0x10, 60, 0x00, 0x04, 0x00, 0x08)
        )
        assertEquals(listOf(1024, 2048), sample.rrIntervalsRaw)
        assertEquals(listOf(1000.0, 2000.0), sample.rrIntervalsMs)
    }

    @Test
    fun oddRrByteIsWarnedButSampleKept() {
        val sample = HeartRateParser.parse(byteArrayOf(0x10, 60, 0x01))
        assertEquals(60, sample.bpm)
        assertTrue(sample.rrIntervalsRaw.isEmpty())
        assertTrue(sample.warnings.any { it.code == DiagnosticCode.MISSING_RR })
        assertTrue(sample.warnings.any { it.code == DiagnosticCode.ODD_RR_BYTE })
    }

    @Test
    fun unknownTrailingBytesAreWarned() {
        val sample = HeartRateParser.parse(
            byteArrayOf(0x00, 60, 0xAA.toByte(), 0xBB.toByte())
        )
        assertEquals(60, sample.bpm)
        assertTrue(sample.warnings.any { it.code == DiagnosticCode.TRAILING_BYTES && it.arguments == listOf(2) })
    }

    @Test
    fun reservedFlagBitsAreWarned() {
        val sample = HeartRateParser.parse(byteArrayOf(0x80.toByte(), 60))
        assertTrue(sample.warnings.any { it.code == DiagnosticCode.RESERVED_FLAGS && it.arguments == listOf(0x80) })
    }

    @Test
    fun zeroBpmIsAcceptedWithWarning() {
        val sample = HeartRateParser.parse(byteArrayOf(0x06, 0x00))
        assertEquals(0, sample.bpm)
        assertTrue(sample.warnings.any { it.code == DiagnosticCode.ZERO_HEART_RATE })
    }

    @Test
    fun hexFormatting() {
        assertEquals("06 4d", HeartRateParser.hex(byteArrayOf(0x06, 0x4D)))
        assertEquals("06 4d", HeartRateParser.parse(byteArrayOf(0x06, 77)).rawHex)
    }
}
