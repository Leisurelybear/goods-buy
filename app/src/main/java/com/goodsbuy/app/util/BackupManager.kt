package com.goodsbuy.app.util

import android.content.Context
import android.net.Uri
import android.util.Log
import com.goodsbuy.app.data.entity.CollectibleEntity
import com.goodsbuy.app.data.db.CollectibleDao
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

private const val TAG = "BackupManager"
private const val BACKUP_VERSION = 1

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

    fun export(context: Context, collectibles: List<Collectible>, outputUri: Uri): Boolean {
        return try {
            val backupDir = File(context.cacheDir, "backup_export")
            backupDir.mkdirs()

            val recordList = collectibles.map { c ->
                val filenames = mutableListOf<String>()
                c.imagePaths.forEach { path ->
                    val src = File(path)
                    if (src.exists()) {
                        val uniqueName = "${System.currentTimeMillis()}_${src.name}"
                        val dest = File(backupDir, uniqueName)
                        src.copyTo(dest, overwrite = false)
                        filenames.add(uniqueName)
                    }
                }
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

            context.contentResolver.openOutputStream(outputUri)?.use { outStream ->
                java.util.zip.ZipOutputStream(BufferedOutputStream(outStream)).use { zos ->
                    zos.putNextEntry(java.util.zip.ZipEntry("manifest.json"))
                    zos.write(json.toByteArray(Charsets.UTF_8))
                    zos.closeEntry()

                    backupDir.listFiles()?.forEach { file ->
                        zos.putNextEntry(java.util.zip.ZipEntry("images/${file.name}"))
                        file.inputStream().use { it.copyTo(zos) }
                        zos.closeEntry()
                    }
                    zos.finish()
                }
            }

            backupDir.deleteRecursively()
            AppLogger.i("Export", "Done: count=${recordList.size}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Export failed", e)
            AppLogger.e("Export", "Failed: ${e.message}", e)
            try { File(context.cacheDir, "backup_export").deleteRecursively() } catch (_: Exception) {}
            false
        }
    }

    suspend fun previewImport(context: Context, inputUri: Uri, dao: CollectibleDao, mode: ImportMode): ImportPreviewResult {
        return try {
            val tempDir = File(context.cacheDir, "backup_preview")
            if (tempDir.exists()) tempDir.deleteRecursively()
            tempDir.mkdirs()

            context.contentResolver.openInputStream(inputUri)?.use { ins ->
                java.util.zip.ZipInputStream(BufferedInputStream(ins)).use { zis ->
                    var entry: java.util.zip.ZipEntry? = zis.nextEntry
                    while (entry != null) {
                        val outFile = File(tempDir, entry.name)
                        if (entry.isDirectory) {
                            outFile.mkdirs()
                        } else {
                            outFile.parentFile?.mkdirs()
                            outFile.outputStream().use { outs -> zis.copyTo(outs) }
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }
            }

            val manifestFile = File(tempDir, "manifest.json")
            if (!manifestFile.exists()) return ImportPreviewResult(emptyList(), 0, 0, 0)

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
        dao: CollectibleDao,
        mode: ImportMode,
        onProgress: (current: Int, total: Int) -> Unit = { _, _ -> }
    ): Result<Int> {
        return try {
            val imagesDir = File(context.filesDir, "images")
            imagesDir.mkdirs()

            val tempDir = File(context.cacheDir, "backup_import")
            if (tempDir.exists()) tempDir.deleteRecursively()
            tempDir.mkdirs()

            context.contentResolver.openInputStream(inputUri)?.use { ins ->
                java.util.zip.ZipInputStream(BufferedInputStream(ins)).use { zis ->
                    var entry: java.util.zip.ZipEntry? = zis.nextEntry
                    while (entry != null) {
                        val outFile = File(tempDir, entry.name)
                        if (entry.isDirectory) {
                            outFile.mkdirs()
                        } else {
                            outFile.parentFile?.mkdirs()
                            outFile.outputStream().use { outs -> zis.copyTo(outs) }
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }
            }

            val manifestFile = File(tempDir, "manifest.json")
            if (!manifestFile.exists()) return Result.failure(IllegalStateException("No manifest found"))

            val manifest = parseManifest(manifestFile.readText(Charsets.UTF_8))
            val total = manifest.collectibles.size

            // Build fingerprint -> existing entity map for dedup & overwrite lookup.
            val existingByFingerprint = dao.getAllCollectiblesOnce().associateBy { it.fingerprint() }
            Log.d(TAG, "Import: existing in DB=${existingByFingerprint.size}, manifest records=${manifest.collectibles.size}")

            val imageMap = mutableMapOf<String, String>()
            File(tempDir, "images").listFiles()?.forEach { file ->
                val uniqueSuffix = if (mode == ImportMode.ADD) "_${UUID.randomUUID().toString().substring(0, 8)}" else ""
                val newFileName = "${System.currentTimeMillis()}${uniqueSuffix}_${file.name}"
                val newFile = File(imagesDir, newFileName)
                file.copyTo(newFile, overwrite = false)
                imageMap[file.name] = newFile.absolutePath
            }

            var importedCount = 0
            var skippedCount = 0
            manifest.collectibles.forEachIndexed { index, record ->
                val newImagePaths = record.imageFilenames.mapNotNull { fn -> imageMap[fn] }
                val fp = record.fingerprint()
                val existing = existingByFingerprint[fp]
                val isDuplicate = existing != null

                if (isDuplicate && mode == ImportMode.SKIP) skippedCount++

                when {
                    isDuplicate && mode == ImportMode.SKIP -> {
                        Log.d(TAG, "Skipping duplicate: ${record.name} (fp=$fp)")
                        AppLogger.d("Import", "Skip duplicate: ${record.name}")
                    }
                    isDuplicate && mode == ImportMode.OVERWRITE -> {
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
                            imagePaths = newImagePaths.joinToString(","),
                            createdAt = record.createdAt, updatedAt = System.currentTimeMillis()
                        )
                        dao.updateCollectible(updated)
                        importedCount++
                    }
                    else -> {
                        // Insert new (also covers ADD-with-suffix and non-duplicate OVERWRITE)
                        val suffix = if (isDuplicate && mode == ImportMode.ADD) "_${UUID.randomUUID().toString().substring(0, 4)}" else ""
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
                        dao.insertCollectible(entity)
                        importedCount++
                    }
                }
                onProgress(index + 1, total)
            }

            Log.d(TAG, "Import done: imported=$importedCount, skipped=$skippedCount, mode=$mode")
            AppLogger.i("Import", "Done: imported=$importedCount, skipped=$skippedCount, mode=$mode, total=$total")
            tempDir.deleteRecursively()
            Result.success(importedCount)
        } catch (e: Exception) {
            Log.e(TAG, "Import failed", e)
            AppLogger.e("Import", "Failed: ${e.message}", e)
            try { File(context.cacheDir, "backup_import").deleteRecursively() } catch (_: Exception) {}
            Result.failure(e)
        }
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
