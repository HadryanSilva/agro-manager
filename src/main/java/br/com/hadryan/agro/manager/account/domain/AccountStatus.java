package br.com.hadryan.agro.manager.account.domain;

/**
 * Lifecycle states of an account.
 *
 * <p>{@code ACTIVE} accounts can log in and use the system normally.
 * {@code SUSPENDED} accounts are blocked at the authentication layer but
 * their data is preserved — suspension is always reversible.
 *
 * <p>Hard deletion is intentionally not a state: accounts accumulate
 * financial and agronomic history that must not vanish.
 */
public enum AccountStatus {
    ACTIVE,
    SUSPENDED
}