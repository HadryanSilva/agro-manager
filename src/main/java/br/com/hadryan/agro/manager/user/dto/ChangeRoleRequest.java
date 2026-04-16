package br.com.hadryan.agro.manager.user.dto;

import br.com.hadryan.agro.manager.user.domain.UserRole;
import jakarta.validation.constraints.NotNull;

public record ChangeRoleRequest(
        @NotNull
        UserRole role
) {
}