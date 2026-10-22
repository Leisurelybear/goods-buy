package com.goodsbuy.app.util

import android.content.Context
import android.net.Uri
import android.util.Log
import com.goodsbuy.app.data.entity.CollectibleEntity
import com.goodsbuy.app.data.db.CollectibleDao
import com.goodsbuy.app.data.db.AppDatabase
import com.goodsbuy.app.domain.model.*
import com.goodsbuy.app.ui.backup.ImportAction
import com.goodsbuy.app.ui.backup.ImportMode
import com.goodsbuy.app.ui.backup.ImportPreviewItem
import com.goodsbuy.app.ui.backup.ImportPreviewResult
import org.json.JSONArray
import org.json.JSONObject
import java.io.*
import java.security.MessageDigest
import java.util.UUID
import androidx.room.withTransaction

private const val TAG = "BackupManager"
private const val BACKUP_VERSION = 1

/**
 * Import-side limits.
 *
 * These must stay comfortably above what our own exporter can produce, otherwise a user's
 * own backup could be rejected on restore: a single collectible can carry many photos, so
 * the entry count is generous while the byte caps keep a hostile archive bounded.
 */
internal const val MAX_ZIP_ENTRIES = 20_000
internal const val MAX_ENTRY_BYTES = 32L * 1024 * 1024
internal const val MAX_TOTAL_BYTES = 200L * 1024 * 1024
internal const val MAX_COMPRESSION_RATIO = 100L
private const val MAX_MANIFEST_BYTES = 5L * 1024 * 1024

/**
 * Hard limits applied while unpacking a backup.
 *
 * Injectable so tests can use tiny values instead of building multi-megabyte
 * archives; production call sites rely on the defaults.
 */
internal data class ZipLimits(
    val maxEntries: Int = MAX_ZIP_ENTRIES,
    val maxEntryBytes: Long = MAX_ENTRY_BYTES,
    val maxTotalBytes: Long = MAX_TOTAL_BYTES,
    val maxCompressionRatio: Long = MAX_COMPRESSION_RATIO
)

data class CollectibleRecord(
    val id: Long,
    val name: String,
    val category: String,
    val type: String,
    val ipName: String,
    val seriesName: String,
    val characterTag: String,
    val remark: String,
    val purchaseChannel: String,
    val purchaseShop: String,
    val purchaseDate: Long,
    val purchasePrice: Double,
    val purchaseQuantity: Int,
    val purchaseShipping: Double,
    val expectedPrice: Double,
    val sellPrice: Double?,
    val sellQuantity: Int?,
    val sellShipping: Double?,
    val isFreeShipping: Boolean,
    val sellDate: Long?,
    val buyerInfo: String?,
    val sellRemark: String?,
    val status: String,
    val storageStatus: String,
    val imageFilenames: List<String>,
    val createdAt: Long,
    val updatedAt: Long
)

data class Manifest(
    val version: Int,
    val timestamp: Long,
    val collectibles: List<CollectibleRecord>
)

/** Coarse phases of an import, surfaced so the UI can label the progress bar. */
enum class BackupStage(val label: String) {
    READING("读取备份"),
    VALIDATING("校验清单"),
    COPYING_IMAGES("复制图片"),
    WRITING("写入数据库"),
    CLEANUP("清理临时文件")
}

/** Coarse phases of an export, surfaced so the UI can label the progress bar. */
enum class ExportStage(val label: String) {
    PREPARING("准备数据"),
    WRITING("写入备份文件"),
    FINALIZING("清理临时文件")
}

/** Read a nullable field, returning null for both a missing key and an explicit JSON null. */
private fun JSONObject.optNullableString(name: String): String? =
    if (has(name) && !isNull(name)) optString(name) else null

private fun JSONObject.optNullableDouble(name: String): Double? =
    if (has(name) && !isNull(name)) optDouble(name) else null

private fun JSONObject.optNullableInt(name: String): Int? =
    if (has(name) && !isNull(name)) optInt(name) else null

private fun JSONObject.optNullableLong(name: String): Long? =
    if (has(name) && !isNull(name)) optLong(name) else null

/**
 * Content fingerprint for dedup: combines the fields that distinguish one
 * collectible from another. Two records with the same name but different
 * price/date/ip/remark are NOT duplicates.
 */
