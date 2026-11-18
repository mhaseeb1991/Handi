package dev.haseeb.handi.di

import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Only provides [FirebaseFirestore] — Hilt builds this (and whatever it's constructed by)
 * lazily, only when something actually injects it. Today that's
 * [dev.haseeb.handi.data.remote.RemoteRecipeDataSource], which nothing else depends on yet
 * (see its doc comment), so this module is effectively dormant until Phase 3 wires it up.
 */
@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    @Provides
    @Singleton
    fun provideFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()
}
