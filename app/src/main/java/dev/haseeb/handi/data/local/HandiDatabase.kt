package dev.haseeb.handi.data.local

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        CategoryEntity::class,
        IngredientEntity::class,
        RecipeEntity::class,
        RecipeIngredientEntity::class,
        RecipeStepEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class HandiDatabase : RoomDatabase() {
    abstract fun catalogDao(): CatalogDao
    abstract fun recipeDao(): RecipeDao

    /** Seeds the prefilled category & ingredient catalogue once, when the DB file is created. */
    object SeedCallback : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            db.beginTransaction()
            try {
                IngredientCatalog.categories.forEachIndexed { index, category ->
                    db.insert(
                        "categories",
                        SQLiteDatabase.CONFLICT_IGNORE,
                        ContentValues().apply {
                            put("id", category.id)
                            put("name", category.name)
                            put("position", index)
                            put("tint", category.tint)
                        },
                    )
                    category.items.forEach { (name, unit) ->
                        db.insert(
                            "ingredients",
                            SQLiteDatabase.CONFLICT_IGNORE,
                            ContentValues().apply {
                                put("categoryId", category.id)
                                put("name", name)
                                put("defaultUnit", unit.name)
                                put("isCustom", 0)
                            },
                        )
                    }
                }
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        }
    }

    companion object {
        const val NAME = "handi.db"
    }
}
