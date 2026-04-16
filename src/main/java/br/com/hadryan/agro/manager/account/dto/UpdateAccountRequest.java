package br.com.hadryan.agro.manager.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload for {@code PUT /accounts/{id}}. Only mutable fields are exposed —
 * {@code status} changes happen through dedicated endpoints
 * ({@code POST /accounts/{id}/suspend}, {@code .../activate}) so the intent
 * is explicit in request logs.
 */
public record UpdateAccountRequest(
        @NotBlank
        @Size(max = 120)
        String name
) {
}