package br.com.hadryan.agro.manager.domain.quotation;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Payload de entrada para criação e atualização de cotações.
 */
public record QuotationRequest(

        @NotBlank(message = "Nome do produto é obrigatório")
        @Size(max = 200)
        String productName,

        @NotBlank(message = "Fornecedor é obrigatório")
        @Size(max = 200)
        String supplier,

        @NotNull(message = "Preço unitário é obrigatório")
        @DecimalMin(value = "0.01", message = "Preço deve ser maior que zero")
        BigDecimal unitPrice,

        @NotNull(message = "Quantidade é obrigatória")
        @DecimalMin(value = "0.001", message = "Quantidade deve ser maior que zero")
        BigDecimal quantity,

        @Size(max = 50)
        String unit,

        @NotNull(message = "Data da cotação é obrigatória")
        LocalDate quotationDate,

        String notes,

        // Lavoura opcional
        UUID farmId
) {}