package br.com.hadryan.agro.manager.account.domain;

import br.com.hadryan.agro.manager.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Root tenant entity. Every business record (users, crops, transactions…)
 * hangs off an {@code Account} via an {@code account_id} foreign key. The
 * Hibernate filter configured in a later task will enforce tenant isolation
 * automatically, so feature code rarely needs to reference this entity
 * directly.
 *
 * <p>Kept deliberately minimal — add fields (CNPJ, address, phone, etc.)
 * only when a concrete feature requires them. That avoids migrating
 * populated production tables to relax NOT NULL constraints later.
 */
@Entity
@Getter
@Table(name = "accounts")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Account extends BaseEntity {

    @Column(nullable = false, length = 120)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountStatus status;

    private Account(String name, AccountStatus status) {
        this.name = name;
        this.status = status;
    }

    /**
     * Factory for a new, active account. Kept separate from the constructor
     * so new accounts can't accidentally start in any other state.
     */
    public static Account create(String name) {
        return new Account(name, AccountStatus.ACTIVE);
    }

    public void rename(String newName) {
        this.name = newName;
    }

    public void suspend() {
        this.status = AccountStatus.SUSPENDED;
    }

    public void activate() {
        this.status = AccountStatus.ACTIVE;
    }

    public boolean isActive() {
        return status == AccountStatus.ACTIVE;
    }
}