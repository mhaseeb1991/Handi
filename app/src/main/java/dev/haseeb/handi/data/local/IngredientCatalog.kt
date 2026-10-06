package dev.haseeb.handi.data.local

import dev.haseeb.handi.data.model.Measure
import dev.haseeb.handi.data.model.Measure.BUNCH
import dev.haseeb.handi.data.model.Measure.CLOVE
import dev.haseeb.handi.data.model.Measure.CUP
import dev.haseeb.handi.data.model.Measure.GRAM
import dev.haseeb.handi.data.model.Measure.MILLILITRE
import dev.haseeb.handi.data.model.Measure.PIECE
import dev.haseeb.handi.data.model.Measure.PINCH
import dev.haseeb.handi.data.model.Measure.TABLESPOON
import dev.haseeb.handi.data.model.Measure.TEASPOON

/**
 * Prefilled catalogue seeded on first launch. Each ingredient carries a sensible
 * default unit so the quantity sheet opens on the right unit (onion -> pcs, rice -> cup).
 */
internal object IngredientCatalog {

    data class SeedCategory(
        val id: String,
        val name: String,
        val tint: Long,
        val items: List<Pair<String, Measure>>,
    )

    private fun items(unit: Measure, vararg names: String) = names.map { it to unit }

    val categories: List<SeedCategory> = listOf(
        SeedCategory(
            "vegetables", "Vegetables", 0xFF5B8A3C,
            items(
                PIECE, "Tomato", "Onion", "Potato", "Green Chilli", "Carrot", "Cucumber", "Capsicum",
                "Brinjal (Baingan)", "Radish (Mooli)", "Turnip (Shaljam)", "Beetroot", "Sweet Potato",
                "Zucchini", "Corn on the Cob", "Lemon",
            ) + items(CLOVE, "Garlic") + items(
                GRAM, "Ginger", "Cabbage", "Cauliflower", "Peas", "Okra (Bhindi)", "Bottle Gourd (Lauki)",
                "Bitter Gourd (Karela)", "Pumpkin (Kaddu)", "Round Gourd (Tinda)", "Taro Root (Arvi)",
                "Green Beans", "Mushroom", "Broccoli", "Lettuce",
            ) + items(BUNCH, "Spinach (Palak)", "Spring Onion", "Mustard Greens (Sarson)"),
        ),
        SeedCategory(
            "fruits", "Fruits", 0xFFD2573B,
            items(
                PIECE, "Apple", "Banana", "Mango", "Orange", "Lime", "Pomegranate", "Guava", "Papaya",
                "Pineapple", "Peach", "Pear", "Plum", "Apricot", "Chikoo", "Coconut", "Avocado", "Kiwi",
            ) + items(GRAM, "Grapes", "Strawberries", "Cherries", "Watermelon", "Melon", "Blueberries"),
        ),
        SeedCategory(
            "pulses", "Pulses & Lentils", 0xFFC08A2E,
            items(
                CUP, "Masoor Dal (Red Lentils)", "Moong Dal", "Mash Dal (Urad)", "Chana Dal",
                "Arhar / Toor Dal", "Chickpeas (Kabuli Chana)", "Black Chickpeas (Kala Chana)",
                "Red Kidney Beans (Rajma)", "Black-eyed Beans (Lobia)", "Whole Green Moong",
                "Whole Masoor", "White Beans", "Soybeans", "Split Peas",
            ),
        ),
        SeedCategory(
            "meat", "Meat & Poultry", 0xFF9C3B34,
            items(
                GRAM, "Chicken (with bone)", "Chicken Boneless", "Chicken Breast", "Chicken Thighs",
                "Chicken Wings", "Chicken Mince", "Mutton", "Mutton Mince", "Beef", "Beef Mince (Qeema)",
                "Beef Undercut", "Lamb Chops", "Beef Shank (Nalli)", "Liver (Kaleji)", "Brain (Maghaz)",
                "Trotters (Paye)",
            ),
        ),
        SeedCategory(
            "seafood", "Seafood", 0xFF2F7A8C,
            items(GRAM, "Fish (Rohu)", "Pomfret", "Surmai", "Salmon", "Tuna", "Prawns", "Shrimp", "Crab", "Lobster", "Squid"),
        ),
        SeedCategory(
            "masala", "Masala & Spices", 0xFFC4661F,
            items(
                TEASPOON, "Salt", "Red Chilli Powder", "Kashmiri Chilli", "Turmeric (Haldi)",
                "Coriander Powder", "Cumin Seeds (Zeera)", "Cumin Powder", "Garam Masala", "Black Pepper",
                "Chaat Masala", "Crushed Red Chilli", "Fennel Seeds (Saunf)", "Mustard Seeds (Rai)",
                "Nigella Seeds (Kalonji)", "Carom Seeds (Ajwain)", "Whole Coriander", "Paprika",
                "Dry Mango Powder (Amchur)", "Pomegranate Seeds (Anardana)", "Nutmeg (Jaifal)",
                "Mace (Javitri)", "Biryani Masala", "Tikka Masala", "Nihari Masala",
            ) + items(
                PIECE, "Cinnamon Stick", "Green Cardamom", "Black Cardamom", "Cloves (Laung)",
                "Bay Leaf", "Star Anise", "Dry Red Chilli",
            ) + items(TABLESPOON, "Ginger Garlic Paste") + items(PINCH, "Saffron (Zafran)", "Asafoetida (Hing)"),
        ),
        SeedCategory(
            "herbs", "Fresh Herbs", 0xFF3F7D55,
            items(BUNCH, "Coriander Leaves", "Mint Leaves", "Fenugreek Leaves (Methi)", "Dill (Soya)", "Parsley", "Basil") +
                items(Measure.HANDFUL, "Curry Leaves") +
                items(TEASPOON, "Rosemary", "Thyme", "Oregano") +
                items(TABLESPOON, "Dried Fenugreek (Kasuri Methi)"),
        ),
        SeedCategory(
            "dairy", "Dairy & Eggs", 0xFF8E7A55,
            items(PIECE, "Eggs") + items(MILLILITRE, "Milk", "Fresh Cream") + items(CUP, "Yogurt (Dahi)") +
                items(
                    GRAM, "Butter", "Paneer", "Cheddar Cheese", "Mozzarella", "Cream Cheese", "Khoya (Mawa)",
                    "Condensed Milk", "Milk Powder",
                ),
        ),
        SeedCategory(
            "grains", "Grains & Flour", 0xFFA8763E,
            items(
                CUP, "Basmati Rice", "Sella Rice", "Brown Rice", "Wheat Flour (Atta)", "All-purpose Flour (Maida)",
                "Gram Flour (Besan)", "Semolina (Suji)", "Rice Flour", "Oats", "Broken Wheat (Daliya)", "Quinoa",
            ) + items(TABLESPOON, "Cornflour") + items(GRAM, "Vermicelli (Seviyan)", "Pasta", "Spaghetti", "Noodles") +
                items(PIECE, "Bread Slice"),
        ),
        SeedCategory(
            "oils", "Oils & Fats", 0xFFB59A2A,
            items(TABLESPOON, "Cooking Oil", "Olive Oil", "Desi Ghee", "Banaspati Ghee", "Mustard Oil", "Coconut Oil", "Sesame Oil"),
        ),
        SeedCategory(
            "sauces", "Sauces & Condiments", 0xFF7A4A8C,
            items(
                TABLESPOON, "Soy Sauce", "White Vinegar", "Tomato Ketchup", "Chilli Sauce", "Hot Sauce",
                "Mayonnaise", "Mustard Paste", "Tamarind Pulp (Imli)", "Lemon Juice", "Worcestershire Sauce",
                "Oyster Sauce", "BBQ Sauce", "Tomato Paste",
            ),
        ),
        SeedCategory(
            "nuts", "Nuts & Dry Fruits", 0xFF8A5A3B,
            items(
                GRAM, "Almonds", "Cashews", "Pistachios", "Walnuts", "Peanuts", "Raisins", "Dates",
                "Dried Apricots", "Pine Nuts (Chilgoza)", "Desiccated Coconut", "Melon Seeds (Charmagaz)",
                "Fox Nuts (Makhana)",
            ) + items(TABLESPOON, "Sesame Seeds"),
        ),
        SeedCategory(
            "baking", "Baking", 0xFF6B5B95,
            items(TEASPOON, "Baking Powder", "Baking Soda", "Instant Yeast", "Vanilla Essence", "Kewra Water", "Rose Water") +
                items(TABLESPOON, "Cocoa Powder", "Custard Powder", "Gelatin") + items(GRAM, "Chocolate") +
                items(PINCH, "Food Colour"),
        ),
        SeedCategory(
            "sweeteners", "Sweeteners", 0xFFC7823A,
            items(CUP, "Sugar", "Brown Sugar", "Icing Sugar") + items(TABLESPOON, "Honey", "Maple Syrup") + items(GRAM, "Jaggery (Gur)"),
        ),
        SeedCategory(
            "liquids", "Stocks & Liquids", 0xFF3E6E8E,
            items(CUP, "Water", "Chicken Stock", "Beef Stock", "Vegetable Stock", "Coconut Milk"),
        ),
    )
}
