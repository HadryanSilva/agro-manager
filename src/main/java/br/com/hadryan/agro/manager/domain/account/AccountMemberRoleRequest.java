package br.com.hadryan.agro.manager.domain.account;

import jakarta.validation.constraints.NotNull;

/**
 * Payload para alteração do papel de um membro.
 * Não permite atribuir OWNER via API — essa operação é restrita.
 */
public record AccountMemberRoleRequest(

        @NotNull(message = "O papel é obrigatório")
        AccountRole role
) {}