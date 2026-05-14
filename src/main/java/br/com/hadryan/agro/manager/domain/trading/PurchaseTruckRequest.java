package br.com.hadryan.agro.manager.domain.trading;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Dados de um caminhão de compra (enviado dentro do lote).
 */
public record PurchaseTruckRequest(
        @NotBlank(message = "Placa do caminhão é obrigatória")
        @Size(max = 10, message = "Placa deve ter no máximo 10 caracteres")
        String truckPlate,

        @NotNull(message = "Quantidade em Kg é obrigatória")
        @DecimalMin(value = "0.01", message = "Quantidade deve ser maior que zero")
        BigDecimal quantityKg,

        @DecimalMin(value = "0.00", inclusive = true, message = "Valor do frete não pode ser negativo")
        BigDecimal freightValue,

        String notes
) {
}
