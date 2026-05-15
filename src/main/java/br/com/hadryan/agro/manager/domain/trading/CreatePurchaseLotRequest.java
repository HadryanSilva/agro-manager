package br.com.hadryan.agro.manager.domain.trading;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Dados para criação de um lote de compra.
 * Diferente de PurchaseLotRequest (usado em updates), exige customerOrderId.
 */
public record CreatePurchaseLotRequest(

        @NotNull(message = "Pedido do cliente é obrigatório")
        UUID customerOrderId,

        @NotNull(message = "Fornecedor é obrigatório")
        UUID supplierId,

        @NotNull(message = "Data da compra é obrigatória")
        LocalDate purchaseDate,

        @NotNull(message = "Preço por Kg é obrigatório")
        @DecimalMin(value = "0.0001", message = "Preço por Kg deve ser maior que zero")
        BigDecimal pricePerKg,

        @NotEmpty(message = "Informe ao menos um caminhão")
        @Valid
        List<PurchaseTruckRequest> trucks,

        String notes
) {
}
