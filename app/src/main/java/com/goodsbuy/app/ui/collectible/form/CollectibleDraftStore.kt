package com.goodsbuy.app.ui.collectible.form

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

data class DraftSummary(val key: String, val id: Long?, val name: String, val updatedAt: Long)

@Singleton
class CollectibleDraftStore @Inject constructor(
    @ApplicationContext context: Context
) {
    private val preferences = context.getSharedPreferences("collectible_drafts", Context.MODE_PRIVATE)

    fun load(key: String): CollectibleFormUiState? {
        val raw = preferences.getString(keyFor(key), null) ?: return null
        return runCatching {
            val obj = JSONObject(raw)
            CollectibleFormUiState(
                id = obj.optNullableLong("id"),
                name = obj.optString("name"), category = obj.optString("category"), type = obj.optString("type", "官方"),
                ipName = obj.optString("ipName"), seriesName = obj.optString("seriesName"), characterTag = obj.optString("characterTag"),
                remark = obj.optString("remark"), purchaseChannel = obj.optString("purchaseChannel"), purchaseShop = obj.optString("purchaseShop"),
                purchaseDate = obj.optLong("purchaseDate", System.currentTimeMillis()), purchasePrice = obj.optString("purchasePrice"),
                purchaseQuantity = obj.optString("purchaseQuantity", "1"), purchaseShipping = obj.optString("purchaseShipping", "0"),
                expectedPrice = obj.optString("expectedPrice"), sellPrice = obj.optString("sellPrice"), sellQuantity = obj.optString("sellQuantity"),
                sellShipping = obj.optString("sellShipping"), isFreeShipping = obj.optBoolean("isFreeShipping"), sellDate = obj.optNullableLong("sellDate"),
                buyerInfo = obj.optString("buyerInfo"), sellRemark = obj.optString("sellRemark"),
                status = runCatching { com.goodsbuy.app.domain.model.OrderStatus.valueOf(obj.optString("status")) }.getOrDefault(com.goodsbuy.app.domain.model.OrderStatus.OWNED),
                storageStatus = runCatching { com.goodsbuy.app.domain.model.StorageStatus.valueOf(obj.optString("storageStatus")) }.getOrDefault(com.goodsbuy.app.domain.model.StorageStatus.IN_STOCK),
                imagePaths = obj.optJSONArray("imagePaths")?.let { array -> (0 until array.length()).map(array::getString) } ?: emptyList(),
                createdAt = obj.optLong("createdAt", System.currentTimeMillis())
            )
        }.getOrNull()
    }

    fun save(key: String, state: CollectibleFormUiState) {
        val obj = JSONObject().apply {
            putNullable("id", state.id); put("name", state.name); put("category", state.category); put("type", state.type)
            put("ipName", state.ipName); put("seriesName", state.seriesName); put("characterTag", state.characterTag); put("remark", state.remark)
            put("purchaseChannel", state.purchaseChannel); put("purchaseShop", state.purchaseShop); put("purchaseDate", state.purchaseDate)
            put("purchasePrice", state.purchasePrice); put("purchaseQuantity", state.purchaseQuantity); put("purchaseShipping", state.purchaseShipping)
            put("expectedPrice", state.expectedPrice); put("sellPrice", state.sellPrice); put("sellQuantity", state.sellQuantity); put("sellShipping", state.sellShipping)
            put("isFreeShipping", state.isFreeShipping); putNullable("sellDate", state.sellDate); put("buyerInfo", state.buyerInfo); put("sellRemark", state.sellRemark)
            put("status", state.status.name); put("storageStatus", state.storageStatus.name); put("imagePaths", JSONArray(state.imagePaths)); put("createdAt", state.createdAt)
            put("updatedAt", System.currentTimeMillis())
        }
        preferences.edit { putString(keyFor(key), obj.toString()) }
    }

    fun delete(key: String) { preferences.edit { remove(keyFor(key)) } }
    fun exists(key: String): Boolean = preferences.contains(keyFor(key))

    fun list(): List<DraftSummary> = preferences.all.keys
        .filter { it.startsWith("draft_") }
        .mapNotNull { storedKey ->
            val key = storedKey.removePrefix("draft_")
            val state = load(key) ?: return@mapNotNull null
            val raw = preferences.getString(storedKey, null) ?: return@mapNotNull null
            val updatedAt = runCatching { JSONObject(raw).optLong("updatedAt", 0L) }.getOrDefault(0L)
            DraftSummary(key, state.id, state.name.ifBlank { "未命名草稿" }, updatedAt)
        }
        .sortedByDescending { it.updatedAt }

    fun deleteAll() { preferences.edit { preferences.all.keys.filter { it.startsWith("draft_") }.forEach(::remove) } }

    private fun keyFor(key: String) = "draft_$key"
    private fun JSONObject.putNullable(name: String, value: Any?) { if (value == null) put(name, JSONObject.NULL) else put(name, value) }
    private fun JSONObject.optNullableLong(name: String): Long? = if (has(name) && !isNull(name)) optLong(name) else null
}
