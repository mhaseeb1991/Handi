package dev.haseeb.handi.data.pdf

import android.content.Context
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.haseeb.handi.data.model.Recipe
import dev.haseeb.handi.data.repository.CatalogRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Renders a recipe to a PDF in app-private cache storage and hands back a `content://` [Uri]
 * suitable for `Intent.ACTION_SEND` — mirrors the pattern `ImageStorage.newCameraUri()` already
 * uses for sharing app-private files via the manifest's `FileProvider`.
 */
@Singleton
class RecipePdfExporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val catalog: CatalogRepository,
) {
    private val exportDir: File get() = File(context.cacheDir, "pdf_exports").apply { mkdirs() }

    /** [servings] is whatever the user currently has the detail screen scaled to. */
    suspend fun export(recipe: Recipe, servings: Int): Uri = withContext(Dispatchers.IO) {
        val categoryNames = catalog.categories.first().associate { it.id to it.name }
        val content = recipe.toPdfContent(servings, categoryNames)

        // Previous exports are disposable scratch files, not something we keep around.
        exportDir.listFiles()?.forEach { it.delete() }
        val file = File(exportDir, "${content.title.toFileSlug()}.pdf")

        val document = PdfDocument()
        try {
            RecipePdfRenderer(content).renderAllPages(document)
            FileOutputStream(file).use { document.writeTo(it) }
        } finally {
            document.close()
        }

        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }
}

internal fun String.toFileSlug(): String {
    val slug = map { if (it.isLetterOrDigit()) it else '-' }.joinToString("").trim('-').take(60)
    return slug.ifEmpty { "recipe" }
}
