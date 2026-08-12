package com.goodsbuy.app.util
import kotlinx.coroutines.flow.first

import android.content.Context
import android.net.Uri
import android.util.Log
import com.goodsbuy.app.data.entity.CollectibleEntity
import com.goodsbuy.app.data.db.CollectibleDao
import com.goodsbuy.app.domain.model.*
import com.goodsbuy.app.ui.backup.ImportAction
import com.goodsbuy.app.ui.backup.ImportPreviewItem
import com.goodsbuy.app.ui.backup.ImportPreviewResult
import org.json.JSONArray
import org.json.JSONObject
import java.io.*
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
            true
        } catch (e: Exception) {
            Log.e(TAG, "Export failed", e)
            try { File(context.cacheDir, "backup_export").deleteRecursively() } catch (_: Exception) {}
            false
        }
    }

    suspend fun previewImport(context: Context, inputUri: Uri, dao: CollectibleDao, forceImportDuplicates: Boolean): ImportPreviewResult {
        return try {
            val tempDir = File(context.cacheDir, "backup_preview")
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

            // Get existing names from database
            val existingItems = dao.getAllCollectibles().first()
            val existingNames = existingItems.map { it.name }.toSet()

            val items = manifest.collectibles.map { record ->
                val isDuplicate = existingNames.contains(record.name)
                val action = if (forceImportDuplicates || !isDuplicate) ImportAction.IMPORT else ImportAction.SKIP
                val reason = if (isDuplicate && !forceImportDuplicates) "已存在同名藏品: ${record.name}" else ""
                ImportPreviewItem(record, action, reason)
            }

            tempDir.deleteRecursively()

            ImportPreviewResult(
                items = items,
                total = items.size,
                willImport = items.count { it.action == ImportAction.IMPORT },
                willSkip = items.count { it.action == ImportAction.SKIP }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Preview import failed", e)
            try { File(context.cacheDir, "backup_preview").deleteRecursively() } catch (_: Exception) {}
            ImportPreviewResult(emptyList(), 0, 0, 0)
        }
    }

    suspend fun import(context: Context, inputUri: Uri, dao: CollectibleDao, forceImportDuplicates: Boolean): Result<Int> {
        return try {
            val imagesDir = File(context.filesDir, "images")
            imagesDir.mkdirs()

            val tempDir = File(context.cacheDir, "backup_import")
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

            val imageMap = mutableMapOf<String, String>()
            File(tempDir, "images").listFiles()?.forEach { file ->
                val uniqueSuffix = if (forceImportDuplicates) "_${UUID.randomUUID().toString().substring(0, 8)}" else ""
                val newFileName = "${System.currentTimeMillis()}${uniqueSuffix}_${file.name}"
                val newFile = File(imagesDir, newFileName)
                file.copyTo(newFile, overwrite = false)
                imageMap[file.name] = newFile.absolutePath
            }

            var importedCount = 0
            manifest.collectibles.forEach { record ->
                val isDuplicate = dao.searchByName(record.name).isNotEmpty()
                if (isDuplicate && !forceImportDuplicates) {
                    Log.d(TAG, "Skipping duplicate: ${record.name}")
                    return@forEach
                }

                val newImagePaths = record.imageFilenames.mapNotNull { fn -> imageMap[fn] }
                val entity = CollectibleEntity(
                    name = if (isDuplicate && forceImportDuplicates) "${record.name}_${UUID.randomUUID().toString().substring(0, 4)}" else record.name,
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

            tempDir.deleteRecursively()
            Result.success(importedCount)
        } catch (e: Exception) {
            Log.e(TAG, "Import failed", e)
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
            collectibles.add(CollectibleRecord(
                id = item.optLong("id"), name = item.optString("name"),
                category = item.optString("category"), type = item.optString("type"),
                ipName = item.optString("ipName"), seriesName = item.optString("seriesName"),
                characterTag = item.optString("characterTag"), remark = item.optString("remark"),
                purchaseChannel = item.optString("purchaseChannel"), purchaseShop = item.optString("purchaseShop"),
                purchaseDate = item.optLong("purchaseDate"), purchasePrice = item.optDouble("purchasePrice"),
                purchaseQuantity = item.optInt("purchaseQuantity"), purchaseShipping = item.optDouble("purchaseShipping"),
                expectedPrice = item.optDouble("expectedPrice"),
                sellPrice = if (item.has("sellPrice")) item.optDouble("sellPrice") else null,
                sellQuantity = if (item.has("sellQuantity")) item.optInt("sellQuantity") else null,
                sellShipping = if (item.has("sellShipping")) item.optDouble("sellShipping") else null,
                isFreeShipping = item.optBoolean("isFreeShipping"),
                sellDate = if (item.has("sellDate")) item.optLong("sellDate") else null,
                buyerInfo = if (item.has("buyerInfo")) item.optString("buyerInfo") else null,
                sellRemark = if (item.has("sellRemark")) item.optString("sellRemark") else null,
                status = item.optString("status"), storageStatus = item.optString("storageStatus"),
                imageFilenames = (0 until item.optJSONArray("imageFilenames").length())
                    .map { item.optJSONArray("imageFilenames").getString(it) },
                createdAt = item.optLong("createdAt"), updatedAt = item.optLong("updatedAt")
            ))
        }
        return Manifest(version, timestamp, collectibles)
    }
}
