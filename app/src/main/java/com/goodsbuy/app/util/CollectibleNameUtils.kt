package com.goodsbuy.app.util

object CollectibleNameUtils {

    private val timestampSuffix = Regex("""\s+\d{4}-\d{2}-\d{2}\s+\d{2}:\d{2}$""")

    fun buildDuplicateName(originalName: String, timestamp: String): String {
        return if (timestampSuffix.containsMatchIn(originalName)) {
            originalName.replace(timestampSuffix) { " $timestamp" }
        } else {
            "$originalName $timestamp"
        }
    }
}