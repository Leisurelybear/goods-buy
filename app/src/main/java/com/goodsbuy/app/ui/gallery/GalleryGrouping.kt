package com.goodsbuy.app.ui.gallery

import com.goodsbuy.app.domain.model.Collectible

const val UNCATEGORIZED_NAME = "未分类"

private val GROUP_ITEM_COMPARATOR =
    compareBy<Collectible> { it.seriesName }.thenByDescending { it.createdAt }

fun groupCollectibles(collectibles: List<Collectible>, groupBy: GroupBy): List<GalleryGroup> {
    val buckets = LinkedHashMap<String, MutableList<Collectible>>()
    collectibles.forEach { c ->
        val raw = when (groupBy) {
            GroupBy.IP -> c.ipName
            GroupBy.SERIES -> c.seriesName
        }
        val key = raw.trim().ifBlank { UNCATEGORIZED_NAME }
        buckets.getOrPut(key) { mutableListOf() }.add(c)
    }
    val uncategorized = buckets.remove(UNCATEGORIZED_NAME)
    val grouped = buckets.entries
        .sortedByDescending { it.value.size }
        .map { (name, items) ->
            GalleryGroup(name, items.size, items.sortedWith(GROUP_ITEM_COMPARATOR))
        }
    return if (uncategorized != null) {
        grouped + GalleryGroup(
            UNCATEGORIZED_NAME,
            uncategorized.size,
            uncategorized.sortedWith(GROUP_ITEM_COMPARATOR)
        )
    } else {
        grouped
    }
}
