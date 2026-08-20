package com.goodsbuy.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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

    fun deleteImage(context: Context, path: String) {
        try {
            val file = File(path)
            if (file.exists()) file.delete()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun deleteImageWithCompanions(context: Context, path: String) {
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

    fun decodeDownscaled(path: String, maxDimension: Int): Bitmap? {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, bounds)
            var sample = 1
            while (bounds.outWidth / (sample * 2) >= maxDimension &&
                bounds.outHeight / (sample * 2) >= maxDimension
            ) {
                sample *= 2
            }
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            BitmapFactory.decodeFile(path, opts)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun applyEdgeFade(context: Context, displayPath: String, shape: FadeShape, intensity: Float): String? {
        return try {
            if (intensity <= 0f) return displayPath
            val base = baseOfImage(displayPath)
            val origFile = File(origBackupPath(base))
            val origExists = origFile.exists()
            val sourcePath = if (origExists) origBackupPath(base) else displayPath
            if (!origExists && !isEditedImage(displayPath)) {
                File(displayPath).copyTo(origFile)
            }
            val src = decodeDownscaled(sourcePath, MAX_FULL_DIMENSION) ?: return null
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
            e.printStackTrace()
            null
        }
    }

    fun resetEdgeFade(context: Context, displayPath: String): String? {
        return try {
            val base = baseOfImage(displayPath)
            val png = File(transparentPngPath(base))
            val orig = File(origBackupPath(base))
            if (png.exists()) png.delete()
            if (orig.exists()) orig.delete()
            val original = File(originalJpgPath(base))
            if (original.exists()) original.absolutePath else displayPath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}