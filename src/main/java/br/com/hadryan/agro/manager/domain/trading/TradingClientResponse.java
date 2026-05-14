package br.com.hadryan.agro.manager.domain.trading;

import java.time.LocalDateTime;
import java.util.UUID;

public record TradingClientResponse(
        UUID id,
        String name,
        String phone,
        String city,
        String notes,
        LocalDateTime createdAt
) {
    public static TradingClientResponse from(TradingClient c) {
        return new TradingClientResponse(
                c.getId(), c.getName(), c.getPhone(), c.getCity(), c.getNotes(), c.getCreatedAt()
        );
    }
}
