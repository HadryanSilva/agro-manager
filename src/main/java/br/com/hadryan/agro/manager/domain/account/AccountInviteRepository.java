package br.com.hadryan.agro.manager.domain.account;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountInviteRepository extends JpaRepository<AccountInvite, UUID> {

    // Busca convites ativos de uma conta (não utilizados e não expirados)
    @Query("""
            SELECT i FROM AccountInvite i
            JOIN FETCH i.account
            JOIN FETCH i.createdBy
            WHERE i.account.id = :accountId
              AND i.usedAt IS NULL
              AND i.expiresAt > :now
            ORDER BY i.createdAt DESC
            """)
    List<AccountInvite> findActiveByAccountId(@Param("accountId") UUID accountId,
                                              @Param("now") LocalDateTime now);

    // Busca convite pelo token com todos os relacionamentos carregados
    @Query("""
            SELECT i FROM AccountInvite i
            JOIN FETCH i.account
            JOIN FETCH i.createdBy
            WHERE i.token = :token
            """)
    Optional<AccountInvite> findByTokenWithDetails(@Param("token") UUID token);

    // Verifica se já existe convite ativo para o mesmo e-mail na mesma conta
    // Evita duplicidade antes de criar um novo convite
    @Query("""
            SELECT COUNT(i) > 0 FROM AccountInvite i
            WHERE i.account.id = :accountId
              AND LOWER(i.invitedEmail) = LOWER(:email)
              AND i.usedAt IS NULL
              AND i.expiresAt > :now
            """)
    boolean existsActiveInviteByEmail(@Param("accountId") UUID accountId,
                                      @Param("email") String email,
                                      @Param("now") LocalDateTime now);

    @Modifying
    @Query("DELETE FROM AccountInvite i WHERE i.account.id = :accountId")
    void deleteByAccountId(@Param("accountId") UUID accountId);
}