package br.com.hadryan.agro.manager.domain.labor;

import java.math.BigDecimal;
import java.util.UUID;

public record GeneratedExpenseResponse(
        UUID expenseId,
        UUID farmId,
        String farmName,
        BigDecimal amount
) {
}
