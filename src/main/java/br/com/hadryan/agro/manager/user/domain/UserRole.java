package br.com.hadryan.agro.manager.user.domain;

/**
 * Access level of a user within their account.
 *
 * <p>Ordered from most to least privileged. Concrete permission checks are
 * done in a later task (Security); this enum just records the intent.
 *
 * <ul>
 *   <li>{@code OWNER} — full control, including billing and member
 *       management. Exactly one per account.</li>
 *   <li>{@code ADMIN} — can manage members (except other admins/owner) and
 *       all operational data.</li>
 *   <li>{@code MEMBER} — day-to-day operational access: create/edit crops
 *       and transactions.</li>
 *   <li>{@code VIEWER} — read-only.</li>
 * </ul>
 */
public enum UserRole {
    OWNER,
    ADMIN,
    MEMBER,
    VIEWER
}