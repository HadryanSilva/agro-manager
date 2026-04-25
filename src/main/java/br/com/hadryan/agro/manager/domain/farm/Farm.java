package br.com.hadryan.agro.manager.domain.farm;

import br.com.hadryan.agro.manager.domain.account.Account;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Representa um ciclo de plantio de melancia vinculado a uma conta.
 * O status é calculado dinamicamente com base nas datas preenchidas.
 */
@Entity
@Table(name = "farms")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Farm {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(nullable = false)
    private String name;

    // Área da lavoura
    @Column(name = "area_value", nullable = false, precision = 10, scale = 2)
    private BigDecimal areaValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "area_unit", nullable = false)
    private AreaUnit areaUnit;

    // Dados do arrendamento
    @Column(name = "lessor_name")
    private String lessorName;

    @Column(name = "lease_start_date")
    private LocalDate leaseStartDate;

    @Column(name = "lease_end_date")
    private LocalDate leaseEndDate;

    @Column(name = "lease_value", precision = 12, scale = 2)
    private BigDecimal leaseValue;

    // Período de plantio
    @Column(name = "planting_start_date")
    private LocalDate plantingStartDate;

    @Column(name = "planting_end_date")
    private LocalDate plantingEndDate;

    // Período de colheita
    @Column(name = "harvest_start_date")
    private LocalDate harvestStartDate;

    @Column(name = "harvest_end_date")
    private LocalDate harvestEndDate;

    @Column(nullable = false)
    @Builder.Default
    private boolean cancelled = false;

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

    /**
     * Calcula o status atual da lavoura com base nas datas registradas.
     * A precedência é: cancelada > colhida > em andamento > em preparação.
     */
    @Transient
    public FarmStatus getStatus() {
        if (cancelled) return FarmStatus.CANCELADA;
        if (harvestStartDate != null) return FarmStatus.COLHIDA;
        if (plantingStartDate != null) return FarmStatus.EM_ANDAMENTO;
        return FarmStatus.EM_PREPARACAO;
    }
}