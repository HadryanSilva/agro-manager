package br.com.hadryan.agro.manager.domain.quotation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Representa uma cotação individual.
 */
public record QuotationResponse(
        UUID id,
        String productName,
        String supplier,
        BigDecimal unitPrice,
        BigDecimal quantity,
        String unit,
        BigDecimal totalPrice,
        LocalDate quotationDate,
        String notes,
        UUID farmId,
        String farmName,
        LocalDateTime createdAt
) {
    public static QuotationResponse from(Quotation q) {
        return new QuotationResponse(
                q.getId(),
                q.getProductName(),
                q.getSupplier(),
                q.getUnitPrice(),
                q.getQuantity(),
                q.getUnit(),
                q.getTotalPrice(),
                q.getQuotationDate(),
                q.getNotes(),
                q.getFarm() != null ? q.getFarm().getId() : null,
                q.getFarm() != null ? q.getFarm().getName() : null,
                q.getCreatedAt()
        );
    }
}