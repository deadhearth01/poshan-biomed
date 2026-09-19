package com.teamposhan.biomed.data.local

import android.content.Context
import android.util.Log
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream

/**
 * Offline-first JSON persistence, with the durability an offline-first health record needs.
 *
 * Everything the app collects is written here first and only later mirrored to the cloud, so
 * this file **is** the record — not a cache of one. That raises the bar on two failure modes
 * the previous implementation got wrong:
 *
 * ### 1. Replacing the file was not atomic
 *
 * It did `delete()` then `renameTo()`. A crash or power loss in the gap between those two
 * left **neither** file on disk, destroying the record it was in the middle of updating. The
 * order is now: write the temp file, move the current file aside as a backup, then rename
 * the temp into place. Every intermediate state is recoverable.
 *
 * ### 2. A corrupt file silently became an empty one
 *
 * Any parse failure fell through to `fallback()` — an empty list. The app would then show no
 * children, and the next write would persist that emptiness, turning a recoverable read
 * error into permanent data loss. Reads now try the backup before giving up, and a failure
 * to read anything is logged rather than passing silently.
 *
 * Writes are also flushed to disk with `fsync` before the rename. Without it the rename can
 * reach the disk before the bytes do, which on an unexpected power loss leaves a file that
 * exists and is empty — the worst of both outcomes.
 */
class JsonFileStore<T>(
    context: Context,
    private val fileName: String,
    private val serializer: KSerializer<T>,
    private val fallback: () -> T,
) {
    private val file = File(context.filesDir, fileName)
    private val backup = File(context.filesDir, "$fileName.bak")
    private val temp = File(context.filesDir, "$fileName.tmp")

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = false
    }

    @Synchronized
    fun read(): T {
        decode(file)?.let { return it }

        // The live file is missing or unreadable. The backup is the previous good write, so
        // losing one update is far better than losing the record.
        decode(backup)?.let {
            Log.w(TAG, "$fileName unreadable; recovered from backup")
            // Put the recovered copy back so the next write has something to back up.
            runCatching { backup.copyTo(file, overwrite = true) }
            return it
        }

        if (file.exists() || backup.exists()) {
            // Both present and both unreadable is worth shouting about: returning empty here
            // is what previously led to the emptiness being written back permanently.
            Log.e(TAG, "$fileName and its backup are both unreadable; starting empty")
        }
        return fallback()
    }

    private fun decode(f: File): T? {
        if (!f.exists() || f.length() == 0L) return null
        return runCatching { json.decodeFromString(serializer, f.readText()) }.getOrNull()
    }

    @Synchronized
    fun write(value: T) {
        val text = runCatching { json.encodeToString(serializer, value) }.getOrElse {
            Log.e(TAG, "$fileName could not be serialised; keeping the previous file", it)
            return
        }

        // 1. Write the new content to a temp file and force it to disk.
        runCatching {
            FileOutputStream(temp).use { out ->
                out.write(text.toByteArray())
                out.flush()
                out.fd.sync()
            }
        }.onFailure {
            Log.e(TAG, "$fileName temp write failed; keeping the previous file", it)
            runCatching { temp.delete() }
            return
        }

        // 2. Move the current file aside. If step 3 fails, read() finds this.
        if (file.exists()) {
            runCatching {
                if (backup.exists()) backup.delete()
                file.renameTo(backup)
            }
        }

        // 3. Put the new file in place.
        if (!temp.renameTo(file)) {
            // Rename failed — fall back to copying, and if that fails too, restore the
            // backup rather than leaving no file at all.
            val copied = runCatching { temp.copyTo(file, overwrite = true) }.isSuccess
            if (!copied) {
                Log.e(TAG, "$fileName could not be replaced; restoring backup")
                runCatching { backup.renameTo(file) }
                return
            }
            runCatching { temp.delete() }
        }
    }

    private companion object {
        const val TAG = "JsonFileStore"
    }
}
