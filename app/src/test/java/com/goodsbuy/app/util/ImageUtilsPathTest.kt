package com.goodsbuy.app.util

import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

class ImageUtilsPathTest {

    private lateinit var tmpDir: File

    @Before
    fun setUp() {
        tmpDir = Files.createTempDirectory("goodsbuy").toFile()
        // AppLogger writes through android.util.Log, which is not available in JVM tests.
        mockkObject(AppLogger)
        every { AppLogger.e(any(), any(), any()) } returns Unit
        every { AppLogger.e(any(), any()) } returns Unit
        every { AppLogger.i(any(), any()) } returns Unit
    }

    @After
    fun tearDown() {
        unmockkAll()
        tmpDir.deleteRecursively()
    }

    @Test
    fun `baseOfImage strips transparent png suffix`() {
        assertEquals("/data/a/abc", ImageUtils.baseOfImage("/data/a/abc_transparent.png"))
    }

    @Test
    fun `baseOfImage strips orig suffix`() {
        assertEquals("/data/a/abc", ImageUtils.baseOfImage("/data/a/abc_orig.jpg"))
    }

    @Test
    fun `baseOfImage strips jpg extension`() {
        assertEquals("/data/a/abc", ImageUtils.baseOfImage("/data/a/abc.jpg"))
    }

    @Test
    fun `baseOfImage strips thumbnail suffixes`() {
        assertEquals("/data/a/abc", ImageUtils.baseOfImage("/data/a/abc_thumb.jpg"))
        assertEquals("/data/a/abc", ImageUtils.baseOfImage("/data/a/abc_thumb.png"))
    }

    @Test
    fun `thumbnailPathFor prefers the generated thumbnail`() {
        val base = ImageUtils.baseOfImage(File(tmpDir, "abc.jpg").absolutePath)
        val original = ImageUtils.originalJpgPath(base)
        val thumb = "${base}_thumb.jpg"
        File(original).writeText("orig")
        File(thumb).writeText("thumb")

        assertEquals(thumb, ImageUtils.thumbnailPathFor(original))
    }

    @Test
    fun `thumbnailPathFor falls back to the original when no thumbnail exists`() {
        val base = ImageUtils.baseOfImage(File(tmpDir, "abc.jpg").absolutePath)
        val original = ImageUtils.originalJpgPath(base)
        File(original).writeText("orig")

        assertEquals(original, ImageUtils.thumbnailPathFor(original))
    }

    @Test
    fun `thumbnailPathFor resolves the thumbnail of an edited png`() {
        val base = ImageUtils.baseOfImage(File(tmpDir, "abc.jpg").absolutePath)
        val edited = ImageUtils.transparentPngPath(base)
        val thumb = "${base}_thumb.png"
        File(edited).writeText("png")
        File(thumb).writeText("thumb")

        assertEquals(thumb, ImageUtils.thumbnailPathFor(edited))
    }

    @Test
    fun `derived paths use consistent base`() {
        val base = ImageUtils.baseOfImage("/x/abc.jpg")
        assertEquals("/x/abc.jpg", ImageUtils.originalJpgPath(base))
        assertEquals("/x/abc_orig.jpg", ImageUtils.origBackupPath(base))
        assertEquals("/x/abc_transparent.png", ImageUtils.transparentPngPath(base))
    }

    @Test
    fun `isEditedImage detects transparent png only`() {
        assertTrue(ImageUtils.isEditedImage("/x/a_transparent.png"))
        assertFalse(ImageUtils.isEditedImage("/x/a.jpg"))
    }

    @Test
    fun `sampleSizeFor caps the longer edge at maxDimension`() {
        assertEquals(2, ImageUtils.sampleSizeFor(4032, 3024, 2048))
        assertEquals(8, ImageUtils.sampleSizeFor(4032, 3024, 640))
        assertEquals(4, ImageUtils.sampleSizeFor(5000, 2000, 2048))
        assertEquals(1, ImageUtils.sampleSizeFor(640, 480, 2048))
    }

    @Test
    fun `sampleSizeFor degrades safely on invalid dimensions`() {
        assertEquals(1, ImageUtils.sampleSizeFor(0, 100, 2048))
        assertEquals(1, ImageUtils.sampleSizeFor(100, 0, 2048))
        assertEquals(1, ImageUtils.sampleSizeFor(100, 100, 0))
    }

