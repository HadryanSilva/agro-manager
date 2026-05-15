package br.com.hadryan.agro.manager.domain.labor;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record EmployeeRequest(
        @NotBlank(message = "Nome e obrigatorio")
        @Size(max = 150, message = "Nome deve ter no maximo 150 caracteres")
        String name,

        @NotNull(message = "Valor da diaria e obrigatorio")
        @DecimalMin(value = "0.01", message = "Valor da diaria deve ser maior que zero")
        BigDecimal dailyRate,

        String notes
) {
}
