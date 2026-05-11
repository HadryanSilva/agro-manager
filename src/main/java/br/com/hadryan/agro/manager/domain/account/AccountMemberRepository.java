package br.com.hadryan.agro.manager.domain.account;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountMemberRepository extends JpaRepository<AccountMember, UUID> {

    // Retorna todas as contas das quais o usuário faz parte
    List<AccountMember> findByUserId(UUID userId);

    // Verifica se o usuário já é membro da conta
    boolean existsByAccountIdAndUserId(UUID accountId, UUID userId);

    // Busca o membership específico de um usuário em uma conta
    Optional<AccountMember> findByAccountIdAndUserId(UUID accountId, UUID userId);

    // Conta o total de membros de uma conta
    long countByAccountId(UUID accountId);

    // Lista todos os membros de uma conta com dados do usuário (evita N+1)
    @Query("""
            SELECT am FROM AccountMember am
            JOIN FETCH am.user
            WHERE am.account.id = :accountId
            ORDER BY am.role ASC, am.joinedAt ASC
            """)
    List<AccountMember> findByAccountIdWithUser(UUID accountId);

    /**
     * Retorna a contagem de membros por conta em uma única query (GROUP BY).
     * Evita o problema N+1 que ocorre ao chamar countByAccountId() por iteração.
     *
     * Retorna pares [accountId, count] como Object[] para mapeamento manual.
     */
    @Query("""
            SELECT am.account.id, COUNT(am)
            FROM AccountMember am
            WHERE am.account.id IN :accountIds
            GROUP BY am.account.id
            """)
    List<Object[]> countMembersByAccountIds(Collection<UUID> accountIds);
}