package br.com.hadryan.agro.manager.domain.expense;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Representação completa de uma despesa retornada ao cliente.
 * O campo paid é derivado da presença de paymentDate.
 */
public record ExpenseResponse(
        UUID id,
        String description,
        ExpenseCategory category,
        BigDecimal value,
        LocalDate competenceDate,
        LocalDate paymentDate,
        boolean paid,
        String notes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ExpenseResponse from(Expense expense) {
        return new ExpenseResponse(
                expense.getId(),
                expense.getDescription(),
                expense.getCategory(),
                expense.getValue(),
                expense.getCompetenceDate(),
                expense.getPaymentDate(),
                expense.isPaid(),
                expense.getNotes(),
                expense.getCreatedAt(),
                expense.getUpdatedAt()
        );
    }
}