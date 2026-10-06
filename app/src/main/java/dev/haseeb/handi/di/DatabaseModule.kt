package dev.haseeb.handi.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.haseeb.handi.data.local.CatalogDao
import dev.haseeb.handi.data.local.HandiDatabase
import dev.haseeb.handi.data.local.RecipeDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): HandiDatabase =
        Room.databaseBuilder(context, HandiDatabase::class.java, HandiDatabase.NAME)
            .addCallback(HandiDatabase.SeedCallback)
            .build()

    @Provides
    fun provideCatalogDao(db: HandiDatabase): CatalogDao = db.catalogDao()

    @Provides
    fun provideRecipeDao(db: HandiDatabase): RecipeDao = db.recipeDao()
}
