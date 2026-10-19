package dev.haseeb.handi.data.pdf

import android.graphics.pdf.PdfDocument
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream

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

    @Test fun rendersAShortRecipeToOnePdfPage() {
        val content = RecipePdfContent(
            title = "Aloo Gobi",
            description = "A dry potato & cauliflower curry.",
            metaLine = "Serves 4 · 35 min",
            sourceUrl = "https://example.com/aloo-gobi",
            ingredientGroups = listOf(
                RecipePdfContent.IngredientGroup("Vegetables", listOf("Potato — 2 pcs", "Cauliflower — 1 pc")),
            ),
            steps = listOf("Chop everything.", "Cook until tender."),
        )

        val bytes = render(content)

        assertTrue("expected non-trivial PDF output", bytes.size > 100)
        assertTrue("expected a %PDF header", String(bytes, 0, 5, Charsets.ISO_8859_1) == "%PDF-")
    }

    @Test fun paginatesALongMethodAcrossMultiplePages() {
        val longSteps = (1..120).map { "Step $it: do something reasonably descriptive while cooking this dish." }
        val content = RecipePdfContent(
            title = "A Very Long Recipe",
            description = "",
            metaLine = "Serves 4",
            sourceUrl = null,
            ingredientGroups = emptyList(),
            steps = longSteps,
        )

        val document = PdfDocument()
        try {
            RecipePdfRenderer(content).renderAllPages(document)
            assertTrue("a 120-step method should overflow one page", document.pages.size > 1)
        } finally {
            document.close()
        }
    }

    @Test fun handlesNoIngredientsAndNoStepsWithoutCrashing() {
        val content = RecipePdfContent(
            title = "Empty Draft",
            description = "",
            metaLine = "Serves 1",
            sourceUrl = null,
            ingredientGroups = emptyList(),
            steps = emptyList(),
        )
        val bytes = render(content)
        assertTrue(bytes.isNotEmpty())
    }
}
