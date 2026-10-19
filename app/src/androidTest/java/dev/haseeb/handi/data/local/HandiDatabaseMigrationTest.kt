package dev.haseeb.handi.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Safety net for schema changes, using the exported schema JSON in `app/schemas/` (see
 * `ksp { arg("room.schemaLocation", ...) }` in `app/build.gradle.kts`).
 *
 * See `docs/DATA_LAYER.md`'s "how to add a migration safely" section for the process this
 * class follows. [migrate1To2_addsSourceUrlColumn_keepingExistingRows] is the live example: a
 * real migration (issue #10, adding `recipes.sourceUrl`), tested by creating a version-1
 * database, inserting a row, migrating it, and asserting that row survived with the new column
 * present (and null, since it didn't exist before).
 *
 * Needs a device or emulator to run (`./gradlew connectedAndroidTest` /
 * `connectedDebugAndroidTest`) — this repo has no CI emulator wired up yet (see
 * `docs/README.md`'s "known gaps"), so run it locally before/after writing a migration.
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

    @Test
    fun migrate1To2_addsSourceUrlColumn_keepingExistingRows() {
        helper.createDatabase(TEST_DB, 1).apply {
            execSQL(
                "INSERT INTO recipes (id, title, description, imagePath, servings, cookMinutes, createdAt, updatedAt) " +
                    "VALUES (1, 'Aloo Gobi', 'A dry curry.', NULL, 4, 35, 1000, 1000)",
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(TEST_DB, 2, true, HandiDatabase.MIGRATION_1_2)

        migrated.query("SELECT title, sourceUrl FROM recipes WHERE id = 1").use { cursor ->
            assertTrue("expected the pre-migration row to still be there", cursor.moveToFirst())
            assertEquals("Aloo Gobi", cursor.getString(cursor.getColumnIndexOrThrow("title")))
            assertTrue("sourceUrl should be NULL for a row that predates the column", cursor.isNull(cursor.getColumnIndexOrThrow("sourceUrl")))
        }
    }

    private companion object {
        const val TEST_DB = "handi-migration-test.db"
    }
}
