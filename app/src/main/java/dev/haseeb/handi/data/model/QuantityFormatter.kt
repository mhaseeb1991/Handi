package dev.haseeb.handi.data.model

import java.util.Locale
import kotlin.math.abs
import kotlin.math.floor

object QuantityFormatter {

    private val fractions = listOf(
        0.25 to "¼", 0.333 to "⅓", 0.5 to "½", 0.666 to "⅔", 0.75 to "¾",
    )

    /** 1.5 -> "1½", 0.25 -> "¼", 2.0 -> "2", 1.2 -> "1.2" */
    fun amount(value: Double): String {
        val whole = floor(value)
        val rest = value - whole
        if (rest < 0.01) return whole.toLong().toString()
        val glyph = fractions.firstOrNull { abs(it.first - rest) < 0.02 }?.second
        return if (glyph != null) {
            if (whole == 0.0) glyph else "${whole.toLong()}$glyph"
        } else {
            String.format(Locale.US, "%.2f", value).trimEnd('0').trimEnd('.')
        }
    }

    fun format(amount: Double?, measure: Measure, scale: Double = 1.0): String {
        if (!measure.needsAmount || amount == null) return measure.short
        val scaled = amount * scale
        val unit = when (measure) {
            Measure.PIECE -> if (scaled > 1) "pcs" else "pc"
            Measure.CUP, Measure.CLOVE, Measure.BUNCH, Measure.PINCH, Measure.HANDFUL ->
                if (scaled > 1) measure.short.pluralise() else measure.short
            else -> measure.short
        }
        return "${amount(scaled)} $unit"
    }

    private fun String.pluralise() = when {
        endsWith("ch") -> this + "es"
        else -> this + "s"
    }

    /** Accepts "1.5", "1,5", "1/2", "1 1/2", "½" style input. */
    fun parse(input: String): Double? {
        val text = input.trim().replace(',', '.')
        if (text.isEmpty()) return null
        fractions.firstOrNull { text.endsWith(it.second) }?.let { (value, glyph) ->
            val head = text.removeSuffix(glyph).trim()
            val whole = if (head.isEmpty()) 0.0 else head.toDoubleOrNull() ?: return null
            return whole + value
        }
        val parts = text.split(' ').filter { it.isNotBlank() }
        return when {
            parts.size == 2 && parts[1].contains('/') -> parts[0].toDoubleOrNull()?.let { w -> fraction(parts[1])?.plus(w) }
            text.contains('/') -> fraction(text)
            else -> text.toDoubleOrNull()
        }?.takeIf { it > 0 }
    }

    private fun fraction(text: String): Double? {
        val (n, d) = text.split('/').takeIf { it.size == 2 } ?: return null
        val num = n.trim().toDoubleOrNull() ?: return null
        val den = d.trim().toDoubleOrNull()?.takeIf { it != 0.0 } ?: return null
        return num / den
    }
}
