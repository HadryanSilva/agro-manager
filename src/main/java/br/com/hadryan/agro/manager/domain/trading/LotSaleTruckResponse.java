package br.com.hadryan.agro.manager.domain.trading;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Resposta de um caminhão de venda.
 */
public record LotSaleTruckResponse(
        UUID id,
        String truckPlate,
        BigDecimal quantityKg,
        String notes
) {
    public static LotSaleTruckResponse from(LotSaleTruck t) {
        return new LotSaleTruckResponse(t.getId(), t.getTruckPlate(), t.getQuantityKg(), t.getNotes());
    }
}
