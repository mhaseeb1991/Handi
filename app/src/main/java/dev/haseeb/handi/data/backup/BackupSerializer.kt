package dev.haseeb.handi.data.backup

import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** JSON <-> [BackupFile] conversion, isolated so it has no Android dependency and is unit-testable. */
internal object BackupSerializer {

    private val json = Json {
        ignoreUnknownKeys = true // a newer export opened on an older build of the app shouldn't crash
        prettyPrint = false
        encodeDefaults = true
    }

    fun encode(file: BackupFile): String = json.encodeToString(file)

    /** @throws BackupFormatException if [text] isn't valid backup JSON. */
    fun decode(text: String): BackupFile = try {
        json.decodeFromString<BackupFile>(text)
    } catch (e: SerializationException) {
        throw BackupFormatException("backup.json is not a valid Handi backup", e)
    } catch (e: IllegalArgumentException) {
        throw BackupFormatException("backup.json is not a valid Handi backup", e)
    }
}

/** Thrown when a file picked for import isn't a (valid, readable) Handi backup archive. */
class BackupFormatException(message: String, cause: Throwable? = null) : Exception(message, cause)
