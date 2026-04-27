package br.com.hadryan.agro.manager.domain.expense;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Projeção de despesa enriquecida com dados da lavoura.
 * Usada na listagem consolidada de transações da conta.
 */
public record TransactionResponse(
        UUID id,
        UUID farmId,
        String farmName,
        String description,
        ExpenseCategory category,
        BigDecimal value,
        LocalDate competenceDate,
        LocalDate paymentDate,
        boolean paid,
        String notes,
        LocalDateTime createdAt
) {
    public static TransactionResponse from(Expense expense) {
        return new TransactionResponse(
                expense.getId(),
                expense.getFarm().getId(),
                expense.getFarm().getName(),
                expense.getDescription(),
                expense.getCategory(),
                expense.getValue(),
                expense.getCompetenceDate(),
                expense.getPaymentDate(),
                expense.isPaid(),
                expense.getNotes(),
                expense.getCreatedAt()
        );
    }
}