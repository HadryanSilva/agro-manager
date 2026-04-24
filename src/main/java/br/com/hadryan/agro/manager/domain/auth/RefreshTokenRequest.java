package br.com.hadryan.agro.manager.domain.auth;

import jakarta.validation.constraints.NotBlank;

/**
 * Refresh token necessário para renovar o access token expirado.
 */
public record RefreshTokenRequest(

        @NotBlank(message = "Refresh token é obrigatório")
        String refreshToken
) {}