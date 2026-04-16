/**
 * Code shared across features.
 *
 * <p>Put something here only when at least two features genuinely need it.
 * Feature-specific helpers live next to the feature that owns them.
 *
 * <p>Current contents:
 * <ul>
 *   <li>{@code domain} — base types for JPA entities (e.g. {@code BaseEntity}).</li>
 * </ul>
 *
 * <p>Future contents (already planned):
 * <ul>
 *   <li>{@code exception} — base exceptions and the global exception handler
 *       (Task 3).</li>
 *   <li>{@code tenant} — multi-tenancy infrastructure (Hibernate filter +
 *       {@code TenantContext}), wired after Account lands (Task 4+).</li>
 * </ul>
 */
package br.com.hadryan.agro.manager.shared;