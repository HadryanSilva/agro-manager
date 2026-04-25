package br.com.hadryan.agro.manager.domain.account;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Payload para criação de convite — apenas o papel a ser atribuído.
 * Se não informado, o padrão é MEMBER.
 */
public record AccountInviteRequest(AccountRole role) {

    // Garante papel padrão MEMBER quando não especificado
    public AccountRole roleOrDefault() {
        return role != null ? role : AccountRole.MEMBER;
    }
}