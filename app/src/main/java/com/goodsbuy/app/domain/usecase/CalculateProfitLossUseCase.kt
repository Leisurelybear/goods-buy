package com.goodsbuy.app.domain.usecase

import com.goodsbuy.app.domain.calculator.ProfitLossCalculator
import com.goodsbuy.app.domain.model.Collectible
import com.goodsbuy.app.domain.model.ProfitLoss
import javax.inject.Inject

class CalculateProfitLossUseCase @Inject constructor(
    private val calculator: ProfitLossCalculator
) {
    operator fun invoke(collectible: Collectible): ProfitLoss = calculator.calculate(collectible)
}
