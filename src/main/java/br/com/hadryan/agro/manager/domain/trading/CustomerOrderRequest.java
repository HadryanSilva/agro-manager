package br.com.hadryan.agro.manager.domain.trading;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CustomerOrderRequest(

        @NotBlank(message = "Nome do cliente é obrigatório")
        String customerName,

        String customerPhone,

        String customerDocument,

        @NotNull(message = "Quantidade em Kg é obrigatória")
        @DecimalMin(value = "0.0001", message = "Quantidade deve ser maior que zero")
        BigDecimal quantityKg,

        BigDecimal pricePerKg,

        @NotBlank(message = "Produto é obrigatório")
        String product,

        @NotNull(message = "Data do pedido é obrigatória")
        LocalDate orderDate,

        LocalDate deliveryDeadline,

        String notes
) {
}
