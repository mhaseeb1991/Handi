package dev.haseeb.handi.data.pdf

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import java.io.File

/**
 * Draws a [RecipePdfContent] onto an A4-ish [PdfDocument], paginating as needed, styled with
 * Handi's own paper/ink/paprika palette (see `core/designsystem/theme/Color.kt`) rather than
 * default black-on-white text — this is pure `android.graphics`, the one piece of the PDF
 * feature that genuinely needs the Android framework; everything about *what* text appears and
 * in what order lives in the already-tested [RecipePdfContent]/[toPdfContent].
 */
internal class RecipePdfRenderer(private val content: RecipePdfContent) {

    private val pageWidth = 595 // A4 at 72dpi
    private val pageHeight = 842
    private val margin = 54f
    private val contentWidth = pageWidth - margin * 2

    // Handi's light palette, hand-copied from core/designsystem/theme/Color.kt — a PDF is
    // read in whatever light a phone/printer gives it, so it always uses the light scheme.
    private val paper = Color.parseColor("#F5EFE4")
    private val paperLow = Color.parseColor("#F0E8DB")
    private val ink = Color.parseColor("#221C16")
    private val inkSoft = Color.parseColor("#5E544A")
    private val paprika = Color.parseColor("#B5412A")
    private val rule = Color.parseColor("#D4C8B7")

