package br.com.hadryan.agro.manager.shared.exception;

/**
 * Thrown when the request can't proceed because of a conflict with the
 * current state. Maps to HTTP 409 Conflict.
 *
 * <p>Typical uses: attempting to create a resource with a value that must be
 * unique and already exists (e.g. user email), or accepting an invitation
 * that has already been consumed.
 */
public class ConflictException extends ApplicationException {

    public ConflictException(String message) {
        super(message);
    }
}