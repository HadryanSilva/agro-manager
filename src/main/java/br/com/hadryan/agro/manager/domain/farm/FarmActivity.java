package br.com.hadryan.agro.manager.domain.farm;

import br.com.hadryan.agro.manager.domain.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Registro de uma atividade ocorrida em uma lavoura.
 * Gerado automaticamente pelos serviços (despesas, farm) ou manualmente pelo usuário (NOTE).
 */
@Entity
@Table(name = "farm_activities")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FarmActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "farm_id", nullable = false)
    private Farm farm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private FarmActivityType type;

    // Descrição legível da atividade (gerada automaticamente ou digitada pelo usuário)
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    // Referência opcional ao objeto relacionado (ex: id da despesa)
    @Column(name = "related_id")
    private UUID relatedId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}