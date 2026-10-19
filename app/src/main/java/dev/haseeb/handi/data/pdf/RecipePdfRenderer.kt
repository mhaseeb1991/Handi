package dev.haseeb.handi.data.pdf

import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument

/**
 * Draws a [RecipePdfContent] onto an A4-ish [PdfDocument], paginating as needed. Pure
 * `android.graphics` — this is the one piece of the PDF feature that genuinely needs the
 * Android framework, so it's kept thin; everything about *what* text appears and in what order
 * lives in the already-tested [RecipePdfContent]/[toPdfContent].
 */
internal class RecipePdfRenderer(private val content: RecipePdfContent) {

    private val pageWidth = 595 // A4 at 72dpi
    private val pageHeight = 842
    private val margin = 48f
    private val contentWidth = pageWidth - margin * 2

    private val titlePaint = Paint().apply { textSize = 22f; isFakeBoldText = true; color = Color.BLACK; isAntiAlias = true }
    private val metaPaint = Paint().apply { textSize = 11f; color = Color.DKGRAY; isAntiAlias = true }
    private val sectionPaint = Paint().apply { textSize = 14f; isFakeBoldText = true; color = Color.BLACK; isAntiAlias = true }
    private val bodyPaint = Paint().apply { textSize = 12f; color = Color.BLACK; isAntiAlias = true }

    private var pageNumber = 1
    private lateinit var page: PdfDocument.Page
    private var y = margin

    fun renderAllPages(document: PdfDocument) {
        page = startPage(document)

        drawWrapped(document, content.title, titlePaint, 28f)
        drawWrapped(document, content.metaLine, metaPaint, 18f)
        if (content.description.isNotEmpty()) {
            y += 4f
            drawWrapped(document, content.description, bodyPaint, 16f)
        }
        content.sourceUrl?.let {
            y += 8f
            drawWrapped(document, "Source: $it", metaPaint, 16f)
        }

        y += 16f
        drawWrapped(document, "Ingredients", sectionPaint, 22f)
        content.ingredientGroups.forEach { group ->
            y += 6f
            drawWrapped(document, group.categoryName, metaPaint, 16f)
            group.lines.forEach { line -> drawWrapped(document, "• $line", bodyPaint, 16f) }
        }

        if (content.steps.isNotEmpty()) {
            y += 16f
            drawWrapped(document, "Method", sectionPaint, 22f)
            content.steps.forEachIndexed { index, step ->
                drawWrapped(document, "${index + 1}. $step", bodyPaint, 16f)
            }
        }

        document.finishPage(page)
    }

    private fun startPage(document: PdfDocument): PdfDocument.Page {
        y = margin
        return document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber++).create())
    }

    private fun newPageIfNeeded(document: PdfDocument, neededHeight: Float) {
        if (y + neededHeight > pageHeight - margin) {
            document.finishPage(page)
            page = startPage(document)
        }
    }

    /** Word-wraps [text] to [contentWidth] and draws it line by line, paginating as it goes. */
    private fun drawWrapped(document: PdfDocument, text: String, paint: Paint, lineHeight: Float) {
        if (text.isEmpty()) {
            newPageIfNeeded(document, lineHeight)
            y += lineHeight
            return
        }
        var start = 0
        while (start < text.length) {
            var count = paint.breakText(text, start, text.length, true, contentWidth, null)
            if (start + count < text.length) {
                // avoid splitting mid-word: back off to the last space in this chunk, if any
                val lastSpace = text.lastIndexOf(' ', start + count - 1)
                if (lastSpace > start) count = lastSpace - start
            }
            val end = (start + count).coerceAtLeast(start + 1).coerceAtMost(text.length)
            newPageIfNeeded(document, lineHeight)
            page.canvas.drawText(text, start, end, margin, y + paint.textSize, paint)
            y += lineHeight
            start = end
            while (start < text.length && text[start] == ' ') start++
        }
    }
}
