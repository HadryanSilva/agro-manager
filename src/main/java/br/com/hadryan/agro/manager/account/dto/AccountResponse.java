package br.com.hadryan.agro.manager.account.dto;

import br.com.hadryan.agro.manager.account.domain.Account;
import br.com.hadryan.agro.manager.account.domain.AccountStatus;
import java.time.Instant;

/**
 * Projection of {@link Account} for API responses. Using a record here
 * decouples the wire format from the entity and prevents accidentally
 * exposing JPA internals (lazy relationships, version field) through
 * Jackson.
 */
public record AccountResponse(
        Long id,
        String name,
        AccountStatus status,
        Instant createdAt,
        Instant updatedAt
) {

    public static AccountResponse from(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getName(),
                account.getStatus(),
                account.getCreatedAt(),
                account.getUpdatedAt()
        );
    }
}