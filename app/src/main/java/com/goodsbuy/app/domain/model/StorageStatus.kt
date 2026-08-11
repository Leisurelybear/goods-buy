package com.goodsbuy.app.domain.model

enum class StorageStatus(val displayName: String) {
    IN_STOCK("现货"), IN_TRANSIT("在途"), GROUP_STORAGE("团长囤货"), AGENT_STORAGE("代购处囤货");
    companion object {
        fun fromKey(key: String): StorageStatus = entries.firstOrNull { it.name == key } ?: IN_STOCK
    }
}
