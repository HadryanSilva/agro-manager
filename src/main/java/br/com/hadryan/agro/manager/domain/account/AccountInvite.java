package br.com.hadryan.agro.manager.domain.account;

import br.com.hadryan.agro.manager.domain.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Convite de acesso a uma conta gerado por OWNER ou ADMIN.
 * O token UUID é compartilhado externamente; o usuário que o aceitar
 * é adicionado como membro com o papel definido em role.
 *
 * usedAt null     → convite ativo
 * usedAt not null → convite já utilizado
 */
@Entity
@Table(name = "account_invites")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountInvite {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    // Token único que compõe a URL de convite compartilhada
    @Column(nullable = false, unique = true)
    private UUID token;

    // Papel atribuído ao usuário ao aceitar
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountRole role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    // Preenchido quando o convite é aceito pelo destinatário
    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @Transient
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    @Transient
    public boolean isUsed() {
        return usedAt != null;
    }

    @Transient
    public boolean isActive() {
        return !isUsed() && !isExpired();
    }
}