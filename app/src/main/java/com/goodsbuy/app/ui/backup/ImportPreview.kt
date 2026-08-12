package com.goodsbuy.app.ui.backup

import com.goodsbuy.app.util.CollectibleRecord

enum class ImportAction { IMPORT, SKIP }

data class ImportPreviewItem(
    val record: CollectibleRecord,
    val action: ImportAction,
    val reason: String = ""
)

data class ImportPreviewResult(
    val items: List<ImportPreviewItem>,
    val total: Int,
    val willImport: Int,
    val willSkip: Int
)
