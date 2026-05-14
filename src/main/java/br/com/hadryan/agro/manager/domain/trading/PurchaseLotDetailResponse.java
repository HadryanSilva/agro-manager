package br.com.hadryan.agro.manager.domain.trading;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record PurchaseLotDetailResponse(
        UUID id,
        UUID supplierId,
        String supplierName,
        String supplierCity,
        UUID customerOrderId,
        String customerName,
        LocalDate purchaseDate,
        BigDecimal pricePerKg,
        PurchaseLotStatus status,
        List<PurchaseTruckResponse> purchaseTrucks,
        List<LotSaleResponse> sales,
        BigDecimal totalPurchasedKg,
        BigDecimal totalSoldKg,
        BigDecimal remainingKg,
        BigDecimal totalCost,
        BigDecimal totalRevenue,
        BigDecimal grossMargin,
        String notes,
        LocalDateTime createdAt
) {
    public static PurchaseLotDetailResponse from(PurchaseLot lot, List<LotSale> sales) {
        List<PurchaseTruckResponse> truckResponses = lot.getTrucks().stream()
                .map(PurchaseTruckResponse::from)
                .toList();

        List<LotSaleResponse> saleResponses = sales.stream()
                .map(LotSaleResponse::from)
                .toList();

        BigDecimal totalPurchasedKg = truckResponses.stream()
                .map(PurchaseTruckResponse::quantityKg)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalSoldKg = saleResponses.stream()
                .map(LotSaleResponse::totalKg)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCost    = totalPurchasedKg.multiply(lot.getPricePerKg());
        BigDecimal totalRevenue = saleResponses.stream()
                .map(LotSaleResponse::totalRevenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal grossMargin = totalRevenue.subtract(totalCost);

        return new PurchaseLotDetailResponse(
                lot.getId(),
                lot.getSupplier().getId(),
                lot.getSupplier().getName(),
                lot.getSupplier().getCity(),
                lot.getCustomerOrder().getId(),
                lot.getCustomerOrder().getCustomerName(),
                lot.getPurchaseDate(),
                lot.getPricePerKg(),
                lot.getStatus(),
                truckResponses,
                saleResponses,
                totalPurchasedKg,
                totalSoldKg,
                totalSoldKg.compareTo(totalPurchasedKg) >= 0 ? BigDecimal.ZERO : totalPurchasedKg.subtract(totalSoldKg),
                totalCost,
                totalRevenue,
                grossMargin,
                lot.getNotes(),
                lot.getCreatedAt()
        );
    }
}