    private val serifBold = Typeface.create(Typeface.SERIF, Typeface.BOLD)
    private val serifItalic = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
    private val sansBold = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    private val sans = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)

    private val pagePaint = Paint().apply { color = paper; style = Paint.Style.FILL }
    private val accentBarPaint = Paint().apply { color = paprika; style = Paint.Style.FILL }
    private val titlePaint = textPaint(26f, serifBold, ink)
    private val kickerPaint = textPaint(10f, sansBold, paprika).apply { letterSpacing = 0.12f }
    private val descriptionPaint = textPaint(12.5f, serifItalic, inkSoft)
    private val linkPaint = textPaint(11f, sansBold, paprika)
    private val sectionPaint = textPaint(15f, serifBold, ink)
    private val categoryPaint = textPaint(10f, sansBold, inkSoft).apply { letterSpacing = 0.1f }
    private val ingredientPaint = textPaint(12f, sans, ink)
    private val amountPaint = textPaint(12f, sansBold, paprika)
    private val stepNumberPaint = textPaint(16f, serifBold, paprika)
    private val stepTextPaint = textPaint(12f, sans, ink)
    private val statValuePaint = textPaint(18f, serifBold, ink)
    private val statLabelPaint = textPaint(9f, sansBold, inkSoft).apply { letterSpacing = 0.1f }
    private val rulePaint = Paint().apply { color = rule; strokeWidth = 1f }
    private val footerPaint = textPaint(8.5f, sans, inkSoft)

    private fun textPaint(size: Float, typeface: Typeface, textColor: Int) = Paint().apply {
        textSize = size
        this.typeface = typeface
        color = textColor
        isAntiAlias = true
    }

    private var pageNumber = 1
    private lateinit var page: PdfDocument.Page
    private var y = margin

    fun renderAllPages(document: PdfDocument) {
        page = startPage(document)

        drawKicker(document, "A RECIPE FROM YOUR HANDI")
        y += 4f
        drawWrapped(document, content.title, titlePaint, 30f)

        content.imagePath?.let { path -> drawPhoto(document, path) }

        y += 10f
        drawStatsRow(document)

        if (content.description.isNotEmpty()) {
            y += 16f
            drawWrapped(document, content.description, descriptionPaint, 17f)
        }
        content.sourceUrl?.let { url ->
            y += 10f
            drawKicker(document, "VIEW ORIGINAL RECIPE", linkPaint)
            y -= 2f
            drawWrapped(document, url, linkPaint.withSize(9.5f), 13f)
        }

        y += 22f
        drawSectionHeader(document, "Ingredients")
        content.ingredientGroups.forEachIndexed { index, group ->
            if (index > 0) y += 10f
            drawCategoryHeader(document, group.categoryName, group.categoryTint)
            group.lines.forEach { line -> drawIngredientRow(document, line) }
        }

        if (content.steps.isNotEmpty()) {
            y += 22f
            drawSectionHeader(document, "Method")
            content.steps.forEachIndexed { index, step -> drawStep(document, index + 1, step) }
        }

        finishPageWithFooter(document)
    }

    // ---- page lifecycle -----------------------------------------------------------------------

    private fun startPage(document: PdfDocument): PdfDocument.Page {
        y = margin
        val newPage = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        val canvas = newPage.canvas
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), pageHeight.toFloat(), pagePaint)
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), 6f, accentBarPaint)
        return newPage
    }

    private fun finishPageWithFooter(document: PdfDocument) {
        drawFooter()
        document.finishPage(page)
    }

    private fun newPageIfNeeded(document: PdfDocument, neededHeight: Float) {
        if (y + neededHeight > pageHeight - margin - FOOTER_RESERVED) {
            drawFooter()
            document.finishPage(page)
            pageNumber++
            page = startPage(document)
        }
    }

    private fun drawFooter() {
        val footerY = pageHeight - margin + 6f
        page.canvas.drawLine(margin, footerY - 14f, pageWidth - margin, footerY - 14f, rulePaint)
        page.canvas.drawText("Handi", margin, footerY, footerPaint)
        val pageLabel = "Page $pageNumber"
        page.canvas.drawText(pageLabel, pageWidth - margin - footerPaint.measureText(pageLabel), footerY, footerPaint)
    }

    // ---- content blocks -------------------------------------------------------------------------

    private fun drawKicker(document: PdfDocument, text: String, paint: Paint = kickerPaint) {
        newPageIfNeeded(document, 14f)
        page.canvas.drawText(text, margin, y + paint.textSize, paint)
        y += 14f
    }

    private fun drawStatsRow(document: PdfDocument) {
        val hasTime = content.timeLabel.isNotEmpty()
        val boxHeight = 52f
        newPageIfNeeded(document, boxHeight)
        val boxWidth = if (hasTime) (contentWidth - 12f) / 2 else contentWidth

        fun box(x: Float, width: Float, value: String, label: String) {
            val rect = RectF(x, y, x + width, y + boxHeight)
            page.canvas.drawRoundRect(rect, 10f, 10f, Paint().apply { color = paperLow; isAntiAlias = true })
            val valueWidth = statValuePaint.measureText(value)
            val labelWidth = statLabelPaint.measureText(label)
            page.canvas.drawText(value, x + (width - valueWidth) / 2, y + 28f, statValuePaint)
            page.canvas.drawText(label, x + (width - labelWidth) / 2, y + 44f, statLabelPaint)
        }

        box(margin, boxWidth, content.servingsLabel, "SERVES")
        if (hasTime) box(margin + boxWidth + 12f, boxWidth, content.timeLabel, "COOK TIME")
        y += boxHeight
    }

    private fun drawSectionHeader(document: PdfDocument, title: String) {
        newPageIfNeeded(document, 30f)
        page.canvas.drawText(title, margin, y + sectionPaint.textSize, sectionPaint)
        val underlineY = y + sectionPaint.textSize + 6f
        page.canvas.drawLine(margin, underlineY, margin + 32f, underlineY, Paint().apply { color = paprika; strokeWidth = 2.5f })
        y += 30f
    }

    private fun drawCategoryHeader(document: PdfDocument, name: String, tint: Long) {
        newPageIfNeeded(document, 18f)
        val dotRadius = 4f
        page.canvas.drawCircle(margin + dotRadius, y + 7f, dotRadius, Paint().apply { color = tint.toInt(); isAntiAlias = true })
        page.canvas.drawText(name.uppercase(), margin + dotRadius * 2 + 8f, y + 10f, categoryPaint)
        y += 18f
    }

    private fun drawIngredientRow(document: PdfDocument, line: RecipePdfContent.IngredientLine) {
        val amountColumnWidth = 90f
        val labelWidth = contentWidth - amountColumnWidth
        val lines = wrapLines(line.label, ingredientPaint, labelWidth)
        val rowHeight = lines.size * 16f + 10f
        newPageIfNeeded(document, rowHeight)

        lines.forEachIndexed { index, text ->
            page.canvas.drawText(text, margin, y + ingredientPaint.textSize, ingredientPaint)
            if (index == 0) {
                val amountWidth = amountPaint.measureText(line.amount)
                page.canvas.drawText(line.amount, margin + contentWidth - amountWidth, y + amountPaint.textSize, amountPaint)
            }
            y += 16f
        }
        y += 2f
        page.canvas.drawLine(margin, y + 4f, margin + contentWidth, y + 4f, rulePaint)
        y += 8f
    }

    private fun drawStep(document: PdfDocument, number: Int, text: String) {
        val numberColumnWidth = 34f
        val textWidth = contentWidth - numberColumnWidth
        val lines = wrapLines(text, stepTextPaint, textWidth)
        val blockHeight = (lines.size * 16f).coerceAtLeast(20f) + 10f
        newPageIfNeeded(document, blockHeight)

        page.canvas.drawText(number.toString().padStart(2, '0'), margin, y + stepNumberPaint.textSize, stepNumberPaint)
        lines.forEachIndexed { index, line ->
            page.canvas.drawText(line, margin + numberColumnWidth, y + 4f + index * 16f + stepTextPaint.textSize, stepTextPaint)
        }
        y += blockHeight
    }

    private fun drawPhoto(document: PdfDocument, path: String) {
        val bitmap = decodeSampledBitmap(path, reqWidth = 900, reqHeight = 400) ?: return
        val photoHeight = 170f
        newPageIfNeeded(document, photoHeight + 12f)
        y += 12f

        val dest = RectF(margin, y, margin + contentWidth, y + photoHeight)
        val clip = Path().apply { addRoundRect(dest, 12f, 12f, Path.Direction.CW) }
        page.canvas.save()
        page.canvas.clipPath(clip)
        page.canvas.drawBitmap(bitmap, centerCropSource(bitmap, dest), dest, null)
        page.canvas.restore()
        bitmap.recycle()
        y += photoHeight
    }

    // ---- text measuring / wrapping -----------------------------------------------------------

    /** Word-wraps [text] to [maxWidth], never splitting mid-word when a space is available. */
    private fun wrapLines(text: String, paint: Paint, maxWidth: Float): List<String> {
        if (text.isEmpty()) return listOf("")
        val lines = mutableListOf<String>()
        var start = 0
        while (start < text.length) {
            var count = paint.breakText(text, start, text.length, true, maxWidth, null)
            if (start + count < text.length) {
                val lastSpace = text.lastIndexOf(' ', start + count - 1)
                if (lastSpace > start) count = lastSpace - start
            }
            val end = (start + count).coerceAtLeast(start + 1).coerceAtMost(text.length)
            lines += text.substring(start, end)
            start = end
            while (start < text.length && text[start] == ' ') start++
        }
        return lines
    }

    private fun drawWrapped(document: PdfDocument, text: String, paint: Paint, lineHeight: Float) {
        wrapLines(text, paint, contentWidth).forEach { line ->
            newPageIfNeeded(document, lineHeight)
            page.canvas.drawText(line, margin, y + paint.textSize, paint)
            y += lineHeight
        }
    }

    private fun Paint.withSize(size: Float): Paint = Paint(this).apply { textSize = size }

    // ---- bitmap helpers -------------------------------------------------------------------------

    private fun decodeSampledBitmap(path: String, reqWidth: Int, reqHeight: Int): Bitmap? {
        if (!File(path).exists()) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sampleSize = 1
        var halfWidth = bounds.outWidth / 2
        var halfHeight = bounds.outHeight / 2
        while (halfWidth / sampleSize >= reqWidth && halfHeight / sampleSize >= reqHeight) {
            sampleSize *= 2
        }
        return BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sampleSize })
    }

    /** The source rect to crop from [bitmap] so drawing it into [dest] behaves like `ContentScale.Crop`. */
    private fun centerCropSource(bitmap: Bitmap, dest: RectF): android.graphics.Rect {
        val destRatio = dest.width() / dest.height()
        val bitmapRatio = bitmap.width.toFloat() / bitmap.height
        return if (bitmapRatio > destRatio) {
            val croppedWidth = (bitmap.height * destRatio).toInt()
            val xOffset = (bitmap.width - croppedWidth) / 2
            android.graphics.Rect(xOffset, 0, xOffset + croppedWidth, bitmap.height)
        } else {
            val croppedHeight = (bitmap.width / destRatio).toInt()
            val yOffset = (bitmap.height - croppedHeight) / 2
            android.graphics.Rect(0, yOffset, bitmap.width, yOffset + croppedHeight)
        }
    }

    private companion object {
        /** Extra bottom space reserved on every page so the footer never collides with content. */
        const val FOOTER_RESERVED = 26f
    }
}
