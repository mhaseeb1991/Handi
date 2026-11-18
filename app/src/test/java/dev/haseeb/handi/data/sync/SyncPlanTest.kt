package dev.haseeb.handi.data.sync

import dev.haseeb.handi.data.model.Recipe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncPlanTest {

    private fun recipe(id: Long = 0, remoteId: String? = null, updatedAt: Long = 0, title: String = "Recipe") = Recipe(
        id = id,
        title = title,
        description = "",
        imagePath = null,
        servings = 1,
        cookMinutes = 1,
        ingredients = emptyList(),
        steps = emptyList(),
        updatedAt = updatedAt,
        remoteId = remoteId,
    )

    @Test fun `a remote recipe with no local counterpart is pulled as a brand new local recipe`() {
        val remote = recipe(remoteId = "r1", updatedAt = 100, title = "Chana Masala")

        val toPull = recipesToPull(local = emptyList(), remoteByRemoteId = mapOf("r1" to remote))

        assertEquals(1, toPull.size)
        assertEquals(0L, toPull[0].id)
        assertEquals("Chana Masala", toPull[0].title)
    }

    @Test fun `a remote recipe newer than its local copy is pulled, keeping the existing local id`() {
        val local = recipe(id = 7, remoteId = "r1", updatedAt = 100)
        val remote = recipe(remoteId = "r1", updatedAt = 200, title = "Updated")

        val toPull = recipesToPull(local = listOf(local), remoteByRemoteId = mapOf("r1" to remote))

        assertEquals(1, toPull.size)
        assertEquals(7L, toPull[0].id)
        assertEquals("Updated", toPull[0].title)
    }

    @Test fun `a remote recipe that is the same age or older than its local copy is not pulled`() {
        val local = recipe(id = 7, remoteId = "r1", updatedAt = 200)
        val remote = recipe(remoteId = "r1", updatedAt = 200)

        assertTrue(recipesToPull(local = listOf(local), remoteByRemoteId = mapOf("r1" to remote)).isEmpty())
    }

    @Test fun `a local recipe never pushed before is pushed`() {
        val local = recipe(id = 1, remoteId = null, updatedAt = 100)

        assertEquals(listOf(local), recipesToPush(local = listOf(local), remoteByRemoteId = emptyMap()))
    }

    @Test fun `a local recipe newer than its remote copy is pushed`() {
        val local = recipe(id = 1, remoteId = "r1", updatedAt = 200)
        val remote = recipe(remoteId = "r1", updatedAt = 100)

        assertEquals(listOf(local), recipesToPush(local = listOf(local), remoteByRemoteId = mapOf("r1" to remote)))
    }

    @Test fun `a local recipe that is the same age or older than its remote copy is not pushed`() {
        val local = recipe(id = 1, remoteId = "r1", updatedAt = 100)
        val remote = recipe(remoteId = "r1", updatedAt = 200)

        assertTrue(recipesToPush(local = listOf(local), remoteByRemoteId = mapOf("r1" to remote)).isEmpty())
    }
}
