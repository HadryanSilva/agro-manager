package br.com.hadryan.agro.manager.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload for {@code POST /accounts}. Once authentication lands this will be
 * reused by the registration flow.
 */
public record CreateAccountRequest(
        @NotBlank
        @Size(max = 120)
        String name
) {
}