package br.com.hadryan.agro.manager.domain.labor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record EmployeePaymentResponse(
        UUID id,
        UUID employeeId,
        String employeeName,
        LocalDate periodStart,
        LocalDate periodEnd,
        LocalDate paymentDate,
        BigDecimal totalAmount,
        long paidEntriesCount,
        String notes,
        List<GeneratedExpenseResponse> generatedExpenses,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static EmployeePaymentResponse from(
            EmployeePayment payment,
            long paidEntriesCount,
            List<GeneratedExpenseResponse> generatedExpenses) {
        return new EmployeePaymentResponse(
                payment.getId(),
                payment.getEmployee().getId(),
                payment.getEmployee().getName(),
                payment.getPeriodStart(),
                payment.getPeriodEnd(),
                payment.getPaymentDate(),
                payment.getTotalAmount(),
                paidEntriesCount,
                payment.getNotes(),
                generatedExpenses,
                payment.getCreatedAt(),
                payment.getUpdatedAt()
        );
    }
}
