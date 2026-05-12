package br.com.hadryan.agro.manager.domain.trading;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Dados para criação ou atualização de uma venda de lote.
 * Os caminhões de entrega são enviados junto à venda.
 */
public record LotSaleRequest(
        @NotBlank(message = "Nome do comprador é obrigatório")
        @Size(max = 150, message = "Nome do comprador deve ter no máximo 150 caracteres")
        String buyerName,

        @NotNull(message = "Data da venda é obrigatória")
        LocalDate saleDate,

        @NotNull(message = "Preço por Kg é obrigatório")
        @DecimalMin(value = "0.0001", message = "Preço por Kg deve ser maior que zero")
        BigDecimal pricePerKg,

        @NotEmpty(message = "Informe ao menos um caminhão")
        @Valid
        List<LotSaleTruckRequest> trucks,

        String notes
) {}
