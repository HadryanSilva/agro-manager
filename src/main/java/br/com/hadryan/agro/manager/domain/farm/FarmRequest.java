package br.com.hadryan.agro.manager.domain.farm;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Dados para criação ou atualização de uma lavoura.
 * As datas de plantio e colheita são opcionais — preenchidas conforme o andamento.
 */
public record FarmRequest(

        @NotBlank(message = "Nome da lavoura é obrigatório")
        @Size(max = 150, message = "Nome deve ter no máximo 150 caracteres")
        String name,

        @NotNull(message = "Área é obrigatória")
        @DecimalMin(value = "0.01", message = "Área deve ser maior que zero")
        BigDecimal areaValue,

        @NotNull(message = "Unidade de área é obrigatória")
        AreaUnit areaUnit,

        // Dados do arrendamento — todos opcionais
        String lessorName,
        LocalDate leaseStartDate,
        LocalDate leaseEndDate,
        BigDecimal leaseValue,

        // Período de plantio — opcionais
        LocalDate plantingStartDate,
        LocalDate plantingEndDate,

        // Período de colheita — opcionais
        LocalDate harvestStartDate,
        LocalDate harvestEndDate,

        boolean cancelled,

        String notes
) {}
