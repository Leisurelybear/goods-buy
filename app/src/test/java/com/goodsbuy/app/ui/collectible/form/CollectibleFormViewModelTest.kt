package com.goodsbuy.app.ui.collectible.form

import android.content.Context
import com.goodsbuy.app.domain.repository.CollectibleRepository
import com.goodsbuy.app.ui.preferences.PreferencesRepository
import io.mockk.mockk
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

class CollectibleFormViewModelTest {

    private lateinit var tmpDir: File

    @Before
    fun setUp() {
        tmpDir = Files.createTempDirectory("form").toFile()
    }

    @After
    fun tearDown() {
        tmpDir.deleteRecursively()
    }

    private fun newViewModel(): CollectibleFormViewModel = CollectibleFormViewModel(
        repository = mockk<CollectibleRepository>(relaxed = true),
        context = mockk<Context>(),
        draftStore = mockk<CollectibleDraftStore>(relaxed = true),
        preferencesRepository = mockk<PreferencesRepository>(relaxed = true)
    )

    @Test
    fun `addCapturedImage appends result and respects max of 9`() {
        val vm = newViewModel()
        repeat(9) { vm.addCapturedImage("/tmp/img$it.jpg", "/tmp/src$it.jpg") }
        vm.addCapturedImage("/tmp/overflow.jpg", "/tmp/overflow-src.jpg")
        assertEquals(9, vm.uiState.value.imagePaths.size)
        assertFalse(vm.uiState.value.imagePaths.contains("/tmp/overflow.jpg"))
    }

    @Test
    fun `addCapturedImage deletes source when result differs`() {
        val src = File(tmpDir, "shot.jpg")
        val result = File(tmpDir, "shot_transparent.png")
        src.writeText("x")
        result.writeText("x")
        val vm = newViewModel()
        vm.addCapturedImage(result.absolutePath, src.absolutePath)
        assertFalse(src.exists())
        assertTrue(result.exists())
        assertEquals(listOf(result.absolutePath), vm.uiState.value.imagePaths)
    }

    @Test
    fun `addCapturedImage keeps source when result equals source`() {
        val src = File(tmpDir, "shot.jpg")
        src.writeText("x")
        val vm = newViewModel()
        vm.addCapturedImage(src.absolutePath, src.absolutePath)
        assertTrue(src.exists())
        assertEquals(listOf(src.absolutePath), vm.uiState.value.imagePaths)
    }

    @Test
    fun `discardCapturedImage deletes the target file and companions`() {
        val base = File(tmpDir, "shot").absolutePath
        val jpg = "$base.jpg"
        val orig = "${base}_orig.jpg"
        val png = "${base}_transparent.png"
        listOf(jpg, orig, png).forEach { File(it).writeText("x") }
        val vm = newViewModel()
        vm.discardCapturedImage(jpg)
        listOf(jpg, orig, png).forEach { assertFalse(File(it).exists()) }
    }
}