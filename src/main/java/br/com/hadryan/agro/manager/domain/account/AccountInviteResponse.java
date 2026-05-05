package br.com.hadryan.agro.manager.domain.account;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Representação de um convite retornada ao cliente.
 * inviteUrl é a URL completa enviada por e-mail ao destinatário.
 */
public record AccountInviteResponse(
        UUID id,
        UUID token,
        String accountName,
        AccountRole role,
        String invitedEmail,
        String createdByName,
        LocalDateTime expiresAt,
        boolean used,
        boolean expired,
        String inviteUrl
) {
    public static AccountInviteResponse from(AccountInvite invite, String frontendUrl) {
        return new AccountInviteResponse(
                invite.getId(),
                invite.getToken(),
                invite.getAccount().getName(),
                invite.getRole(),
                invite.getInvitedEmail(),
                invite.getCreatedBy().getName(),
                invite.getExpiresAt(),
                invite.isUsed(),
                invite.isExpired(),
                frontendUrl + "/invite/" + invite.getToken()
        );
    }
}