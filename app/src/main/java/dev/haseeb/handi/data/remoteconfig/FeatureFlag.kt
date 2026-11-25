package dev.haseeb.handi.data.remoteconfig

/**
 * A remotely-togglable feature, with an in-app default that's used whenever Firebase Remote
 * Config hasn't been set up, hasn't fetched yet, or is unreachable (see [RemoteConfigRepository]
 * — this app must keep working fully offline, so the default is never optional).
 *
 * To add a new flag: add an entry here, then read it via
 * `remoteConfigRepository.isEnabled(FeatureFlag.YOUR_FLAG)` at the call site that should be
 * gated. Nothing else needs to know a flag exists.
 */
enum class FeatureFlag(val key: String, val defaultValue: Boolean) {
    /** Gates the "Share as PDF" action on `DetailScreen` (see issue #5). */
    PDF_EXPORT_ENABLED("pdf_export_enabled", true),

    /** Gates the "Backup & Restore" row on `SettingsScreen` (see issue #2). */
    BACKUP_RESTORE_ENABLED("backup_restore_enabled", true),
    ;

    companion object {
        /** The map `FirebaseRemoteConfig.setDefaultsAsync(Map)` expects. */
        fun defaults(): Map<String, Any> = entries.associate { it.key to it.defaultValue }
    }
}