private fun CollectibleRecord.fingerprint(): String {
    val raw = listOf(
        name, ipName, seriesName, characterTag,
        purchaseChannel, purchaseShop, remark,
        purchasePrice.toString(), purchaseQuantity.toString(), purchaseShipping.toString(),
        purchaseDate.toString(), status, storageStatus,
        sellPrice.fp(), sellDate?.toString() ?: "",
        category, type
    ).joinToString("|")
    val md = MessageDigest.getInstance("SHA-1")
    val bytes = md.digest(raw.toByteArray(Charsets.UTF_8))
    return bytes.joinToString("") { "%02x".format(it) }
}

/** 
 * Normalizes a nullable Double for fingerprinting. NaN must never leak into the
 * fingerprint: SQLite stores NaN as NULL, so a record fingerprinted as "NaN"
 * would never match the same entity fingerprinted as "" after a DB round-trip.
 */
private fun Double?.fp(): String = takeIf { it != null && !it.isNaN() }?.toString() ?: ""

/** Fingerprint of a DB entity, matching [CollectibleRecord.fingerprint] field-for-field. */
private fun CollectibleEntity.fingerprint(): String {
    val raw = listOf(
        name, ipName, seriesName, characterTag,
        purchaseChannel, purchaseShop, remark,
        purchasePrice.toString(), purchaseQuantity.toString(), purchaseShipping.toString(),
        purchaseDate.toString(), status, storageStatus,
        sellPrice.fp(), sellDate?.toString() ?: "",
        category, type
    ).joinToString("|")
    val md = MessageDigest.getInstance("SHA-1")
    val bytes = md.digest(raw.toByteArray(Charsets.UTF_8))
    return bytes.joinToString("") { "%02x".format(it) }
}

object BackupManager {

    fun export(
        context: Context,
        collectibles: List<Collectible>,
        outputUri: Uri,
        onProgress: (stage: ExportStage, current: Int, total: Int) -> Unit = { _, _, _ -> }
    ): Result<Unit> {
        val backupDir = File(context.cacheDir, "backup_export")
        return try {
            if (backupDir.exists()) backupDir.deleteRecursively()
            backupDir.mkdirs()

            onProgress(ExportStage.PREPARING, 0, collectibles.size)
            val recordList = collectibles.mapIndexed { index, c ->
                val filenames = mutableListOf<String>()
                c.imagePaths.forEach { path ->
                    val src = File(path)
                    if (src.exists()) {
                        val uniqueName = "${UUID.randomUUID()}_${src.name.substringAfterLast('/', src.name)}"
                        val dest = File(backupDir, uniqueName)
                        src.copyTo(dest, overwrite = false)
                        filenames.add(uniqueName)
                    }
                }
                onProgress(ExportStage.PREPARING, index + 1, collectibles.size)
                CollectibleRecord(
                    id = c.id, name = c.name, category = c.category, type = c.type,
                    ipName = c.ipName, seriesName = c.seriesName, characterTag = c.characterTag,
                    remark = c.remark, purchaseChannel = c.purchaseChannel, purchaseShop = c.purchaseShop,
                    purchaseDate = c.purchaseDate, purchasePrice = c.purchasePrice,
                    purchaseQuantity = c.purchaseQuantity, purchaseShipping = c.purchaseShipping,
                    expectedPrice = c.expectedPrice, sellPrice = c.sellPrice,
                    sellQuantity = c.sellQuantity, sellShipping = c.sellShipping,
                    isFreeShipping = c.isFreeShipping, sellDate = c.sellDate,
                    buyerInfo = c.buyerInfo, sellRemark = c.sellRemark,
                    status = c.status.name, storageStatus = c.storageStatus.name,
                    imageFilenames = filenames, createdAt = c.createdAt, updatedAt = c.updatedAt
                )
            }

            val json = buildJson(BACKUP_VERSION, System.currentTimeMillis(), recordList)
            val imageFiles = backupDir.listFiles().orEmpty()
            val totalEntries = imageFiles.size + 1

            val outputStream = context.contentResolver.openOutputStream(outputUri)
                ?: throw IOException("Unable to open backup output")
            outputStream.use { outStream ->
                java.util.zip.ZipOutputStream(BufferedOutputStream(outStream)).use { zos ->
                    var written = 0
                    zos.putNextEntry(java.util.zip.ZipEntry("manifest.json"))
                    zos.write(json.toByteArray(Charsets.UTF_8))
                    zos.closeEntry()
                    onProgress(ExportStage.WRITING, ++written, totalEntries)

                    imageFiles.forEach { file ->
                        zos.putNextEntry(java.util.zip.ZipEntry("images/${file.name}"))
                        file.inputStream().use { it.copyTo(zos) }
                        zos.closeEntry()
                        onProgress(ExportStage.WRITING, ++written, totalEntries)
                    }
                    zos.finish()
                }
            }

            onProgress(ExportStage.FINALIZING, 0, 1)
            backupDir.deleteRecursively()
            onProgress(ExportStage.FINALIZING, 1, 1)
            AppLogger.i("Export", "Done: count=${recordList.size}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Export failed", e)
            AppLogger.e("Export", "Failed: ${e.message}", e)
            try { backupDir.deleteRecursively() } catch (_: Exception) {}
            Result.failure(e)
        }
    }

