package com.goodsbuy.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object ImageUtils {
    private const val MAX_FULL_DIMENSION = 2048

    fun copyImageToInternalStorage(context: Context, uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val fileName = "${UUID.randomUUID()}.jpg"
            val file = File(context.filesDir, "images/$fileName")
            file.parentFile?.mkdirs()
            inputStream.use { input ->
                FileOutputStream(file).use { out ->
                    input.copyTo(out)
                }
            }
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun deleteImage(path: String) {
        try {
            val file = File(path)
            if (file.exists()) file.delete()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun deleteImageWithCompanions(path: String) {
        try {
            val base = baseOfImage(path)
            listOf(File(path), File(origBackupPath(base)), File(transparentPngPath(base))).forEach {
                if (it.exists()) it.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun baseOfImage(path: String): String = when {
        path.endsWith("_transparent.png") -> path.removeSuffix("_transparent.png")
        path.endsWith("_orig.jpg") -> path.removeSuffix("_orig.jpg")
        else -> path.removeSuffix(".jpg")
    }

    fun originalJpgPath(base: String): String = "$base.jpg"
    fun origBackupPath(base: String): String = "${base}_orig.jpg"
    fun transparentPngPath(base: String): String = "${base}_transparent.png"
    fun isEditedImage(path: String): Boolean = path.endsWith("_transparent.png")

    fun sampleSizeFor(width: Int, height: Int, maxDimension: Int): Int {
        if (width <= 0 || height <= 0 || maxDimension <= 0) return 1
        var sample = 1
        val maxEdge = maxOf(width, height)
        while (maxEdge / sample > maxDimension) {
            sample *= 2
        }
        return sample
    }

    fun decodeDownscaled(path: String, maxDimension: Int): Bitmap? {
        return try {
            val source = ImageDecoder.createSource(File(path))
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val sample = sampleSizeFor(info.size.width, info.size.height, maxDimension)
                if (sample > 1) decoder.setTargetSampleSize(sample)
            }
        } catch (e: Exception) {
            AppLogger.e("Fade", "decodeDownscaled failed: $path", e)
            null
        }
    }

    fun applyEdgeFade(displayPath: String, shape: FadeShape, intensity: Float): String? {
        return try {
            if (intensity <= 0f) return displayPath
            val base = baseOfImage(displayPath)
            val src = decodeDownscaled(displayPath, MAX_FULL_DIMENSION) ?: return null
            val w = src.width
            val h = src.height
            val pixels = IntArray(w * h)
            src.getPixels(pixels, 0, w, 0, 0, w, h)
            val alpha = EdgeFadeMask.alphaMask(w, h, shape, intensity)
            val out = EdgeFadeMask.applyAlpha(pixels, alpha)
            val outBmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            outBmp.setPixels(out, 0, w, 0, 0, w, h)
            val png = File(transparentPngPath(base))
            FileOutputStream(png).use { outBmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
            src.recycle()
            outBmp.recycle()
            png.absolutePath
        } catch (e: Exception) {
            AppLogger.e("Fade", "applyEdgeFade failed: $displayPath", e)
            null
        }
    }

    fun resetEdgeFade(displayPath: String): String? {
        return try {
            val base = baseOfImage(displayPath)
            val png = File(transparentPngPath(base))
            val original = File(originalJpgPath(base))
            val backup = File(origBackupPath(base))
            val restoreFrom = when {
                original.exists() -> original
                backup.exists() -> backup
                else -> null
            }
            if (restoreFrom != null) {
                if (png.exists()) png.delete()
                if (backup.exists() && restoreFrom != backup) backup.delete()
                restoreFrom.absolutePath
            } else {
                displayPath
            }
        } catch (e: Exception) {
            AppLogger.e("Fade", "resetEdgeFade failed: $displayPath", e)
            null
        }
    }

    /** Deletes the original jpg and any leftover backup of an edited png, keeping the png. */
    fun deleteOriginalCompanions(path: String) {
        try {
            val base = baseOfImage(path)
            listOf(File(originalJpgPath(base)), File(origBackupPath(base))).forEach {
                if (it.exists()) it.delete()
            }
        } catch (e: Exception) {
            AppLogger.e("Fade", "deleteOriginalCompanions failed: $path", e)
        }
    }

    fun deleteUnreferencedImages(paths: List<String>, persistedPaths: List<String>) {
        try {
            val persistedSet = persistedPaths.toSet()
            val persistedBases = persistedPaths.map { baseOfImage(it) }.toSet()
            paths.forEach { path ->
                val base = baseOfImage(path)
                if (base !in persistedBases) {
                    deleteImageWithCompanions(originalJpgPath(base))
                } else if (path !in persistedSet) {
                    val files = listOf(File(path), File(transparentPngPath(base)), File(origBackupPath(base)))
                    files.filterNot { it.absolutePath in persistedSet }.forEach { if (it.exists()) it.delete() }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}