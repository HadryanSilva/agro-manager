package br.com.hadryan.agro.manager.domain.labor;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record EmployeeWorkEntryRequest(
        @NotNull(message = "Funcionario e obrigatorio")
        UUID employeeId,

        UUID farmId,

        @NotNull(message = "Data de trabalho e obrigatoria")
        LocalDate workDate,

        @DecimalMin(value = "0.01", message = "Valor da diaria deve ser maior que zero")
        BigDecimal dailyRate,

        String notes
) {
}
