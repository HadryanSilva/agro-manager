package br.com.hadryan.agro.manager.domain.auth;

/**
 * Tokens retornados após autenticação bem-sucedida.
 *
 * @param accessToken  token de curta duração usado nas requisições autenticadas
 * @param refreshToken token de longa duração usado para renovar o access token
 * @param tokenType    sempre "Bearer"
 * @param expiresIn    validade do access token em segundos
 */
public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn
) {}