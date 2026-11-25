package dev.haseeb.handi.data.share

import org.junit.Assert.assertEquals
import org.junit.Test

class ShareLinkTest {

    @Test fun `builds a handi recipe deep link from a share id`() {
        assertEquals("handi://recipe/abc-123", shareLinkUri("abc-123"))
    }
}
