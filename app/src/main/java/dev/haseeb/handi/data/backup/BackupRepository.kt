package dev.haseeb.handi.data.backup

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.haseeb.handi.data.image.ImageStorage
import dev.haseeb.handi.data.model.Measure
import dev.haseeb.handi.data.repository.CatalogRepository
import dev.haseeb.handi.data.repository.RecipeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** Number of recipes / custom ingredients written to or read from a backup archive. */
data class BackupSummary(val recipes: Int, val customIngredients: Int)

/**
 * Exports every recipe (and its photo) plus custom catalogue ingredients to a single `.zip`
 * the user picks a destination for, and restores from one.
 *
 * Import strategy (deliberate, see `docs/DATA_LAYER.md`): there is no stable id shared
 * between devices/installs, so every imported recipe is inserted as a **new** row rather
 * than attempting to match/overwrite an existing one. Re-importing the same archive twice
 * will therefore create duplicate recipes — this is a known v1 limitation, not a bug.
 * Custom ingredients are deduplicated by Room's existing unique `(categoryId, name)` index,
 * so re-importing those is safe.
 */
@Singleton
class BackupRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val recipes: RecipeRepository,
    private val catalog: CatalogRepository,
    private val images: ImageStorage,
) {
    /** Writes a backup archive to [destination] (an SAF "create document" Uri). */
    suspend fun export(destination: Uri): BackupSummary = withContext(Dispatchers.IO) {
        val allRecipes = recipes.recipes.first()
        val customIngredients = catalog.ingredientsByCategory.first().values.flatten().filter { it.isCustom }

        val imageBytes = mutableMapOf<String, ByteArray>()
        val backupRecipes = allRecipes.map { recipe ->
            val imageFile = recipe.imagePath
                ?.let { path -> File(path).takeIf { it.exists() && it.isFile } }
                ?.runCatching { readBytes() }
                ?.getOrNull()
                ?.let { bytes ->
                    val name = "img_${imageBytes.size}.jpg"
                    imageBytes[name] = bytes
                    name
                }
            recipe.toBackup(imageFile)
        }

        val manifest = BackupFile(
            exportedAt = System.currentTimeMillis(),
            recipes = backupRecipes,
            customIngredients = customIngredients.map { it.toBackupCustom() },
        )
        val json = BackupSerializer.encode(manifest)

        val out = context.contentResolver.openOutputStream(destination)
            ?: throw BackupFormatException("Could not open the chosen location for writing")
        out.use { BackupArchive.write(it, json, imageBytes) }

        BackupSummary(recipes = backupRecipes.size, customIngredients = customIngredients.size)
    }

    /** Reads a backup archive from [source] (an SAF "open document" Uri) and restores it. */
    suspend fun import(source: Uri): BackupSummary = withContext(Dispatchers.IO) {
        val input = context.contentResolver.openInputStream(source)
            ?: throw BackupFormatException("Could not open the chosen file for reading")
        val archive = input.use { BackupArchive.read(it) }
        val manifest = BackupSerializer.decode(archive.manifestJson)

        manifest.customIngredients.forEach { backupIngredient ->
            catalog.addCustomIngredient(
                categoryId = backupIngredient.categoryId,
                name = backupIngredient.name,
                unit = Measure.from(backupIngredient.measure),
            )
        }

        manifest.recipes.forEach { backupRecipe ->
            val imagePath = backupRecipe.imageFile
                ?.let { name -> archive.images[name] }
                ?.let { bytes -> images.importBytes(bytes) }
            recipes.save(backupRecipe.toRecipe(imagePath))
        }

        BackupSummary(recipes = manifest.recipes.size, customIngredients = manifest.customIngredients.size)
    }
}
