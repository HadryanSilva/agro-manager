package br.com.hadryan.agro.manager.domain.expense;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Payload de entrada para criação e atualização de despesas.
 * farmId é opcional — quando nulo, a despesa é geral da conta.
 * paymentDate nulo indica despesa a pagar; preenchido indica pago.
 */
public record ExpenseRequest(

        @NotBlank(message = "Descrição é obrigatória")
        @Size(max = 200, message = "Descrição deve ter no máximo 200 caracteres")
        String description,

        @NotNull(message = "Categoria é obrigatória")
        ExpenseCategory category,

        @NotNull(message = "Valor é obrigatório")
        @DecimalMin(value = "0.01", message = "Valor deve ser maior que zero")
        BigDecimal value,

        @NotNull(message = "Data de competência é obrigatória")
        LocalDate competenceDate,

        // Opcional — preenchido quando o pagamento já foi realizado
        LocalDate paymentDate,

        String notes,

        // Opcional — null indica despesa geral da conta (sem lavoura vinculada)
        UUID farmId
) {}