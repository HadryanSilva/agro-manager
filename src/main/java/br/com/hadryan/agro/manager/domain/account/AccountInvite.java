package br.com.hadryan.agro.manager.domain.account;

import br.com.hadryan.agro.manager.domain.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Convite nominal de acesso a uma conta gerado por OWNER ou ADMIN.
 * O token UUID compõe a URL enviada por e-mail; apenas o usuário cujo
 * e-mail coincide com invitedEmail pode aceitar o convite.
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

    // Token único que compõe a URL de convite enviada por e-mail
    @Column(nullable = false, unique = true)
    private UUID token;

    // Código curto de 8 caracteres para entrada manual no onboarding
    @Column(nullable = false, unique = true, columnDefinition = "char(8)")
    private String code;

    // E-mail do destinatário — apenas este e-mail pode aceitar o convite
    @Column(name = "invited_email", length = 255)
    private String invitedEmail;

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