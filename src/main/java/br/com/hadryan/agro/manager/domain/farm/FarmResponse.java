package br.com.hadryan.agro.manager.domain.farm;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Representação completa da lavoura retornada ao cliente.
 * Inclui o status calculado dinamicamente.
 */
public record FarmResponse(
        UUID id,
        String name,
        BigDecimal areaValue,
        AreaUnit areaUnit,
        String lessorName,
        LocalDate leaseStartDate,
        LocalDate leaseEndDate,
        BigDecimal leaseValue,
        LocalDate plantingStartDate,
        LocalDate plantingEndDate,
        LocalDate harvestStartDate,
        LocalDate harvestEndDate,
        boolean cancelled,
        String notes,
        FarmStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static FarmResponse from(Farm farm) {
        return new FarmResponse(
                farm.getId(),
                farm.getName(),
                farm.getAreaValue(),
                farm.getAreaUnit(),
                farm.getLessorName(),
                farm.getLeaseStartDate(),
                farm.getLeaseEndDate(),
                farm.getLeaseValue(),
                farm.getPlantingStartDate(),
                farm.getPlantingEndDate(),
                farm.getHarvestStartDate(),
                farm.getHarvestEndDate(),
                farm.isCancelled(),
                farm.getNotes(),
                farm.getStatus(),
                farm.getCreatedAt(),
                farm.getUpdatedAt()
        );
    }
}