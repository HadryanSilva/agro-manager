package br.com.hadryan.agro.manager.domain.trading;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ClientOrderRequest(
        @NotNull(message = "Cliente é obrigatório")
        UUID clientId,

        @NotNull(message = "Data do pedido é obrigatória")
        LocalDate orderDate,

        @NotNull(message = "Preço por kg do cliente é obrigatório")
        @DecimalMin(value = "0.0001", message = "Preço por kg deve ser maior que zero")
        BigDecimal clientPricePerKg,

        @NotNull
        @Size(min = 1, message = "O pedido deve ter ao menos uma perna de fornecedor")
        List<@Valid OrderSupplierLegRequest> legs,

        String notes
) {
}
