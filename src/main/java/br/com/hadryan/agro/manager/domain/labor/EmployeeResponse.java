package br.com.hadryan.agro.manager.domain.labor;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record EmployeeResponse(
        UUID id,
        String name,
        BigDecimal dailyRate,
        boolean active,
        String notes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static EmployeeResponse from(Employee employee) {
        return new EmployeeResponse(
                employee.getId(),
                employee.getName(),
                employee.getDailyRate(),
                employee.isActive(),
                employee.getNotes(),
                employee.getCreatedAt(),
                employee.getUpdatedAt()
        );
    }
}
