package com.amine.postbox.core

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.ConcurrentHashMap
import kotlinx.serialization.encodeToString

class PostboxStore
internal constructor(
    private val directory: File,
    private val beforeCommit: () -> Unit,
    private val syncDirectory: (File) -> Unit = ::forceDirectory,
) {
    constructor(directory: File) : this(directory, {})

    private val codec = CaptureCodec()
    private val lock = locks.computeIfAbsent(directory.canonicalPath) { DirectoryState() }
    @Volatile
    var hasStorageIssue: Boolean = false
        private set

    fun list(): List<StoredCapture> =
        synchronized(lock) {
            entries().sortedByDescending { it.capture.capturedAtEpochMs }
        }

    fun save(capture: PostCapture): SaveResult =
        synchronized(lock) {
            val canonical = codec.canonicalBytes(capture)
            val normalized = codec.decode(canonical)
            val entries = entries()
            entries
                .firstOrNull { it.capture.id == normalized.id }
                ?.let {
                    return@synchronized if (codec.digest(it.capture) == codec.digest(normalized)) {
                        synchronizeDirectory()
                        SaveResult.ALREADY_PRESENT
                    } else SaveResult.CONFLICT
                }
            // Never replace an unreadable entry whose UUID matches this request.
            if (File(directory, "${normalized.id}.json").exists())
                throw IOException("Local storage unavailable.")
            val bytes = encode(StoredCapture(normalized))
            if (
                files().size >= 50 || files().sumOf { it.length() } + bytes.size > 64L * 1024 * 1024
            )
                return@synchronized SaveResult.FULL
            write(normalized.id, bytes)
            SaveResult.ADDED
        }

    fun archive(id: String, archived: Boolean) = update(id) { it.copy(archived = archived) }

    fun markSent(id: String) = update(id) { it.copy(sent = true) }

    fun delete(id: String) {
        synchronized(lock) {
            validateId(id)
            val file = File(directory, "$id.json")
            if (file.exists() && !file.delete()) throw IOException("Local storage unavailable.")
            if (directory.isDirectory) synchronizeDirectory()
        }
    }

    private fun update(id: String, transform: (StoredCapture) -> StoredCapture) {
        synchronized(lock) {
            validateId(id)
            val old =
                entries().firstOrNull { it.capture.id == id }
                    ?: throw IOException("Capture unavailable.")
            val bytes = encode(transform(old))
            if (
                files().sumOf { it.length() } - File(directory, "$id.json").length() + bytes.size >
                    64L * 1024 * 1024
            )
                throw IOException("Local storage full.")
            write(id, bytes)
        }
    }

    private fun entries(): List<StoredCapture> {
        ensureDirectory()
        hasStorageIssue = false
        return files().mapNotNull { file ->
            try {
                require(file.length() <= MAX_CAPTURE_BYTES + 256L)
                val item =
                    postboxJson.decodeFromString<StoredCapture>(
                        Charsets.UTF_8.newDecoder()
                            .decode(java.nio.ByteBuffer.wrap(file.readBytes()))
                            .toString()
                    )
                val capture = codec.decode(codec.canonicalBytes(item.capture))
                require(file.name == "${capture.id}.json")
                item.copy(capture = capture)
            } catch (_: Exception) {
                hasStorageIssue = true
                null
            }
        }
    }

    private fun files(): List<File> =
        directory.listFiles()?.filter { it.isFile && it.name.endsWith(".json") }
            ?: if (!directory.exists()) emptyList()
            else throw IOException("Local storage unavailable.")

    private fun ensureDirectory() {
        if (!directory.isDirectory) {
            val missing = mutableListOf<File>()
            var current: File? = directory.absoluteFile
            while (current != null && !current.exists()) {
                missing += current
                current = current.parentFile
            }
            if (current == null || !current.isDirectory)
                throw IOException("Local storage unavailable.")
            // Stop at the first existing private directory. Its own parent is outside this
            // creation.
            lock.pendingParents.addAll(
                missing.asReversed().map {
                    it.parentFile ?: throw IOException("Local storage unavailable.")
                }
            )
            if (!directory.mkdirs() && !directory.isDirectory)
                throw IOException("Local storage unavailable.")
        }
        val parents = lock.pendingParents.iterator()
        while (parents.hasNext()) {
            synchronizeDirectory(parents.next())
            // Keep this parent pending if synchronization throws, including across store instances.
            parents.remove()
        }
    }

    private fun encode(item: StoredCapture) =
        postboxJson.encodeToString(item).toByteArray(Charsets.UTF_8)

    private fun write(id: String, bytes: ByteArray) {
        ensureDirectory()
        val temp = File.createTempFile("capture-", ".tmp", directory)
        try {
            FileOutputStream(temp).use { out ->
                out.write(bytes)
                out.flush()
                out.fd.sync()
            }
            beforeCommit()
            Files.move(
                temp.toPath(),
                File(directory, "$id.json").toPath(),
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING,
            )
            synchronizeDirectory()
        } catch (_: Exception) {
            throw IOException("Local storage unavailable.")
        } finally {
            temp.delete()
        }
    }

    private fun synchronizeDirectory(target: File = directory) {
        try {
            syncDirectory(target)
        } catch (_: Exception) {
            throw IOException("Local storage unavailable.")
        }
    }

    private fun validateId(id: String) {
        require(id.matches(Regex("[0-9a-fA-F-]{36}"))) { "Invalid capture." }
    }

    companion object {
        private val locks = ConcurrentHashMap<String, DirectoryState>()
    }
}

private class DirectoryState {
    val pendingParents = linkedSetOf<File>()
}

private fun forceDirectory(directory: File) {
    java.nio.channels.FileChannel.open(directory.toPath(), java.nio.file.StandardOpenOption.READ)
        .use { channel ->
            channel.force(true)
        }
}
