package dev.haseeb.handi.data.local

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * [RecipeDao.save]'s insert-with-explicit-id path and the `remoteId` column are what
 * [dev.haseeb.handi.data.sync.SyncRepository] (issue #7) relies on to pull a recipe that's new
 * to this device but already has a known local id reserved for it. Needs a real (in-memory)
 * Room database — Mockito/fakes can't stand in for SQLite's actual insert-vs-update behavior.
 */
@RunWith(AndroidJUnit4::class)
class RecipeDaoSyncTest {

    private lateinit var db: HandiDatabase
    private lateinit var dao: RecipeDao

    @Before
    fun createDb() {
        db = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext, HandiDatabase::class.java)
            .build()
        dao = db.recipeDao()
    }

    @After
    fun closeDb() = db.close()

    private fun recipe(id: Long = 0) = RecipeEntity(
        id = id,
        title = "Aloo Gobi",
        description = "A dry curry.",
        imagePath = null,
        servings = 4,
        cookMinutes = 35,
        createdAt = 1000,
        updatedAt = 1000,
    )

    @Test
    fun exists_isFalseForAnUnknownId_andTrueOnceSaved() = runBlocking {
        assertFalse(dao.exists(42))
        dao.save(recipe(id = 42), emptyList(), emptyList())
        assertTrue(dao.exists(42))
    }

    @Test
    fun save_withAnExplicitIdThatDoesNotExistYet_insertsAtThatId() = runBlocking {
        val id = dao.save(recipe(id = 42), emptyList(), emptyList())

        assertEquals(42L, id)
        assertEquals("Aloo Gobi", dao.get(42)?.recipe?.title)
    }

    @Test
    fun save_withAnExplicitIdThatAlreadyExists_updatesInPlaceInsteadOfInserting() = runBlocking {
        dao.save(recipe(id = 42), emptyList(), emptyList())

        val id = dao.save(recipe(id = 42).copy(title = "Updated Aloo Gobi"), emptyList(), emptyList())

        assertEquals(42L, id)
        assertEquals("Updated Aloo Gobi", dao.get(42)?.recipe?.title)
    }

    @Test
    fun setRemoteId_persistsTheIdWithoutTouchingOtherColumns() = runBlocking {
        dao.save(recipe(id = 1), emptyList(), emptyList())

        dao.setRemoteId(1, "doc-abc")

        val saved = dao.get(1)?.recipe
        assertEquals("doc-abc", saved?.remoteId)
        assertEquals("Aloo Gobi", saved?.title)
        assertEquals(1000L, saved?.updatedAt)
    }

    @Test
    fun aFreshRecipe_hasNoRemoteIdUntilOneIsAssigned() = runBlocking {
        dao.save(recipe(id = 1), emptyList(), emptyList())
        assertNull(dao.get(1)?.recipe?.remoteId)
    }
}
