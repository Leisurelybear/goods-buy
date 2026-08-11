package com.graincabinet.app.domain.usecase

import com.graincabinet.app.domain.calculator.ProfitLossCalculator
import com.graincabinet.app.domain.model.Collectible
import com.graincabinet.app.domain.model.ProfitLoss
import javax.inject.Inject

class CalculateProfitLossUseCase @Inject constructor(
    private val calculator: ProfitLossCalculator
) {
    operator fun invoke(collectible: Collectible): ProfitLoss = calculator.calculate(collectible)
}
