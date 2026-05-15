package br.com.hadryan.agro.manager.domain.trading;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record OrderSupplierLegRequest(
        @NotNull(message = "Fornecedor é obrigatório")
        UUID supplierId,

        @NotNull(message = "Preço por kg do fornecedor é obrigatório")
        @DecimalMin(value = "0.0001", message = "Preço por kg deve ser maior que zero")
        BigDecimal supplierPricePerKg,

        @NotNull
        @Size(min = 1, message = "Cada perna deve ter ao menos um caminhão")
        List<@Valid OrderTruckRequest> trucks,

        String notes
) {
}