    @Test
    fun `deleteImageWithCompanions removes all derived files`() {
        val base = ImageUtils.baseOfImage(File(tmpDir, "abc.jpg").absolutePath)
        val paths = listOf(
            ImageUtils.originalJpgPath(base),
            ImageUtils.origBackupPath(base),
            ImageUtils.transparentPngPath(base),
            "${base}_thumb.jpg",
            "${base}_thumb.png"
        )
        paths.forEach { File(it).writeText("x") }

        ImageUtils.deleteImageWithCompanions(ImageUtils.originalJpgPath(base))

        paths.forEach { assertFalse(File(it).exists()) }
    }

    @Test
    fun `deleteImage removes only the exact file`() {
        val base = ImageUtils.baseOfImage(File(tmpDir, "abc.jpg").absolutePath)
        val orig = ImageUtils.originalJpgPath(base)
        val backup = ImageUtils.origBackupPath(base)
        File(orig).writeText("x")
        File(backup).writeText("x")

        ImageUtils.deleteImage(orig)

        assertFalse(File(orig).exists())
        assertTrue(File(backup).exists())
    }

    @Test
    fun `resetEdgeFade removes derived files and restores original when present`() {
        val base = ImageUtils.baseOfImage(File(tmpDir, "abc.jpg").absolutePath)
        val original = ImageUtils.originalJpgPath(base)
        val orig = ImageUtils.origBackupPath(base)
        val png = ImageUtils.transparentPngPath(base)
        File(original).writeText("jpg")
        File(orig).writeText("orig")
        File(png).writeText("png")

        val result = ImageUtils.resetEdgeFade(png)

        assertEquals(original, result)
        assertTrue(File(original).exists())
        assertFalse(File(orig).exists())
        assertFalse(File(png).exists())
    }

    @Test
    fun `resetEdgeFade falls back to orig backup when original jpg was deleted on save`() {
        val base = ImageUtils.baseOfImage(File(tmpDir, "abc.jpg").absolutePath)
        val orig = ImageUtils.origBackupPath(base)
        val png = ImageUtils.transparentPngPath(base)
        File(orig).writeText("orig")
        File(png).writeText("png")

        val result = ImageUtils.resetEdgeFade(png)

        assertEquals(orig, result)
        assertTrue(File(orig).exists())
        assertFalse(File(png).exists())
    }

    @Test
    fun `resetEdgeFade keeps the only copy when backup was restored without originals`() {
        val base = ImageUtils.baseOfImage(File(tmpDir, "abc.jpg").absolutePath)
        val png = ImageUtils.transparentPngPath(base)
        File(png).writeText("png")

        val result = ImageUtils.resetEdgeFade(png)

        assertEquals(png, result)
        assertTrue(File(png).exists())
    }

    @Test
    fun `deleteUnreferencedImages keeps persisted base when draft holds derived png`() {
        val base = ImageUtils.baseOfImage(File(tmpDir, "abc.jpg").absolutePath)
        val original = ImageUtils.originalJpgPath(base)
        val orig = ImageUtils.origBackupPath(base)
        val png = ImageUtils.transparentPngPath(base)
        File(original).writeText("jpg")
        File(orig).writeText("orig")
        File(png).writeText("png")

        ImageUtils.deleteUnreferencedImages(listOf(png), listOf(original))

        assertTrue(File(original).exists())
        assertFalse(File(orig).exists())
        assertFalse(File(png).exists())
    }

    @Test
    fun `deleteUnreferencedImages keeps exact persisted matches`() {
        val base = ImageUtils.baseOfImage(File(tmpDir, "abc.jpg").absolutePath)
        val original = ImageUtils.originalJpgPath(base)
        File(original).writeText("jpg")

        ImageUtils.deleteUnreferencedImages(listOf(original), listOf(original))

        assertTrue(File(original).exists())
    }

    @Test
    fun `deleteUnreferencedImages removes everything for unpersisted paths`() {
        val base = ImageUtils.baseOfImage(File(tmpDir, "abc.jpg").absolutePath)
        val original = ImageUtils.originalJpgPath(base)
        val orig = ImageUtils.origBackupPath(base)
        val png = ImageUtils.transparentPngPath(base)
        File(original).writeText("jpg")
        File(orig).writeText("orig")
        File(png).writeText("png")

        ImageUtils.deleteUnreferencedImages(listOf(png), emptyList())

        assertFalse(File(original).exists())
        assertFalse(File(orig).exists())
        assertFalse(File(png).exists())
    }

    @Test
    fun `deleteUnreferencedImages keeps persisted transparent png of same base`() {
        val base = ImageUtils.baseOfImage(File(tmpDir, "abc.jpg").absolutePath)
        val png = ImageUtils.transparentPngPath(base)
        File(png).writeText("png")

        ImageUtils.deleteUnreferencedImages(listOf(png), listOf(png))

        assertTrue(File(png).exists())
    }
}