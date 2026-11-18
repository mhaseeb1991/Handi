package dev.haseeb.handi.data.common

import com.google.android.gms.tasks.Task
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Minimal suspend bridge for a GMS [Task], shared by every Firebase-coupled repository
 * (`RemoteConfigRepository`, `AuthRepository`, `RemoteRecipeDataSource`, `SharedRecipeDataSource`)
 * so none of them need the `kotlinx-coroutines-play-services` dependency just for `Task.await()`.
 *
 * Wrapped in [withTimeout]: confirmed live (issue #8's testing) that a Firestore write against a
 * project where the Firestore API isn't enabled yet never completes its [Task] at all -- the SDK
 * retries the underlying stream forever rather than failing it, so without a timeout this would
 * hang any "syncing..."/"sharing..." UI indefinitely with no error ever surfacing.
 */
internal suspend fun <T> Task<T>.await(timeoutMillis: Long = 20_000): T = withTimeout(timeoutMillis) {
    suspendCancellableCoroutine { cont ->
        addOnCompleteListener { task ->
            val exception = task.exception
            if (exception != null) cont.resumeWithException(exception) else cont.resume(task.result)
        }
    }
}
