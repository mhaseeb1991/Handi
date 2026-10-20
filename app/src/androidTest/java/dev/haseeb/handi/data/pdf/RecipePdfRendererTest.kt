package dev.haseeb.handi.data.pdf

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfDocument
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * [RecipePdfRenderer] is the one piece of the PDF feature that genuinely needs a device —
 * `android.graphics.pdf.PdfDocument` isn't available to plain JVM unit tests. This exercises
 * it end to end: build real pages, write real bytes, and sanity-check the output is an actual
 * PDF rather than just "didn't throw."
 */
@RunWith(AndroidJUnit4::class)
class RecipePdfRendererTest {

    private fun render(content: RecipePdfContent): ByteArray {
        val document = PdfDocument()
        try {
            RecipePdfRenderer(content).renderAllPages(document)
            val out = ByteArrayOutputStream()
            document.writeTo(out)
            return out.toByteArray()
        } finally {
            document.close()
        }
    }

    private fun sample(
        steps: List<String> = listOf("Chop everything.", "Cook until tender."),
        imagePath: String? = null,
    ) = RecipePdfContent(
        title = "Aloo Gobi",
        description = "A dry potato & cauliflower curry.",
        servingsLabel = "4",
        timeLabel = "35 min",
        sourceUrl = "https://example.com/aloo-gobi",
        imagePath = imagePath,
        ingredientGroups = listOf(
            RecipePdfContent.IngredientGroup(
                "Vegetables",
                0xFF5B8A3C,
                listOf(
                    RecipePdfContent.IngredientLine("Potato", "2 pcs"),
                    RecipePdfContent.IngredientLine("Cauliflower (cut into florets)", "1 pc"),
                ),
            ),
        ),
        steps = steps,
    )

    @Test fun rendersAShortRecipeToOnePdfPage() {
        val bytes = render(sample())

        assertTrue("expected non-trivial PDF output", bytes.size > 100)
        assertTrue("expected a %PDF header", String(bytes, 0, 5, Charsets.ISO_8859_1) == "%PDF-")
    }

    @Test fun paginatesALongMethodAcrossMultiplePages() {
        val longSteps = (1..120).map { "Step $it: do something reasonably descriptive while cooking this dish." }
        val document = PdfDocument()
        try {
            RecipePdfRenderer(sample(steps = longSteps)).renderAllPages(document)
            assertTrue("a 120-step method should overflow one page", document.pages.size > 1)
        } finally {
            document.close()
        }
    }

    @Test fun handlesNoIngredientsAndNoStepsWithoutCrashing() {
        val empty = RecipePdfContent(
            title = "Empty Draft",
            description = "",
            servingsLabel = "1",
            timeLabel = "",
            sourceUrl = null,
            imagePath = null,
            ingredientGroups = emptyList(),
            steps = emptyList(),
        )
        val bytes = render(empty)
        assertTrue(bytes.isNotEmpty())
    }

    @Test fun rendersThePhotoWithoutCrashingWhenOneIsPresent() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.cacheDir, "pdf_renderer_test.jpg")
        Bitmap.createBitmap(40, 30, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.RED)
            file.outputStream().use { compress(Bitmap.CompressFormat.JPEG, 90, it) }
            recycle()
        }

        val bytes = render(sample(imagePath = file.absolutePath))

        assertTrue("expected a %PDF header even with a photo embedded", String(bytes, 0, 5, Charsets.ISO_8859_1) == "%PDF-")
        file.delete()
    }

    @Test fun missingPhotoFileIsSkippedRatherThanCrashing() {
        val bytes = render(sample(imagePath = "/no/such/file.jpg"))
        assertTrue(bytes.isNotEmpty())
    }
}
