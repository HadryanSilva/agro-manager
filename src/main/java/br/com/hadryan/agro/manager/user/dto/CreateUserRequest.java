package br.com.hadryan.agro.manager.user.dto;

import br.com.hadryan.agro.manager.user.domain.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Payload for creating a user with a local password. Used by admins to
 * add members to an existing account. Self-registration (which also
 * creates an account) will have its own endpoint in Task 6.
 */
public record CreateUserRequest(
        @NotNull
        Long accountId,

        @NotBlank
        @Email
        @Size(max = 255)
        String email,

        @NotBlank
        @Size(max = 120)
        String name,

        @NotBlank
        @Size(min = 8, max = 72) // BCrypt has a 72-byte input limit
        String password,

        @NotNull
        UserRole role
) {
}