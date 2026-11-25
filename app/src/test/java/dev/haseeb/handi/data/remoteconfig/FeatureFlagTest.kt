package dev.haseeb.handi.data.remoteconfig

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FeatureFlagTest {

    @Test fun `every flag has a non-blank, unique key`() {
        val keys = FeatureFlag.entries.map { it.key }
        assertTrue(keys.all { it.isNotBlank() })
        assertEquals("expected no duplicate Remote Config keys", keys.size, keys.toSet().size)
    }

    @Test fun `defaults() produces the exact map FirebaseRemoteConfig setDefaultsAsync expects`() {
        val defaults = FeatureFlag.defaults()
        assertEquals(FeatureFlag.entries.size, defaults.size)
        FeatureFlag.entries.forEach { flag -> assertEquals(flag.defaultValue, defaults[flag.key]) }
    }

    @Test fun `new features ship enabled by default unless deliberately staged off`() {
        // Documents intent: PDF export and Backup & Restore are existing, already-shipped
        // features being retrofitted with a kill switch, not new work being staged in.
        assertEquals(true, FeatureFlag.PDF_EXPORT_ENABLED.defaultValue)
        assertEquals(true, FeatureFlag.BACKUP_RESTORE_ENABLED.defaultValue)
    }
}
