package br.com.hadryan.agro.manager.domain.auth;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Tokens retornados após autenticação bem-sucedida.
 *
 * @param accessToken  token de curta duração usado nas requisições autenticadas
 * @param refreshToken token de longa duração usado apenas para cookie HttpOnly
 * @param tokenType    sempre "Bearer"
 * @param expiresIn    validade do access token em segundos
 */
public record AuthResponse(
        String accessToken,
        @JsonIgnore
        String refreshToken,
        String tokenType,
        long expiresIn
) {}
