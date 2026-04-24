package br.com.hadryan.agro.manager.shared.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Envelope padrão para todas as respostas da API.
 * Garante consistência no formato retornado ao cliente.
 *
 * @param success indica se a operação foi bem-sucedida
 * @param message mensagem descritiva (opcional em sucesso, obrigatória em erro)
 * @param data    payload da resposta (nulo em caso de erro)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        String message,
        T data
) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, null, data);
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data);
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, message, null);
    }

    public static <T> ApiResponse<T> error(String message, T data) {
        return new ApiResponse<>(false, message, data);
    }
}