package com.goodsbuy.app.domain.model

/**
 * Per-category (IP / series / category) accounting breakdown.
 *
 * The fields deliberately separate "everything bought" from "already settled"
 * so the UI can show a 投入 number and a 利润率 that actually reconcile:
 * [profit] is always `revenue - realizedCost`, and the profit rate uses
 * [realizedCost] as its denominator (matching [ProfitLoss]).
 */
data class CategoryStat(
    val categoryName: String,
    val count: Int,
    /** 全部购入成本（购入单价 × 数量 + 购入运费），含尚未售出的部分。 */
    val grossInvestment: Double,
    /** 已结算部分（已售/赠品/遗失）的成本，即利润率分母。 */
    val realizedCost: Double,
    /** 未结算部分按预期单价 × 剩余数量估算的持仓市值。 */
    val holdingValue: Double,
    /** 已实现收入（仅已售出部分）。 */
    val revenue: Double,
    /** 已实现利润 = revenue - realizedCost。 */
    val profit: Double
)
