package dev.haseeb.handi.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QuantityFormatterTest {

    @Test fun `formats fractions as glyphs`() {
        assertEquals("½", QuantityFormatter.amount(0.5))
        assertEquals("1½", QuantityFormatter.amount(1.5))
        assertEquals("2", QuantityFormatter.amount(2.0))
        assertEquals("1.2", QuantityFormatter.amount(1.2))
    }

    @Test fun `parses common kitchen input`() {
        assertEquals(0.5, QuantityFormatter.parse("1/2")!!, 0.001)
        assertEquals(1.5, QuantityFormatter.parse("1 1/2")!!, 0.001)
        assertEquals(1.5, QuantityFormatter.parse("1½")!!, 0.001)
        assertEquals(2.5, QuantityFormatter.parse("2,5")!!, 0.001)
        assertNull(QuantityFormatter.parse("abc"))
        assertNull(QuantityFormatter.parse("0"))
    }

    @Test fun `pluralises and scales`() {
        assertEquals("2 pcs", QuantityFormatter.format(1.0, Measure.PIECE, scale = 2.0))
        assertEquals("250 g", QuantityFormatter.format(500.0, Measure.GRAM, scale = 0.5))
        assertEquals("to taste", QuantityFormatter.format(null, Measure.TO_TASTE))
        assertEquals("2 bunches", QuantityFormatter.format(2.0, Measure.BUNCH))
    }
}
