package br.com.hadryan.agro.manager.domain.account;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Representação de uma conta retornada ao cliente.
 * Inclui o papel do usuário autenticado nessa conta.
 *
 * @param id          identificador da conta
 * @param name        nome da conta
 * @param userRole    papel do usuário autenticado nesta conta
 * @param memberCount total de membros ativos
 * @param createdAt   data de criação
 */
public record AccountResponse(
        UUID id,
        String name,
        AccountRole userRole,
        long memberCount,
        LocalDateTime createdAt
) {

    public static AccountResponse from(Account account, AccountRole role, long memberCount) {
        return new AccountResponse(
                account.getId(),
                account.getName(),
                role,
                memberCount,
                account.getCreatedAt()
        );
    }
}