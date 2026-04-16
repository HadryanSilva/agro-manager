package br.com.hadryan.agro.manager.shared.exception;

/**
 * Root of the application's exception hierarchy.
 *
 * <p>Any exception that the {@code GlobalExceptionHandler} translates into a
 * meaningful HTTP response should extend this class. Framework exceptions
 * (Spring, JPA, validation) are handled separately because we don't own them.
 *
 * <p>This class is abstract on purpose — throwing {@code ApplicationException}
 * directly would be imprecise. Use one of the concrete subtypes
 * ({@code NotFoundException}, {@code BusinessException}, {@code
 * ConflictException}) or add a new one when a new category is needed.
 */
public abstract class ApplicationException extends RuntimeException {

    protected ApplicationException(String message) {
        super(message);
    }

    protected ApplicationException(String message, Throwable cause) {
        super(message, cause);
    }
}
