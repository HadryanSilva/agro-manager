package br.com.hadryan.agro.manager.user.domain;

/**
 * Lifecycle states of a user. {@code INACTIVE} acts as a soft delete —
 * the row is preserved (historic references remain valid) but the user
 * can no longer log in.
 */
public enum UserStatus {
    ACTIVE,
    INACTIVE
}