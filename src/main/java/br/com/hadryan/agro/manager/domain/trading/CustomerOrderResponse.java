package br.com.hadryan.agro.manager.domain.trading;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record CustomerOrderResponse(
        UUID id,
        String customerName,
        String customerPhone,
        String customerDocument,
        BigDecimal quantityKg,
        BigDecimal pricePerKg,
        String product,
        LocalDate orderDate,
        LocalDate deliveryDeadline,
        CustomerOrderStatus status,
        String notes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static CustomerOrderResponse from(CustomerOrder o, CustomerOrderStatus status) {
        return new CustomerOrderResponse(
                o.getId(),
                o.getCustomerName(),
                o.getCustomerPhone(),
                o.getCustomerDocument(),
                o.getQuantityKg(),
                o.getPricePerKg(),
                o.getProduct(),
                o.getOrderDate(),
                o.getDeliveryDeadline(),
                status,
                o.getNotes(),
                o.getCreatedAt(),
                o.getUpdatedAt()
        );
    }
}
