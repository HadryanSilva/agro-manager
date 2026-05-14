package br.com.hadryan.agro.manager.domain.trading;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ClientOrderDetailResponse(
        UUID id,
        UUID clientId,
        String clientName,
        String clientPhone,
        LocalDate orderDate,
        BigDecimal clientPricePerKg,
        ClientOrderStatus status,
        List<OrderSupplierLegResponse> legs,
        BigDecimal totalKg,
        BigDecimal totalRevenue,
        BigDecimal totalCost,
        BigDecimal grossMargin,
        String notes,
        LocalDateTime createdAt
) {
    public static ClientOrderDetailResponse from(ClientOrder order) {
        List<OrderSupplierLegResponse> legResponses = order.getLegs().stream()
                .map(OrderSupplierLegResponse::from).toList();

        BigDecimal totalKg = legResponses.stream()
                .map(OrderSupplierLegResponse::totalKg)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalRevenue = totalKg.multiply(order.getClientPricePerKg());

        BigDecimal totalCost = legResponses.stream()
                .map(OrderSupplierLegResponse::totalCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new ClientOrderDetailResponse(
                order.getId(),
                order.getClient().getId(),
                order.getClient().getName(),
                order.getClient().getPhone(),
                order.getOrderDate(),
                order.getClientPricePerKg(),
                order.getStatus(),
                legResponses,
                totalKg,
                totalRevenue,
                totalCost,
                totalRevenue.subtract(totalCost),
                order.getNotes(),
                order.getCreatedAt()
        );
    }
}
