package br.com.hadryan.agro.manager.domain.account;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Representação de um membro da conta retornada ao cliente.
 * Inclui dados básicos do usuário para exibição na lista.
 */
public record AccountMemberResponse(
        UUID id,
        UUID userId,
        String name,
        String email,
        String avatarUrl,
        AccountRole role,
        LocalDateTime joinedAt
) {
    public static AccountMemberResponse from(AccountMember member) {
        return new AccountMemberResponse(
                member.getId(),
                member.getUser().getId(),
                member.getUser().getName(),
                member.getUser().getEmail(),
                member.getUser().getAvatarUrl(),
                member.getRole(),
                member.getJoinedAt()
        );
    }
}