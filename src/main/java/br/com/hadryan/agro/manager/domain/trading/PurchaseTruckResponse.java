package br.com.hadryan.agro.manager.domain.trading;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Resposta de um caminhão de compra.
 */
public record PurchaseTruckResponse(
        UUID id,
        String truckPlate,
        BigDecimal quantityKg,
        BigDecimal freightValue,
        String notes
) {
    public static PurchaseTruckResponse from(PurchaseTruck t) {
        return new PurchaseTruckResponse(
                t.getId(),
                t.getTruckPlate(),
                t.getQuantityKg(),
                t.getFreightValue(),
                t.getNotes()
        );
    }
}
