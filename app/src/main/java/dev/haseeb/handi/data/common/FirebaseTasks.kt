package dev.haseeb.handi.data.common

import com.google.android.gms.tasks.Task
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Minimal suspend bridge for a GMS [Task], shared by every Firebase-coupled repository
 * (`RemoteConfigRepository`, `AuthRepository`) so none of them need the
 * `kotlinx-coroutines-play-services` dependency just for `Task.await()`.
 */
internal suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
    addOnCompleteListener { task ->
        val exception = task.exception
        if (exception != null) cont.resumeWithException(exception) else cont.resume(task.result)
    }
}