    suspend fun previewImport(context: Context, inputUri: Uri, dao: CollectibleDao, mode: ImportMode): ImportPreviewResult {
        return try {
            val tempDir = File(context.cacheDir, "backup_preview")
            if (tempDir.exists()) tempDir.deleteRecursively()
            tempDir.mkdirs()

            extractZip(context, inputUri, tempDir)

            val manifestFile = File(tempDir, "manifest.json")
            if (!manifestFile.exists()) {
                tempDir.deleteRecursively()
                return ImportPreviewResult(emptyList(), 0, 0, 0)
            }
            if (manifestFile.length() > MAX_MANIFEST_BYTES) throw IOException("Manifest is too large")

            val manifest = parseManifest(manifestFile.readText(Charsets.UTF_8))

            // Dedup by content fingerprint, NOT by name alone — many distinct
            // collectibles share a name (e.g. 64 "运费" entries for different orders).
            val existingItems = dao.getAllCollectiblesOnce()
            val existingFingerprints = existingItems.map { it.fingerprint() }.toSet()

            val items = manifest.collectibles.map { record ->
                val isDuplicate = existingFingerprints.contains(record.fingerprint())
                val (action, reason) = when {
                    !isDuplicate -> ImportAction.IMPORT to ""
                    mode == ImportMode.SKIP -> ImportAction.SKIP to "已存在相同藏品: ${record.name}"
                    mode == ImportMode.ADD -> ImportAction.IMPORT to "将以新条目添加（保留原条目）"
                    mode == ImportMode.OVERWRITE -> ImportAction.OVERWRITE to "将覆盖已存在的相同藏品"
                    else -> ImportAction.IMPORT to ""
                }
                ImportPreviewItem(record, action, reason)
            }

            tempDir.deleteRecursively()

            ImportPreviewResult(
                items = items,
                total = items.size,
                willImport = items.count { it.action != ImportAction.SKIP },
                willSkip = items.count { it.action == ImportAction.SKIP }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Preview import failed", e)
            try { File(context.cacheDir, "backup_preview").deleteRecursively() } catch (_: Exception) {}
            ImportPreviewResult(emptyList(), 0, 0, 0)
        }
    }

    suspend fun import(
        context: Context,
        inputUri: Uri,
        database: AppDatabase,
        mode: ImportMode,
        onProgress: (stage: BackupStage, current: Int, total: Int) -> Unit = { _, _, _ -> }
    ): Result<Int> {
        val createdImagePaths = mutableListOf<String>()
        var databaseCommitted = false
        return try {
            val dao = database.collectibleDao()
            val imagesDir = File(context.filesDir, "images")
            imagesDir.mkdirs()

            val tempDir = File(context.cacheDir, "backup_import")
            if (tempDir.exists()) tempDir.deleteRecursively()
            tempDir.mkdirs()

            extractZip(context, inputUri, tempDir)
            onProgress(BackupStage.READING, 1, 1)

            val manifestFile = File(tempDir, "manifest.json")
            if (!manifestFile.exists()) {
                tempDir.deleteRecursively()
                return Result.failure(IllegalStateException("No manifest found"))
            }

            if (manifestFile.length() > MAX_MANIFEST_BYTES) throw IOException("Manifest is too large")
            val manifest = parseManifest(manifestFile.readText(Charsets.UTF_8))
            val total = manifest.collectibles.size
            onProgress(BackupStage.VALIDATING, 1, 1)

            // Build fingerprint -> existing entity map for dedup & overwrite lookup.
            val existingByFingerprint = dao.getAllCollectiblesOnce().associateBy { it.fingerprint() }
            Log.d(TAG, "Import: existing in DB=${existingByFingerprint.size}, manifest records=${manifest.collectibles.size}")

            val imageMap = mutableMapOf<String, String>()
            // Images replaced by an OVERWRITE import, deleted once the transaction commits.
            val replacedImagePaths = mutableListOf<String>()

            var importedCount = 0
            var skippedCount = 0
            val plans = manifest.collectibles.map { record ->
                val fp = record.fingerprint()
                val existing = existingByFingerprint[fp]
                val isDuplicate = existing != null
                val action = when {
                    isDuplicate && mode == ImportMode.SKIP -> ImportAction.SKIP
                    isDuplicate && mode == ImportMode.OVERWRITE -> ImportAction.OVERWRITE
                    else -> ImportAction.IMPORT
                }
                Triple(record, existing, action)
            }
            val filesToCopy = plans.filter { it.third != ImportAction.SKIP }
                .flatMap { it.first.imageFilenames }
                .toSet()
            val imagesToCopy = File(tempDir, "images").listFiles()
                ?.filter { it.name in filesToCopy }
                .orEmpty()
            imagesToCopy.forEachIndexed { index, file ->
                val newFileName = "${UUID.randomUUID()}_${file.name}"
                val newFile = File(imagesDir, newFileName)
                file.copyTo(newFile, overwrite = false)
                // Restored images arrive as plain files, so give them the same thumbnail
                // the capture pipeline produces; otherwise grids would keep decoding the
                // full-size image until it is edited.
                ImageUtils.regenerateThumbnail(ImageUtils.baseOfImage(newFile.absolutePath), newFile.absolutePath)
                imageMap[file.name] = newFile.absolutePath
                createdImagePaths.add(newFile.absolutePath)
                onProgress(BackupStage.COPYING_IMAGES, index + 1, imagesToCopy.size)
            }

            val updates = mutableListOf<CollectibleEntity>()
            val inserts = mutableListOf<CollectibleEntity>()
            plans.forEachIndexed { index, (record, existing, action) ->
                val newImagePaths = record.imageFilenames.mapNotNull { imageMap[it] }

                if (action == ImportAction.SKIP) skippedCount++

                when (action) {
                    ImportAction.SKIP -> {
                        Log.d(TAG, "Skipping duplicate: ${record.name} (fp=${record.fingerprint()})")
                        AppLogger.d("Import", "Skip duplicate: ${record.name}")
                    }
                    ImportAction.OVERWRITE -> {
                        if (record.imageFilenames.isNotEmpty()) {
                            replacedImagePaths.addAll(existing!!.imagePaths.split(",").map { it.trim() }.filter { it.isNotEmpty() })
                        }
                        val updated = existing!!.copy(
                            name = record.name,
                            category = record.category, type = record.type,
                            ipName = record.ipName, seriesName = record.seriesName, characterTag = record.characterTag,
                            remark = record.remark, purchaseChannel = record.purchaseChannel, purchaseShop = record.purchaseShop,
                            purchaseDate = record.purchaseDate, purchasePrice = record.purchasePrice,
                            purchaseQuantity = record.purchaseQuantity, purchaseShipping = record.purchaseShipping,
                            expectedPrice = record.expectedPrice, sellPrice = record.sellPrice,
                            sellQuantity = record.sellQuantity, sellShipping = record.sellShipping,
                            isFreeShipping = record.isFreeShipping, sellDate = record.sellDate,
                            buyerInfo = record.buyerInfo, sellRemark = record.sellRemark,
                            status = record.status, storageStatus = record.storageStatus,
                            imagePaths = if (record.imageFilenames.isEmpty()) existing.imagePaths else newImagePaths.joinToString(","),
                            createdAt = record.createdAt, updatedAt = System.currentTimeMillis()
                        )
                        updates.add(updated)
                        importedCount++
                    }
                    else -> {
                        // Insert new (also covers ADD-with-suffix and non-duplicate OVERWRITE)
                        val suffix = if (existing != null && mode == ImportMode.ADD) "_${UUID.randomUUID().toString().substring(0, 4)}" else ""
                        val entity = CollectibleEntity(
                            name = "${record.name}$suffix",
                            category = record.category, type = record.type,
                            ipName = record.ipName, seriesName = record.seriesName, characterTag = record.characterTag,
                            remark = record.remark, purchaseChannel = record.purchaseChannel, purchaseShop = record.purchaseShop,
                            purchaseDate = record.purchaseDate, purchasePrice = record.purchasePrice,
                            purchaseQuantity = record.purchaseQuantity, purchaseShipping = record.purchaseShipping,
                            expectedPrice = record.expectedPrice, sellPrice = record.sellPrice,
                            sellQuantity = record.sellQuantity, sellShipping = record.sellShipping,
                            isFreeShipping = record.isFreeShipping, sellDate = record.sellDate,
                            buyerInfo = record.buyerInfo, sellRemark = record.sellRemark,
                            status = record.status, storageStatus = record.storageStatus,
                            imagePaths = newImagePaths.joinToString(","),
                            createdAt = record.createdAt, updatedAt = record.updatedAt
                        )
                        inserts.add(entity)
                        importedCount++
                    }
                }
            }

            onProgress(BackupStage.WRITING, 0, 1)
            database.withTransaction {
                dao.updateCollectibles(updates)
                dao.insertCollectibles(inserts)
            }
            databaseCommitted = true
            if (replacedImagePaths.isNotEmpty()) {
                // The replaced files may still be shared with another collectible, so only
                // drop the ones nothing references any more.
                val stillReferenced = dao.getAllCollectiblesOnce()
                    .flatMap { it.imagePaths.split(",") }
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                ImageUtils.deleteUnreferencedImages(replacedImagePaths, stillReferenced)
            }
            onProgress(BackupStage.WRITING, 1, 1)
            Log.d(TAG, "Import done: imported=$importedCount, skipped=$skippedCount, mode=$mode")
            AppLogger.i("Import", "Done: imported=$importedCount, skipped=$skippedCount, mode=$mode, total=$total")
            onProgress(BackupStage.CLEANUP, 1, 1)
            tempDir.deleteRecursively()
            Result.success(importedCount)
        } catch (e: Exception) {
            Log.e(TAG, "Import failed", e)
            AppLogger.e("Import", "Failed: ${e.message}", e)
            try { File(context.cacheDir, "backup_import").deleteRecursively() } catch (_: Exception) {}
            if (!databaseCommitted) createdImagePaths.forEach { ImageUtils.deleteImageWithCompanions(it) }
            Result.failure(e)
        }
    }

    private fun extractZip(context: Context, inputUri: Uri, tempDir: File) {
        val input = context.contentResolver.openInputStream(inputUri)
            ?: throw IOException("Unable to open backup input")
        input.use { extractZip(it, tempDir) }
    }

    /**
     * Streams a backup ZIP into [tempDir].
     *
     * Rejects anything that could escape [tempDir] (path traversal, absolute
     * paths, nested paths under images/) as well as anything that could exhaust
     * disk or memory (too many entries, oversized entries, oversized totals,
     * absurd compression ratios).
     */
    internal fun extractZip(input: InputStream, tempDir: File, limits: ZipLimits = ZipLimits()) {
        var totalBytes = 0L
        var entries = 0
        val seenEntries = mutableSetOf<String>()
        input.use { raw ->
            java.util.zip.ZipInputStream(BufferedInputStream(raw)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val currentEntry = entry
                    if (++entries > limits.maxEntries) throw IOException("Backup contains too many files")
                    val normalized = currentEntry.name.replace('\\', '/')
                    if (currentEntry.isDirectory) {
                        if (normalized != "images/") throw IOException("Invalid backup path")
                        zis.closeEntry()
                        entry = zis.nextEntry
                        continue
                    }
                    if (!isAllowedEntry(normalized) || !seenEntries.add(normalized)) throw IOException("Invalid or duplicate backup path")
                    val outFile = File(tempDir, normalized)
                    val root = tempDir.canonicalFile
                    val target = outFile.canonicalFile
                    if (target != root && !target.path.startsWith(root.path + File.separator)) throw IOException("Invalid backup path")
                    outFile.parentFile?.mkdirs()
                    var entryBytes = 0L
                    outFile.outputStream().use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        while (true) {
                            val read = zis.read(buffer)
                            if (read < 0) break
                            entryBytes += read
                            totalBytes += read
                            if (entryBytes > limits.maxEntryBytes || totalBytes > limits.maxTotalBytes) throw IOException("Backup is too large")
                            output.write(buffer, 0, read)
                        }
                    }
                    // A tiny compressed size for a large entry is the classic zip-bomb
                    // shape; reject it before writing more to disk.
                    val compressedSize = currentEntry.compressedSize
                    if (compressedSize > 0 && entryBytes / compressedSize > limits.maxCompressionRatio) {
                        throw IOException("Suspicious compression ratio")
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
        }
    }

    internal fun isAllowedEntry(name: String): Boolean {
        if (name.isBlank() || name.indexOf('\u0000') >= 0 || name.startsWith("/") || name.contains(":")) return false
        if (name == "manifest.json") return true
        if (!name.startsWith("images/")) return false
        val fileName = name.removePrefix("images/")
        return fileName.isNotBlank() && !fileName.contains('/') && fileName != "." && fileName != ".."
    }

    private fun buildJson(version: Int, timestamp: Long, collectibles: List<CollectibleRecord>): String {
        val obj = JSONObject()
        obj.put("version", version)
        obj.put("timestamp", timestamp)
        val arr = JSONArray()
        collectibles.forEach { c ->
            val item = JSONObject()
            item.put("id", c.id)
            item.put("name", c.name)
            item.put("category", c.category)
            item.put("type", c.type)
            item.put("ipName", c.ipName)
            item.put("seriesName", c.seriesName)
            item.put("characterTag", c.characterTag)
            item.put("remark", c.remark)
            item.put("purchaseChannel", c.purchaseChannel)
            item.put("purchaseShop", c.purchaseShop)
            item.put("purchaseDate", c.purchaseDate)
            item.put("purchasePrice", c.purchasePrice)
            item.put("purchaseQuantity", c.purchaseQuantity)
            item.put("purchaseShipping", c.purchaseShipping)
            item.put("expectedPrice", c.expectedPrice)
            c.sellPrice?.let { item.put("sellPrice", it) }
            c.sellQuantity?.let { item.put("sellQuantity", it) }
            c.sellShipping?.let { item.put("sellShipping", it) }
            item.put("isFreeShipping", c.isFreeShipping)
            c.sellDate?.let { item.put("sellDate", it) }
            c.buyerInfo?.let { item.put("buyerInfo", it) }
            c.sellRemark?.let { item.put("sellRemark", it) }
            item.put("status", c.status)
            item.put("storageStatus", c.storageStatus)
            item.put("imageFilenames", JSONArray(c.imageFilenames))
            item.put("createdAt", c.createdAt)
            item.put("updatedAt", c.updatedAt)
            arr.put(item)
        }
        obj.put("collectibles", arr)
        return obj.toString(2)
    }

    private fun parseManifest(json: String): Manifest {
        val obj = JSONObject(json)
        val version = obj.getInt("version")
        val timestamp = obj.getLong("timestamp")
        val collectibles = mutableListOf<CollectibleRecord>()
        val arr = obj.getJSONArray("collectibles")
        if (arr.length() > MAX_ZIP_ENTRIES) throw IOException("Manifest contains too many records")
        for (i in 0 until arr.length()) {
            val item = arr.getJSONObject(i)
            val imgArr = item.optJSONArray("imageFilenames")
            val imageFilenames = if (imgArr != null) {
                (0 until imgArr.length()).map { imgArr.getString(it) }
            } else {
                emptyList()
            }
            collectibles.add(CollectibleRecord(
                id = item.optLong("id"), name = item.optString("name"),
                category = item.optString("category"), type = item.optString("type"),
                ipName = item.optString("ipName"), seriesName = item.optString("seriesName"),
                characterTag = item.optString("characterTag"), remark = item.optString("remark"),
                purchaseChannel = item.optString("purchaseChannel"), purchaseShop = item.optString("purchaseShop"),
                purchaseDate = item.optLong("purchaseDate"), purchasePrice = item.optDouble("purchasePrice"),
                purchaseQuantity = item.optInt("purchaseQuantity"), purchaseShipping = item.optDouble("purchaseShipping"),
                expectedPrice = item.optDouble("expectedPrice"),
                sellPrice = item.optNullableDouble("sellPrice"),
                sellQuantity = item.optNullableInt("sellQuantity"),
                sellShipping = item.optNullableDouble("sellShipping"),
                isFreeShipping = item.optBoolean("isFreeShipping"),
                sellDate = item.optNullableLong("sellDate"),
                buyerInfo = item.optNullableString("buyerInfo"),
                sellRemark = item.optNullableString("sellRemark"),
                status = item.optString("status"), storageStatus = item.optString("storageStatus"),
                imageFilenames = imageFilenames,
                createdAt = item.optLong("createdAt"), updatedAt = item.optLong("updatedAt")
            ))
        }
        return Manifest(version, timestamp, collectibles)
    }
}
