package br.com.hadryan.agro.manager.domain.trading;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record PurchaseLotSummaryResponse(
        UUID id,
        UUID supplierId,
        String supplierName,
        UUID customerOrderId,
        String customerName,
        LocalDate purchaseDate,
        BigDecimal pricePerKg,
        PurchaseLotStatus status,
        BigDecimal totalPurchasedKg,
        BigDecimal totalSoldKg,
        BigDecimal remainingKg,
        BigDecimal totalCost,
        String notes,
        LocalDateTime createdAt
) {
    public static PurchaseLotSummaryResponse from(PurchaseLot lot,
                                                  BigDecimal totalPurchasedKg,
                                                  BigDecimal totalSoldKg) {
        BigDecimal remaining = totalPurchasedKg.subtract(totalSoldKg);
        BigDecimal totalCost = totalPurchasedKg.multiply(lot.getPricePerKg());

        return new PurchaseLotSummaryResponse(
                lot.getId(),
                lot.getSupplier().getId(),
                lot.getSupplier().getName(),
                lot.getCustomerOrder().getId(),
                lot.getCustomerOrder().getCustomerName(),
                lot.getPurchaseDate(),
                lot.getPricePerKg(),
                lot.getStatus(),
                totalPurchasedKg,
                totalSoldKg,
                remaining.max(BigDecimal.ZERO),
                totalCost,
                lot.getNotes(),
                lot.getCreatedAt()
        );
    }
}
