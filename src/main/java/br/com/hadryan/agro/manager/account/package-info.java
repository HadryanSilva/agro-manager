/**
 * Account feature — the root tenant of the system.
 *
 * <p>Everything else (users, crops, transactions, invitations) is scoped to
 * an {@link br.com.hadryan.agro.manager.account.domain.Account}. Tenant
 * isolation is enforced by a Hibernate filter wired up in a later task; this
 * package stays focused on the account lifecycle itself (create, rename,
 * suspend, activate).
 *
 * <p>Layout follows the project convention:
 * <ul>
 *   <li>{@code domain} — entity + value types</li>
 *   <li>{@code repository} — Spring Data interfaces</li>
 *   <li>{@code service} — business operations</li>
 *   <li>{@code controller} — HTTP surface</li>
 *   <li>{@code dto} — request/response payloads</li>
 * </ul>
 */
package br.com.hadryan.agro.manager.account;