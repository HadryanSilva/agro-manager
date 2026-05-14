package br.com.hadryan.agro.manager.domain.trading;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record ClientOrderSummaryResponse(
        UUID id,
        UUID clientId,
        String clientName,
        LocalDate orderDate,
        BigDecimal clientPricePerKg,
        ClientOrderStatus status,
        BigDecimal totalKg,
        BigDecimal totalRevenue,
        BigDecimal totalCost,
        BigDecimal grossMargin,
        String notes,
        LocalDateTime createdAt
) {
    public static ClientOrderSummaryResponse from(ClientOrder order, BigDecimal totalKg, BigDecimal totalCost) {
        BigDecimal totalRevenue = totalKg.multiply(order.getClientPricePerKg());
        return new ClientOrderSummaryResponse(
                order.getId(),
                order.getClient().getId(),
                order.getClient().getName(),
                order.getOrderDate(),
                order.getClientPricePerKg(),
                order.getStatus(),
                totalKg,
                totalRevenue,
                totalCost,
                totalRevenue.subtract(totalCost),
                order.getNotes(),
                order.getCreatedAt()
        );
    }
}
