package br.com.hadryan.agro.manager.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Mutable fields of the user profile. Role, status, and credentials go
 * through dedicated endpoints so the intent is explicit in logs.
 */
public record UpdateUserRequest(
        @NotBlank
        @Size(max = 120)
        String name
) {
}