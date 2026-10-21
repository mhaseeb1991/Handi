package dev.haseeb.handi.data.remoteconfig

import android.content.Context
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.haseeb.handi.BuildConfig
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Wraps Firebase Remote Config so the rest of the app only ever sees
 * [FeatureFlag.defaultValue] (works fully offline, no Firebase project required) or a value
 * Remote Config overrides it with after a successful fetch. Never throws, never blocks app
 * startup on a network call.
 *
 * Works with **or** without a Firebase project configured — see `app/build.gradle.kts`'s
 * conditional plugin application and `docs/DATA_LAYER.md` for how to add your own
 * `google-services.json`. Without one, [remoteConfig] stays `null` and every flag reads its
 * compiled-in default directly. **With** one but before the first successful fetch (or before a
 * given key has ever been created as a Remote Config parameter), [isEnabled] still returns the
 * compiled-in default — see the `VALUE_SOURCE_STATIC` check there, and
 * `RemoteConfigRepositoryTest` for the regression test.
 */
@Singleton
class RemoteConfigRepository @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val remoteConfig: FirebaseRemoteConfig? = runCatching {
        if (FirebaseApp.getApps(context).isEmpty()) return@runCatching null
        FirebaseRemoteConfig.getInstance().apply {
            setConfigSettingsAsync(
                FirebaseRemoteConfigSettings.Builder()
                    // Debug builds fetch every time (no client-side caching to fight while
                    // testing a flag change); release keeps a real interval so we don't hammer
                    // Firebase's servers on every cold start.
                    .setMinimumFetchIntervalInSeconds(if (BuildConfig.DEBUG) 0L else RELEASE_FETCH_INTERVAL_SECONDS)
                    .build(),
            )
            setDefaultsAsync(FeatureFlag.defaults())
        }
    }.getOrNull()

    /**
     * Fetches and activates the latest values. Safe to call from app start — never throws. Logs
     * (doesn't swallow silently) on failure, since "flag didn't update" is otherwise impossible
     * to diagnose from outside the app; callers still don't need to handle failure themselves,
     * only the eventual (possibly unchanged) flag values.
     */
    suspend fun refresh() {
        val config = remoteConfig ?: return
        runCatching { config.fetchAndActivate().await() }
            .onFailure { Log.w(TAG, "fetchAndActivate() failed; keeping previously active values", it) }
    }

    fun isEnabled(flag: FeatureFlag): Boolean {
        val config = remoteConfig ?: return flag.defaultValue
        return runCatching {
            val value = config.getValue(flag.key)
            // getBoolean()/getValue() never throw for a missing key — they silently return the
            // SDK's own built-in static fallback (false) instead. STATIC means neither our
            // setDefaultsAsync() value nor a fetched one has taken effect yet (e.g. a race with
            // that async call right after construction, or no Remote Config parameter created
            // yet) — in that case trust our compiled default, not the SDK's bare "false".
            if (value.source == FirebaseRemoteConfig.VALUE_SOURCE_STATIC) flag.defaultValue else value.asBoolean()
        }.onFailure { Log.w(TAG, "isEnabled(${flag.key}) failed; using the compiled default", it) }
            .getOrDefault(flag.defaultValue)
    }

    private companion object {
        const val TAG = "RemoteConfigRepository"
        const val RELEASE_FETCH_INTERVAL_SECONDS = 3600L
    }
}

/**
 * Minimal suspend bridge for a GMS [Task], so this one call site doesn't need the
 * `kotlinx-coroutines-play-services` dependency just for `Task.await()`.
 */
private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
    addOnCompleteListener { task ->
        val exception = task.exception
        if (exception != null) cont.resumeWithException(exception) else cont.resume(task.result)
    }
}
