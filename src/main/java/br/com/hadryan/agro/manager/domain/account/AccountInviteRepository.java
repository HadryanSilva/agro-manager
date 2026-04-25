package br.com.hadryan.agro.manager.domain.account;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountInviteRepository extends JpaRepository<AccountInvite, UUID> {

    // Busca convite pelo token para aceite ou exibição de detalhes
    @Query("SELECT i FROM AccountInvite i JOIN FETCH i.account JOIN FETCH i.createdBy WHERE i.token = :token")
    Optional<AccountInvite> findByTokenWithDetails(UUID token);

    // Lista convites ativos (não utilizados e não expirados) de uma conta
    @Query("""
            SELECT i FROM AccountInvite i
            JOIN FETCH i.createdBy
            WHERE i.account.id = :accountId
              AND i.usedAt IS NULL
              AND i.expiresAt > :now
            ORDER BY i.createdAt DESC
            """)
    List<AccountInvite> findActiveByAccountId(UUID accountId, LocalDateTime now);
}