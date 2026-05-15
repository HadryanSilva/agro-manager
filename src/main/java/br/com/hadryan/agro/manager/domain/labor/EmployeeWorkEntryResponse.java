package br.com.hadryan.agro.manager.domain.labor;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record EmployeeWorkEntryResponse(
        UUID id,
        UUID employeeId,
        String employeeName,
        UUID farmId,
        String farmName,
        LocalDate workDate,
        BigDecimal dailyRate,
        boolean paid,
        UUID paymentId,
        String notes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static EmployeeWorkEntryResponse from(EmployeeWorkEntry entry) {
        return new EmployeeWorkEntryResponse(
                entry.getId(),
                entry.getEmployee().getId(),
                entry.getEmployee().getName(),
                entry.getFarm() != null ? entry.getFarm().getId() : null,
                entry.getFarm() != null ? entry.getFarm().getName() : null,
                entry.getWorkDate(),
                entry.getDailyRate(),
                entry.isPaid(),
                entry.getPaymentId(),
                entry.getNotes(),
                entry.getCreatedAt(),
                entry.getUpdatedAt()
        );
    }
}
