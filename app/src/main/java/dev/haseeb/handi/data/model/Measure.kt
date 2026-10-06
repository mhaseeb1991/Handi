package dev.haseeb.handi.data.model

/**
 * Units a user can pick for an ingredient. [TO_TASTE] needs no amount,
 * which covers the "free text" style quantities like salt "to taste".
 */
enum class Measure(val label: String, val short: String, val needsAmount: Boolean = true) {
    GRAM("gram", "g"),
    KILOGRAM("kilogram", "kg"),
    MILLILITRE("millilitre", "ml"),
    LITRE("litre", "L"),
    CUP("cup", "cup"),
    TABLESPOON("tablespoon", "tbsp"),
    TEASPOON("teaspoon", "tsp"),
    PIECE("piece", "pc"),
    CLOVE("clove", "clove"),
    BUNCH("bunch", "bunch"),
    HANDFUL("handful", "handful"),
    PINCH("pinch", "pinch"),
    TO_TASTE("to taste", "to taste", needsAmount = false),
    ;

    companion object {
        fun from(raw: String?): Measure = entries.firstOrNull { it.name == raw } ?: PIECE
    }
}
