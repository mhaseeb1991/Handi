package dev.haseeb.handi.data.remote

/**
 * A recipe as stored in Firestore, under `users/{uid}/recipes/{id}` — the remote side of Phase 2
 * (issue #6), not yet read from or written to by any sync flow (that's Phase 3, issue #7). All
 * fields have defaults so Firestore's POJO deserializer can construct this from a document
 * (Firestore's recommended shape for a Kotlin data class).
 *
 * No photo: recipe photos live in app-private storage locally (`data/image/ImageStorage.kt`)
 * and aren't portable file paths across devices — syncing them is a Firebase Storage upload,
 * which is Phase 3's job, not this data shape's.
 */
data class RemoteRecipe(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val servings: Int = 0,
    val cookMinutes: Int = 0,
    val ingredients: List<RemoteIngredient> = emptyList(),
    val steps: List<String> = emptyList(),
    val updatedAt: Long = 0,
    val sourceUrl: String? = null,
)

data class RemoteIngredient(
    val name: String = "",
    val categoryId: String = "",
    val amount: Double? = null,
    /** [dev.haseeb.handi.data.model.Measure.name], e.g. "GRAM" — same convention as `data/backup/`. */
    val measure: String = "",
    val note: String = "",
)
