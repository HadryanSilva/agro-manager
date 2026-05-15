package br.com.hadryan.agro.manager.domain.trading;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderTruckResponse(
        UUID id,
        String truckPlate,
        BigDecimal quantityKg,
        BigDecimal freightValue,
        String notes
) {
    public static OrderTruckResponse from(OrderTruck t) {
        return new OrderTruckResponse(
                t.getId(), t.getTruckPlate(), t.getQuantityKg(), t.getFreightValue(), t.getNotes()
        );
    }
}
