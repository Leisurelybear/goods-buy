package com.goodsbuy.app.util

import java.util.Locale

/** 金额展示：整数金额不带小数（¥120），非整数保留两位（¥12.99）。 */
fun formatPrice(value: Double): String =
    if (value % 1.0 == 0.0) {
        String.format(Locale.getDefault(), "%.0f", value)
    } else {
        String.format(Locale.getDefault(), "%.2f", value)
    }
