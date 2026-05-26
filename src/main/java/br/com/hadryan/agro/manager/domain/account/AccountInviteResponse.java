package br.com.hadryan.agro.manager.domain.account;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Representação de um convite retornada ao cliente.
 * inviteUrl é a URL completa enviada por e-mail ao destinatário.
 * code é o código curto formatado XXXX-XXXX para exibição.
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
        String inviteUrl,
        String code
) {
    public static AccountInviteResponse from(AccountInvite invite, String frontendUrl) {
        String raw = invite.getCode();
        String formattedCode = raw.substring(0, 4) + "-" + raw.substring(4);
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
                frontendUrl + "/invite/" + invite.getToken(),
                formattedCode
        );
    }
}
