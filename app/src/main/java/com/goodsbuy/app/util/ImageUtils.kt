package com.goodsbuy.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageDecoder
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object ImageUtils {
    private const val MAX_FULL_DIMENSION = 2048
    private const val THUMBNAIL_MAX_DIMENSION = 512
    private const val JPEG_QUALITY = 85

    /**
     * Decodes [uri], applies the EXIF orientation, downscales it to
     * [MAX_FULL_DIMENSION] and stores it as JPEG in internal storage.
     *
     * A matching thumbnail is written next to it so grids can decode a small
     * bitmap instead of the full-resolution file. Previously the picked file was
     * copied byte-for-byte (often a 5-10 MB camera shot) under a `.jpg` name.
     */
    fun copyImageToInternalStorage(context: Context, uri: Uri): String? {
        return try {
            val decoded = decodeDownscaled(context, uri, MAX_FULL_DIMENSION) ?: return null
            val bitmap = flattenOntoWhite(decoded)
            if (bitmap !== decoded) decoded.recycle()

            val base = File(context.filesDir, "images/${UUID.randomUUID()}").absolutePath
            File(base).parentFile?.mkdirs()
            val stored = writeJpeg(bitmap, originalJpgPath(base))
            writeThumbnail(base, bitmap)
            bitmap.recycle()
            stored?.absolutePath
        } catch (e: Exception) {
            AppLogger.e("Image", "copyImageToInternalStorage failed", e)
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
            companionPaths(base, path).forEach {
                if (it.exists()) it.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun baseOfImage(path: String): String = when {
        path.endsWith("_transparent.png") -> path.removeSuffix("_transparent.png")
        path.endsWith("_thumb.png") -> path.removeSuffix("_thumb.png")
        path.endsWith("_thumb.jpg") -> path.removeSuffix("_thumb.jpg")
        path.endsWith("_orig.jpg") -> path.removeSuffix("_orig.jpg")
        else -> path.removeSuffix(".jpg")
    }

    fun originalJpgPath(base: String): String = "$base.jpg"
    fun origBackupPath(base: String): String = "${base}_orig.jpg"
    fun transparentPngPath(base: String): String = "${base}_transparent.png"
    fun isEditedImage(path: String): Boolean = path.endsWith("_transparent.png")

    /**
     * Returns the thumbnail for [path] when one has been generated, otherwise
     * [path] itself. Images stored before thumbnails existed still resolve.
     */
    fun thumbnailPathFor(path: String): String {
        val base = baseOfImage(path)
        val jpg = File("${base}_thumb.jpg")
        if (jpg.exists()) return jpg.absolutePath
        val png = File("${base}_thumb.png")
        if (png.exists()) return png.absolutePath
        return path
    }

    /** Regenerates the thumbnail of [base] from [sourcePath] (used when un-editing). */
    fun regenerateThumbnail(base: String, sourcePath: String): Boolean {
        val decoded = decodeDownscaled(sourcePath, THUMBNAIL_MAX_DIMENSION) ?: return false
        val ok = writeThumbnail(base, decoded)
        decoded.recycle()
        return ok
    }

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

    /** ImageDecoder applies the EXIF orientation automatically. */
    private fun decodeDownscaled(context: Context, uri: Uri, maxDimension: Int): Bitmap? {
        return try {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val sample = sampleSizeFor(info.size.width, info.size.height, maxDimension)
                if (sample > 1) decoder.setTargetSampleSize(sample)
            }
        } catch (e: Exception) {
            AppLogger.e("Image", "decode failed: $uri", e)
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
            // Keep the grid thumbnail in sync with the edited image.
            writeThumbnail(base, outBmp)
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
                regenerateThumbnail(base, restoreFrom.absolutePath)
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

    private fun companionPaths(base: String, path: String): List<File> = listOf(
        File(path),
        File(originalJpgPath(base)),
        File(origBackupPath(base)),
        File(transparentPngPath(base)),
        File("${base}_thumb.jpg"),
        File("${base}_thumb.png")
    ).distinctBy { it.absolutePath }

    private fun writeJpeg(bitmap: Bitmap, target: String): File? = try {
        val file = File(target)
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
        file
    } catch (e: Exception) {
        AppLogger.e("Image", "writeJpeg failed: $target", e)
        null
    }

    /**
     * Writes `<base>_thumb.jpg` (or `.png` when the source has transparency so the
     * faded edges are preserved) and removes the stale variant of the other type.
     */
    private fun writeThumbnail(base: String, source: Bitmap): Boolean {
        val hasAlpha = source.hasAlpha()
        val target = if (hasAlpha) "${base}_thumb.png" else "${base}_thumb.jpg"
        val stale = File(if (hasAlpha) "${base}_thumb.jpg" else "${base}_thumb.png")
        if (stale.exists()) stale.delete()

        val maxEdge = maxOf(source.width, source.height)
        val scaled = if (maxEdge <= THUMBNAIL_MAX_DIMENSION) {
            source
        } else {
            val ratio = THUMBNAIL_MAX_DIMENSION.toFloat() / maxEdge
            Bitmap.createScaledBitmap(
                source,
                (source.width * ratio).toInt().coerceAtLeast(1),
                (source.height * ratio).toInt().coerceAtLeast(1),
                true
            )
        }
        return try {
            FileOutputStream(File(target)).use { out ->
                if (hasAlpha) {
                    scaled.compress(Bitmap.CompressFormat.PNG, 100, out)
                } else {
                    scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
                }
            }
            true
        } catch (e: Exception) {
            AppLogger.e("Image", "writeThumbnail failed: $target", e)
            false
        } finally {
            if (scaled !== source) scaled.recycle()
        }
    }

    /**
     * JPEG has no alpha channel, so transparent pixels would otherwise turn
     * black. Composite onto white first.
     */
    private fun flattenOntoWhite(bitmap: Bitmap): Bitmap {
        if (!bitmap.hasAlpha()) return bitmap
        val flattened = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(flattened)
        canvas.drawColor(Color.WHITE)
        canvas.drawBitmap(bitmap, 0f, 0f, null)
        return flattened
    }
}
