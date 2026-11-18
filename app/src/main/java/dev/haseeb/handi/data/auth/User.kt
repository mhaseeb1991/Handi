package dev.haseeb.handi.data.auth

/** The signed-in Handi account, if any. Mirrors the subset of `FirebaseUser` the UI needs. */
data class User(
    val uid: String,
    val displayName: String?,
    val email: String?,
    val photoUrl: String?,
)
