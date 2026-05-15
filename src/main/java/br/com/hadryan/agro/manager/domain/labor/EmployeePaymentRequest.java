package br.com.hadryan.agro.manager.domain.labor;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record EmployeePaymentRequest(
        @NotNull(message = "Funcionario e obrigatorio")
        UUID employeeId,

        @NotNull(message = "Inicio do periodo e obrigatorio")
        LocalDate periodStart,

        @NotNull(message = "Fim do periodo e obrigatorio")
        LocalDate periodEnd,

        @NotNull(message = "Data de pagamento e obrigatoria")
        LocalDate paymentDate,

        String notes
) {
}
