package dev.haseeb.handi.data.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.SecureRandom

class AuthRepositoryKtTest {

    @Test fun `nonce is URL-safe - no padding, plus, or forward slash`() {
        val nonce = randomNonce(SecureRandom())
        assertTrue(nonce.isNotEmpty())
        assertFalse(nonce.contains('+'))
        assertFalse(nonce.contains('/'))
        assertFalse(nonce.contains('='))
    }

    @Test fun `two nonces in a row are different, not a fixed or predictable value`() {
        val random = SecureRandom()
        assertNotEquals(randomNonce(random), randomNonce(random))
    }

    @Test fun `nonce length is stable (16 random bytes, base64url-encoded)`() {
        // 16 bytes -> 22 base64url characters with no padding.
        assertEquals(22, randomNonce(SecureRandom()).length)
    }
}
