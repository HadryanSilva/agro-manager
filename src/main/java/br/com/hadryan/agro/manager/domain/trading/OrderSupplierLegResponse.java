package br.com.hadryan.agro.manager.domain.trading;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record OrderSupplierLegResponse(
        UUID id,
        UUID supplierId,
        String supplierName,
        String supplierCity,
        BigDecimal supplierPricePerKg,
        List<OrderTruckResponse> trucks,
        BigDecimal totalKg,
        BigDecimal totalCost,
        String notes
) {
    public static OrderSupplierLegResponse from(OrderSupplierLeg leg) {
        List<OrderTruckResponse> truckResponses = leg.getTrucks().stream()
                .map(OrderTruckResponse::from).toList();

        BigDecimal totalKg = leg.getTrucks().stream()
                .map(OrderTruck::getQuantityKg)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCost = leg.getTrucks().stream()
                .map(t -> {
                    BigDecimal product = t.getQuantityKg().multiply(leg.getSupplierPricePerKg());
                    BigDecimal freight = t.getFreightValue() != null ? t.getFreightValue() : BigDecimal.ZERO;
                    return product.add(freight);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new OrderSupplierLegResponse(
                leg.getId(),
                leg.getSupplier().getId(),
                leg.getSupplier().getName(),
                leg.getSupplier().getCity(),
                leg.getSupplierPricePerKg(),
                truckResponses,
                totalKg,
                totalCost,
                leg.getNotes()
        );
    }
}
