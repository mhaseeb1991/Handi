package dev.haseeb.handi.data.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.haseeb.handi.data.common.await
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.security.SecureRandom
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Google sign-in via Firebase Auth, using Credential Manager (the current recommended API —
 * not the deprecated `GoogleSignInClient`). Part of the Phase 2 "cloud foundation" (issue #6):
 * an account/auth concept that the app works completely without, same philosophy as
 * [dev.haseeb.handi.data.remoteconfig.RemoteConfigRepository] — no existing local-only flow
 * (recipes, editor, backup/restore) depends on being signed in.
 *
 * **Needs one piece of setup this repo can't do for you**: Google sign-in has to be enabled as
 * a provider in the Firebase console (Authentication → Sign-in method → Google), which
 * generates a web OAuth client and adds it to a re-downloaded `google-services.json`. Until
 * that's done, [isGoogleSignInAvailable] is `false` and the sign-in UI hides itself — see
 * `docs/DATA_LAYER.md`.
 */
@Singleton
class AuthRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val auth: FirebaseAuth? = runCatching { FirebaseAuth.getInstance() }.getOrNull()

    /**
     * The web client id the `google-services` plugin generates as `R.string.default_web_client_id`
     * — but only when a Google OAuth client exists in `google-services.json`. Looked up by name
     * (not a direct `R.string` reference) because that resource may not exist at all yet, and a
     * missing-resource compile error would be a much worse failure mode than "feature hidden."
     */
    @Suppress("DiscouragedApi") // deliberate: see the doc comment above — the resource may not exist yet
    private val webClientId: String? by lazy {
        val id = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        if (id == 0) null else context.getString(id)
    }

    val isGoogleSignInAvailable: Boolean get() = auth != null && webClientId != null

    /** Emits the current user (or `null`) immediately, then again on every sign-in/out. */
    val currentUser: Flow<User?> = callbackFlow {
        val firebaseAuth = auth
        if (firebaseAuth == null) {
            trySend(null)
            awaitClose { }
        } else {
            val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser.toUserOrNull()) }
            firebaseAuth.addAuthStateListener(listener)
            awaitClose { firebaseAuth.removeAuthStateListener(listener) }
        }
    }

    /**
     * [activityContext] must be (or wrap) an `Activity` — Credential Manager needs it to show
     * the account picker UI, so this can't run against the `@ApplicationContext` this repository
     * otherwise holds. Pass `LocalContext.current` from the composable that triggers sign-in.
     */
    suspend fun signInWithGoogle(activityContext: Context): Result<User> = runCatching {
        val firebaseAuth = auth ?: error("Firebase Auth isn't configured for this build.")
        val clientId = webClientId ?: error("Google sign-in isn't enabled for this Firebase project yet.")

        // GetSignInWithGoogleOption (not the broader GetGoogleIdOption) is the API Google's own
        // lint rule (CredentialManagerSignInWithGoogle) recommends for this exact "Sign in with
        // Google" button flow, rather than a generic "any Google ID" picker.
        val option = GetSignInWithGoogleOption.Builder(clientId)
            .setNonce(randomNonce(SecureRandom()))
            .build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()

        val response = CredentialManager.create(activityContext).getCredential(activityContext, request)
        val credential = response.credential
        // GetSignInWithGoogleOption responses come back tagged TYPE_GOOGLE_ID_TOKEN_SIWG_CREDENTIAL,
        // not the plain TYPE_GOOGLE_ID_TOKEN_CREDENTIAL GetGoogleIdOption uses — checking only the
        // latter would mean this branch is simply never taken. Accept either, since both decode
        // the same way via GoogleIdTokenCredential.createFrom().
        val googleIdToken = (credential as? CustomCredential)
            ?.takeIf {
                it.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL ||
                    it.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_SIWG_CREDENTIAL
            }
            ?.let { GoogleIdTokenCredential.createFrom(it.data) }
            ?: error("Credential Manager returned an unexpected credential type.")

        val firebaseCredential = GoogleAuthProvider.getCredential(googleIdToken.idToken, null)
        val result = firebaseAuth.signInWithCredential(firebaseCredential).await()
        result.user.toUserOrNull() ?: error("Signed in, but Firebase returned no user.")
    }

    fun signOut() {
        auth?.signOut()
    }
}

private fun FirebaseUser?.toUserOrNull(): User? = this?.let {
    User(uid = it.uid, displayName = it.displayName, email = it.email, photoUrl = it.photoUrl?.toString())
}

/**
 * A fresh, URL-safe random nonce for each Google sign-in attempt — Credential Manager/Firebase
 * use this to tie the returned ID token to this specific request (replay protection). Exposed
 * as a plain function (not buried in [AuthRepository]) so its shape is unit-testable without
 * Android.
 */
internal fun randomNonce(random: SecureRandom): String {
    val bytes = ByteArray(NONCE_BYTES)
    random.nextBytes(bytes)
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
}

private const val NONCE_BYTES = 16
