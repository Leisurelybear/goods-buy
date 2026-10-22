package com.goodsbuy.app.util

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Security tests for the ZIP extraction path: a backup file is untrusted input,
 * so it must not be able to write outside the temp directory or exhaust the
 * device.
 */
class BackupManagerSecurityTest {

    private val tempDir: File = Files.createTempDirectory("backup-extract").toFile()

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun `extracts a well formed backup`() {
        val zip = zipOf(
            "manifest.json" to """{"version":1}""".toByteArray(),
            "images/a.jpg" to "image-bytes".toByteArray()
        )

        extract(zip)

        assertEquals("""{"version":1}""", File(tempDir, "manifest.json").readText())
        assertEquals("image-bytes", File(tempDir, "images/a.jpg").readText())
    }

    @Test
    fun `accepts a directory entry for images`() {
        val zip = zipOf(
            "images/" to ByteArray(0),
            "images/a.jpg" to "x".toByteArray()
        )

        extract(zip)

        assertTrue(File(tempDir, "images/a.jpg").exists())
    }

    @Test
    fun `rejects parent directory traversal at the root`() {
        assertRejected(zipOf("../evil.txt" to "x".toByteArray()))
    }

    @Test
    fun `rejects parent directory traversal inside images`() {
        assertRejected(zipOf("images/../evil.txt" to "x".toByteArray()))
    }

    @Test
    fun `rejects absolute paths`() {
        assertRejected(zipOf("/tmp/evil.txt" to "x".toByteArray()))
    }

    @Test
    fun `rejects windows drive paths`() {
        assertRejected(zipOf("C:/evil.txt" to "x".toByteArray()))
    }

    @Test
    fun `rejects paths outside manifest and images`() {
        assertRejected(zipOf("other/a.txt" to "x".toByteArray()))
    }

    @Test
    fun `rejects nested paths under images`() {
        assertRejected(zipOf("images/sub/a.jpg" to "x".toByteArray()))
    }

    @Test
    fun `rejects entries that collide after path normalisation`() {
        // ZipOutputStream refuses literal duplicates, so the realistic shape of
        // this attack is a separator-confusion pair that normalises to one path.
        assertRejected(
            zipOf(
                "images/a.jpg" to "x".toByteArray(),
                "images\\a.jpg" to "y".toByteArray()
            )
        )
    }

    @Test
    fun `rejects a backup with too many entries`() {
        val entries = (0..3).map { "images/f$it.jpg" to "x".toByteArray() }
        assertRejected(zipOf(*entries.toTypedArray()), ZipLimits(maxEntries = 3))
    }

    @Test
    fun `rejects an entry larger than the per-file limit`() {
        assertRejected(zipOf("images/big.jpg" to ByteArray(17)), ZipLimits(maxEntryBytes = 16))
    }

    @Test
    fun `rejects a backup whose total size exceeds the limit`() {
        assertRejected(
            zipOf(
                "images/a.jpg" to ByteArray(16),
                "images/b.jpg" to ByteArray(16)
            ),
            ZipLimits(maxTotalBytes = 24)
        )
    }

    @Test
    fun `rejects an absurd compression ratio`() {
        // 64 KB of zeros deflates to a few hundred bytes: the classic zip-bomb shape.
        assertRejected(
            zipOf("images/bomb.jpg" to ByteArray(64 * 1024)),
            ZipLimits(maxCompressionRatio = 10)
        )
    }

    @Test
    fun `default limits stay above what our own exporter can produce`() {
        // A user's own backup must be restorable, so the import caps cannot be
        // tightened below the scale a real library reaches.
        val limits = ZipLimits()
        assertTrue("entry cap must fit a large library", limits.maxEntries >= 10_000)
        assertTrue("per-entry cap must fit a full-size photo", limits.maxEntryBytes >= 20L * 1024 * 1024)
        assertTrue("total cap must fit a large library", limits.maxTotalBytes >= 200L * 1024 * 1024)
    }

    @Test
    fun `isAllowedEntry accepts only manifest and flat image paths`() {
        assertTrue(BackupManager.isAllowedEntry("manifest.json"))
        assertTrue(BackupManager.isAllowedEntry("images/abc.jpg"))

        assertFalse(BackupManager.isAllowedEntry(""))
        assertFalse(BackupManager.isAllowedEntry("images/"))
        assertFalse(BackupManager.isAllowedEntry("images/."))
        assertFalse(BackupManager.isAllowedEntry("images/.."))
        assertFalse(BackupManager.isAllowedEntry("images/sub/abc.jpg"))
        assertFalse(BackupManager.isAllowedEntry("manifest.json.bak"))
        assertFalse(BackupManager.isAllowedEntry("images\\abc.jpg"))
        assertFalse(BackupManager.isAllowedEntry("images/a\u0000b.jpg"))
    }

    private fun extract(zip: ByteArray, limits: ZipLimits = ZipLimits()) {
        ByteArrayInputStream(zip).use { BackupManager.extractZip(it, tempDir, limits) }
    }

    private fun assertRejected(zip: ByteArray, limits: ZipLimits = ZipLimits()) {
        try {
            extract(zip, limits)
            throw AssertionError("Expected the backup to be rejected")
        } catch (expected: IOException) {
            // expected
        }
    }

    private fun zipOf(vararg entries: Pair<String, ByteArray>): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zos ->
            entries.forEach { (name, bytes) ->
                zos.putNextEntry(ZipEntry(name))
                zos.write(bytes)
                zos.closeEntry()
            }
        }
        return out.toByteArray()
    }
}
