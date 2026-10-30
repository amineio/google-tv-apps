package com.amine.postbox.core

import java.io.File
import java.nio.file.Files
import org.junit.Assert.*
import org.junit.Test

class CaptureStoreTest {
    private fun dir() = Files.createTempDirectory("postbox-test").toFile()

    @Test
    fun reloadAndFlagsPreserveDigest() {
        val d = dir()
        val s = PostboxStore(d)
        val c = CaptureCodecTest.sample()
        val digest = CaptureCodec().digest(c)
        assertEquals(SaveResult.ADDED, s.save(c))
        s.archive(c.id, true)
        s.markSent(c.id)
        val item = PostboxStore(d).list().single()
        assertTrue(item.archived)
        assertTrue(item.sent)
        assertEquals(digest, CaptureCodec().digest(item.capture))
        assertEquals(SaveResult.ALREADY_PRESENT, s.save(c))
        assertTrue(s.list().single().archived)
        s.archive(c.id, false)
        assertFalse(s.list().single().archived)
        s.delete(c.id)
        assertTrue(s.list().isEmpty())
    }

    @Test
    fun conflictingRetryPreservesStoredCapture() {
        val s = PostboxStore(dir())
        val c = CaptureCodecTest.sample()
        s.save(c)
        assertEquals(SaveResult.CONFLICT, s.save(c.copy(text = "changed")))
        assertEquals(c, s.list().single().capture)
    }

    @Test
    fun capacityIncludesArchivedAndSent() {
        val s = PostboxStore(dir())
        repeat(50) {
            val c = CaptureCodecTest.sample()
            assertEquals(SaveResult.ADDED, s.save(c))
            s.archive(c.id, true)
            s.markSent(c.id)
        }
        assertEquals(SaveResult.FULL, s.save(CaptureCodecTest.sample()))
        assertEquals(50, s.list().size)
    }

    @Test
    fun malformedFileIsPreserved() {
        val d = dir()
        val damaged = File(d, "damaged.json")
        damaged.writeText("broken")
        val s = PostboxStore(d)
        assertTrue(s.list().isEmpty())
        assertTrue(damaged.exists())
        assertTrue(s.hasStorageIssue)
    }

    @Test
    fun interruptedReplacementPreservesOldFile() {
        val d = dir()
        val c = CaptureCodecTest.sample()
        PostboxStore(d).save(c)
        val s = PostboxStore(d, beforeCommit = { throw java.io.IOException("failure") })
        assertThrows(java.io.IOException::class.java) { s.archive(c.id, true) }
        assertFalse(PostboxStore(d).list().single().archived)
        assertFalse(d.listFiles()!!.any { it.name.endsWith(".tmp") })
    }

    @Test
    fun editedDraftHasNewIdentity() {
        val s = PostboxStore(dir())
        val old = CaptureCodecTest.sample()
        s.save(old)
        val edited = old.copy(id = java.util.UUID.randomUUID().toString(), text = "Edited")
        s.save(edited)
        assertNotEquals(old.id, edited.id)
        assertEquals(old, s.list().first { it.capture.id == old.id }.capture)
    }

    @Test
    fun totalByteCapacityPreservesExistingFiles() {
        val d = dir()
        File(d, "damaged.json").outputStream().use { out ->
            val block = ByteArray(1024 * 1024)
            repeat(64) { out.write(block) }
        }
        val s = PostboxStore(d)
        assertEquals(SaveResult.FULL, s.save(CaptureCodecTest.sample()))
        assertEquals(64L * 1024 * 1024, File(d, "damaged.json").length())
    }
}
