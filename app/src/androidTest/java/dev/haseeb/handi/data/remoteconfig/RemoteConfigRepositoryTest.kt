package dev.haseeb.handi.data.remoteconfig

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * [RemoteConfigRepository] needs a real `Context` (and, for the second test below, a real
 * Firebase project) — hence `androidTest` rather than a plain unit test.
 */
@RunWith(AndroidJUnit4::class)
class RemoteConfigRepositoryTest {

    /**
     * Regression test for a real bug caught via a manual on-device check: `getBoolean()`/
     * `getValue()` never throw for a key with no default and no fetched value yet — they
     * silently return the SDK's own built-in `false`, not [FeatureFlag.defaultValue]. Before
     * [isEnabled] checked `getValue(...).source`, this returned `false` for
     * [FeatureFlag.PDF_EXPORT_ENABLED] (default `true`) immediately after construction, racing
     * against the async `setDefaultsAsync()` call — exactly the "no Firebase project configured
     * yet" moment every fresh install goes through.
     */
    @Test fun isEnabled_returnsTheCompiledDefault_immediatelyAfterConstruction_beforeAnyFetch() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val repository = RemoteConfigRepository(context)

        // No refresh() call yet — this must still be the compiled default, not whatever
        // Firebase's own static fallback for an unset boolean happens to be.
        assertEquals(FeatureFlag.PDF_EXPORT_ENABLED.defaultValue, repository.isEnabled(FeatureFlag.PDF_EXPORT_ENABLED))
        assertEquals(FeatureFlag.BACKUP_RESTORE_ENABLED.defaultValue, repository.isEnabled(FeatureFlag.BACKUP_RESTORE_ENABLED))
    }
}
