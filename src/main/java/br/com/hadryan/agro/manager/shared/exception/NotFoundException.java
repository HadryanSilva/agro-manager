package br.com.hadryan.agro.manager.shared.exception;

/**
 * Thrown when a requested resource does not exist. Maps to HTTP 404.
 *
 * <p>Prefer the {@link #of(String, Object)} factory for consistent messages
 * like {@code "User with id 42 not found"}.
 */
public class NotFoundException extends ApplicationException {

    public NotFoundException(String message) {
        super(message);
    }

    /**
     * Convenience factory producing {@code "<resource> with id <id> not found"}.
     */
    public static NotFoundException of(String resourceName, Object id) {
        return new NotFoundException("%s with id %s not found".formatted(resourceName, id));
    }
}