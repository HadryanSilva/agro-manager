package br.com.hadryan.agro.manager.shared.exception;

/**
 * Thrown when a business rule is violated. Maps to HTTP 422 Unprocessable
 * Entity.
 *
 * <p>Use this for cases where the request is syntactically correct (so 400
 * isn't right) but the requested operation violates a domain invariant —
 * e.g. trying to close a crop that has open transactions, trying to invite
 * a user already in the account.
 */
public class BusinessException extends ApplicationException {

    public BusinessException(String message) {
        super(message);
    }
}