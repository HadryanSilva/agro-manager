package br.com.hadryan.agro.manager.shared.exception;

/**
 * Exceção lançada quando um recurso solicitado não é encontrado no banco de dados.
 * Mapeada para HTTP 404 Not Found.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resource, String field, Object value) {
        super(String.format("%s não encontrado com %s: '%s'", resource, field, value));
    }

    public ResourceNotFoundException(String message) {
        super(message);
    }
}