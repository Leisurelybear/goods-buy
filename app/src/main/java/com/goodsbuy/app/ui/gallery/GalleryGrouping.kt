package com.goodsbuy.app.ui.gallery

import com.goodsbuy.app.domain.model.Collectible

const val UNCATEGORIZED_NAME = "未分类"

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
        .sortedWith(Comparator { left, right ->
            val sizeOrder = right.value.size.compareTo(left.value.size)
            if (sizeOrder != 0) sizeOrder else left.key.compareTo(right.key)
        })
        .map { (name, items) ->
            GalleryGroup(name, items.size, items.sortedWith(itemComparator(groupBy)))
        }
    return if (uncategorized != null) {
        grouped + GalleryGroup(
            UNCATEGORIZED_NAME,
            uncategorized.size,
            uncategorized.sortedWith(itemComparator(groupBy))
        )
    } else {
        grouped
    }
}

private fun itemComparator(groupBy: GroupBy): Comparator<Collectible> = when (groupBy) {
    GroupBy.IP -> compareBy<Collectible> { it.seriesName.trim() }
        .thenByDescending { it.createdAt }
        .thenBy { it.name.trim() }
    GroupBy.SERIES -> compareBy<Collectible> { it.name.trim() }
        .thenByDescending { it.createdAt }
        .thenBy { it.characterTag.trim() }
}
