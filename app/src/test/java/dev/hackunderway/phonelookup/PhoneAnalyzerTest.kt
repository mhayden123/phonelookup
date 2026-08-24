package dev.hackunderway.phonelookup

import dev.hackunderway.phonelookup.data.PhoneAnalyzer
import dev.hackunderway.phonelookup.data.searchVariants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PhoneAnalyzerTest {

    @Test
    fun `parses a national-format number using the selected region`() {
        val info = PhoneAnalyzer.analyze("044 668 1800", "CH")

        assertTrue("expected a valid Swiss number", info.valid)
        assertEquals("+41446681800", info.e164)
        assertEquals("41", info.countryCode.toString())
        assertEquals("CH", info.regionCode)
        assertTrue("expected at least one time zone", info.timezones.isNotEmpty())
    }

    @Test
    fun `an international-format number ignores the selected region`() {
        val fromCh = PhoneAnalyzer.analyze("+41 44 668 1800", "CH")
        val fromUs = PhoneAnalyzer.analyze("+41 44 668 1800", "US")

        assertEquals(fromCh.e164, fromUs.e164)
        assertTrue(fromUs.valid)
    }

    @Test
    fun `identifies line type and location for a known landline`() {
        val info = PhoneAnalyzer.analyze("+1 650 253 0000", "US")

        assertTrue(info.valid)
        assertEquals("+16502530000", info.e164)
        assertNotNull("expected a geocoded location", info.location)
        assertEquals("Fixed line", info.lineType)
    }

    @Test
    fun `reports a readable error instead of throwing on junk input`() {
        val info = PhoneAnalyzer.analyze("not a phone number", "US")

        assertFalse(info.valid)
        assertNotNull("expected an explanation", info.error)
    }

    @Test
    fun `a wrong-length number is parsed but not valid`() {
        val info = PhoneAnalyzer.analyze("+1 650 253", "US")

        assertFalse(info.valid)
    }

    @Test
    fun `search variants cover the formats a number appears in online`() {
        val info = PhoneAnalyzer.analyze("+41 44 668 1800", "CH")
        val variants = info.searchVariants()

        assertTrue("expected E.164 form", variants.contains("+41446681800"))
        assertTrue("expected digits-only form", variants.contains("41446681800"))
        assertTrue("expected the international form", variants.contains(info.international.orEmpty()))
        assertEquals("variants should be de-duplicated", variants.size, variants.distinct().size)
    }

    @Test
    fun `supported regions are populated and sorted`() {
        val regions = PhoneAnalyzer.supportedRegions()

        assertTrue("expected many regions", regions.size > 200)
        assertTrue(regions.any { it.code == "US" })
        assertEquals(regions.map { it.name }.sorted(), regions.map { it.name })
    }
}
