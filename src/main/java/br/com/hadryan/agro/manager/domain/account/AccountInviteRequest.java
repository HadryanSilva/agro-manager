package br.com.hadryan.agro.manager.domain.account;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Payload para criação de convite nominal.
 * O e-mail é obrigatório — apenas o destinatário pode aceitar o convite.
 */
public record AccountInviteRequest(
        @NotBlank(message = "E-mail do convidado é obrigatório")
        @Email(message = "E-mail inválido")
        String email,

        AccountRole role
) {
    // Garante papel padrão MEMBER quando não especificado
    public AccountRole roleOrDefault() {
        return role != null ? role : AccountRole.MEMBER;
    }
}