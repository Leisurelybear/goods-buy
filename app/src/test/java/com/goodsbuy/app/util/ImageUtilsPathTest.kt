package com.goodsbuy.app.util

import android.content.Context
import io.mockk.mockk
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
    }

    @After
    fun tearDown() {
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
    fun `deleteImageWithCompanions removes all derived files`() {
        val base = ImageUtils.baseOfImage(File(tmpDir, "abc.jpg").absolutePath)
        val paths = listOf(
            ImageUtils.originalJpgPath(base),
            ImageUtils.origBackupPath(base),
            ImageUtils.transparentPngPath(base)
        )
        paths.forEach { File(it).writeText("x") }

        ImageUtils.deleteImageWithCompanions(mockk<Context>(), ImageUtils.originalJpgPath(base))

        paths.forEach { assertFalse(File(it).exists()) }
    }

    @Test
    fun `deleteImage removes only the exact file`() {
        val base = ImageUtils.baseOfImage(File(tmpDir, "abc.jpg").absolutePath)
        val orig = ImageUtils.originalJpgPath(base)
        val backup = ImageUtils.origBackupPath(base)
        File(orig).writeText("x")
        File(backup).writeText("x")

        ImageUtils.deleteImage(mockk<Context>(), orig)

        assertFalse(File(orig).exists())
        assertTrue(File(backup).exists())
    }
}