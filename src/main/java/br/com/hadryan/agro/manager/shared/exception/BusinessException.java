package br.com.hadryan.agro.manager.shared.exception;

import org.springframework.http.HttpStatus;

/**
 * Exceção lançada quando uma regra de negócio é violada.
 * Mapeada para HTTP 400 Bad Request por padrão.
 */
public class BusinessException extends RuntimeException {

    private final HttpStatus status;

    public BusinessException(String message) {
        super(message);
        this.status = HttpStatus.BAD_REQUEST;
    }

    public BusinessException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}