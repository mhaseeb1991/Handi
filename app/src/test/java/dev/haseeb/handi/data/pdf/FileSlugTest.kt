package dev.haseeb.handi.data.pdf

import org.junit.Assert.assertEquals
import org.junit.Test

class FileSlugTest {

    @Test fun `replaces non-alphanumeric characters with dashes`() {
        assertEquals("Chana-Masala", "Chana Masala".toFileSlug())
        assertEquals("Mom-s-Biryani", "Mom's Biryani!".toFileSlug())
    }

    @Test fun `trims leading and trailing dashes left by punctuation`() {
        assertEquals("Aloo-Gobi", "  Aloo Gobi!!".toFileSlug())
    }

    @Test fun `falls back to a generic name when nothing alphanumeric remains`() {
        assertEquals("recipe", "   ".toFileSlug())
        assertEquals("recipe", "!!!".toFileSlug())
    }

    @Test fun `caps length so a very long title doesn't break the filesystem`() {
        val long = "a".repeat(200)
        assertEquals(60, long.toFileSlug().length)
    }
}
