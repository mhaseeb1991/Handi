package dev.haseeb.handi.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Safety net for schema changes, using the exported schema JSON in `app/schemas/` (see
 * `ksp { arg("room.schemaLocation", ...) }` in `app/build.gradle.kts`).
 *
 * `HandiDatabase` has only ever been `version = 1` — there's nothing to migrate *from* yet, so
 * there's nothing to migrate *to* test. What this test does today is validate that the
 * version-1 schema Room generates from the current `@Entity` classes still matches the
 * originally exported `1.json`, catching an accidental, unexported entity change before it
 * would otherwise only surface at runtime on a user's device.
 *
 * **When you bump the database version and write a real `Migration`:** add a test here that
 * calls `helper.createDatabase(TEST_DB, 1)` (optionally inserting rows to carry across), then
 * `helper.runMigrationsAndValidate(TEST_DB, <newVersion>, true, YourMigration)` and assert the
 * carried-over data survived. Keep this version-1 test as-is; it documents the baseline.
 *
 * Needs a device or emulator to run (`./gradlew connectedAndroidTest`) — this repo has no CI
 * emulator wired up yet (see `docs/README.md`'s "known gaps"), so run it locally before/after
 * writing a migration.
 */
@RunWith(AndroidJUnit4::class)
class HandiDatabaseMigrationTest {

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        HandiDatabase::class.java,
    )

    @Test
    fun version1SchemaCreatesSuccessfully() {
        helper.createDatabase(TEST_DB, 1).close()
    }

    private companion object {
        const val TEST_DB = "handi-migration-test.db"
    }
}
