package br.com.hadryan.agro.manager.domain.expense;

import br.com.hadryan.agro.manager.domain.farm.Farm;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Representa uma despesa de uma lavoura.
 * O status de pagamento é derivado de paymentDate:
 * null = a pagar, preenchido = pago.
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "farm_id", nullable = false)
    private Farm farm;

    @Column(nullable = false, length = 200)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ExpenseCategory category;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal value;

    // Data de competência — quando a despesa foi incorrida
    @Column(name = "competence_date", nullable = false)
    private LocalDate competenceDate;

    // Data de pagamento — null indica "a pagar"; preenchida indica "pago"
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

    // Status de pagamento derivado — nunca persistido
    @Transient
    public boolean isPaid() {
        return paymentDate != null;
    }
}