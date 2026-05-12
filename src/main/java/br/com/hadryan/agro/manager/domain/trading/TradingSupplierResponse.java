package br.com.hadryan.agro.manager.domain.trading;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Resposta de fornecedor com totais calculados em memória.
 */
public record TradingSupplierResponse(
        UUID id,
        String name,
        String phone,
        String city,
        String notes,
        LocalDateTime createdAt
) {
    public static TradingSupplierResponse from(TradingSupplier s) {
        return new TradingSupplierResponse(
                s.getId(), s.getName(), s.getPhone(),
                s.getCity(), s.getNotes(), s.getCreatedAt()
        );
    }
}