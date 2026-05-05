package br.com.hadryan.agro.manager.domain.expense;

import br.com.hadryan.agro.manager.domain.account.Account;
import br.com.hadryan.agro.manager.domain.farm.Farm;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Representa uma despesa.
 * Pode ser vinculada a uma lavoura (farm != null) ou ser geral da conta (farm == null).
 * O campo account garante que despesas gerais ainda pertencem à conta correta.
 */
@Entity
@Table(name = "expenses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Conta sempre obrigatória — garante o tenant correto para despesas gerais
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    // Lavoura opcional — null indica despesa geral da conta
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "farm_id")
    private Farm farm;

    @Column(nullable = false, length = 200)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ExpenseCategory category;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal value;

    @Column(name = "competence_date", nullable = false)
    private LocalDate competenceDate;

    @Column(name = "payment_date")
    private LocalDate paymentDate;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @Transient
    public boolean isPaid() {
        return paymentDate != null;
    }
}