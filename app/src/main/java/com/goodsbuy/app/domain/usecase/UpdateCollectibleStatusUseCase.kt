package com.goodsbuy.app.domain.usecase

import com.goodsbuy.app.domain.model.Collectible
import com.goodsbuy.app.domain.model.OrderStatus
import com.goodsbuy.app.domain.repository.CollectibleRepository
import javax.inject.Inject

/**
 * Single entry point for every status transition.
 *
 * Before this existed the list, gallery, detail page and form each applied
 * their own `sellDate` rule, so the same collectible could end up with a
 * different sale month depending on where the user tapped. Keeping the rule
 * here guarantees identical results no matter which surface triggers it.
 *
 * Rules:
 * - entering [OrderStatus.SOLD] without a sale date stamps "now";
 * - staying on [OrderStatus.SOLD] preserves the existing sale date;
 * - leaving [OrderStatus.SOLD] clears the sale date so a later re-sale is
 *   stamped fresh instead of silently reusing a stale month;
 * - [Collectible.updatedAt] is refreshed on every transition.
 */
class UpdateCollectibleStatusUseCase @Inject constructor(
    private val repository: CollectibleRepository
) {

    /** Pure transition used by callers that persist the result themselves. */
    fun migrate(
        collectible: Collectible,
        newStatus: OrderStatus,
        now: Long = System.currentTimeMillis()
    ): Collectible {
        val sellDate = when {
            newStatus != OrderStatus.SOLD -> null
            collectible.sellDate != null -> collectible.sellDate
            else -> now
        }
        return collectible.copy(status = newStatus, sellDate = sellDate, updatedAt = now)
    }

    /** Applies the transition and persists it. Returns the stored collectible. */
    suspend operator fun invoke(collectible: Collectible, newStatus: OrderStatus): Collectible {
        val updated = migrate(collectible, newStatus)
        repository.updateCollectible(updated)
        return updated
    }
}
