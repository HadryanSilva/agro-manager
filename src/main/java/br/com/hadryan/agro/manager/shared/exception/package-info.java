/**
 * Exception hierarchy and the global HTTP-layer translator.
 *
 * <p>Domain code throws from this hierarchy; {@code GlobalExceptionHandler}
 * turns each exception into an RFC 7807 {@code ProblemDetail} response.
 *
 * <p>Status mapping:
 * <ul>
 *   <li>{@link br.com.hadryan.agro.manager.shared.exception.NotFoundException} → 404</li>
 *   <li>{@link br.com.hadryan.agro.manager.shared.exception.BusinessException} → 422</li>
 *   <li>{@link br.com.hadryan.agro.manager.shared.exception.ConflictException} → 409</li>
 * </ul>
 *
 * <p>When you need a new category, add a subclass of {@link
 * br.com.hadryan.agro.manager.shared.exception.ApplicationException} and a
 * matching {@code @ExceptionHandler} method. Don't catch-and-rethrow
 * framework exceptions in services — let them bubble up to the handler.
 */
package br.com.hadryan.agro.manager.shared.exception;