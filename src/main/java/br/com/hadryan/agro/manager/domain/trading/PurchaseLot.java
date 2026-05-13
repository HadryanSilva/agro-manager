package br.com.hadryan.agro.manager.domain.trading;

import br.com.hadryan.agro.manager.domain.account.Account;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Lote de compra do modo comprador.
 * Representa uma negociação com um fornecedor: data, preço por Kg acordado.
 * O volume total em Kg é derivado da soma dos caminhões (purchase_trucks).
 */
@Entity
@Table(name = "purchase_lots")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseLot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    // Fornecedor — sempre obrigatório (não usar CascadeType.ALL em @ManyToOne)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    private TradingSupplier supplier;

    @Column(name = "purchase_date", nullable = false)
    private LocalDate purchaseDate;

    @Column(name = "price_per_kg", nullable = false, precision = 10, scale = 4)
    private BigDecimal pricePerKg;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private PurchaseLotStatus status = PurchaseLotStatus.OPEN;

    @Column(columnDefinition = "TEXT")
    private String notes;

    // Caminhões que trouxeram este lote — cascade controlado pelo lote pai
    @OneToMany(mappedBy = "lot", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<PurchaseTruck> trucks = new ArrayList<>();

    // Vendas deste lote — cascade controlado pelo lote pai
    @OneToMany(mappedBy = "lot", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<LotSale> sales = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now(ZoneOffset.UTC);
        updatedAt = LocalDateTime.now(ZoneOffset.UTC);
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now(ZoneOffset.UTC);
    }
}
