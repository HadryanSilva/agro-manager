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
 * Pedido de venda de um lote.
 * Representa a negociação com um comprador: data e preço por Kg acordado.
 * O volume vendido é derivado da soma dos caminhões de entrega (lot_sale_trucks).
 */
@Entity
@Table(name = "lot_sales")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LotSale {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lot_id", nullable = false)
    private PurchaseLot lot;

    @Column(name = "buyer_name", nullable = false, length = 150)
    private String buyerName;

    @Column(name = "sale_date", nullable = false)
    private LocalDate saleDate;

    @Column(name = "price_per_kg", nullable = false, precision = 10, scale = 4)
    private BigDecimal pricePerKg;

    @Column(columnDefinition = "TEXT")
    private String notes;

    // Caminhões de entrega desta venda — cascade controlado pela venda pai
    @OneToMany(mappedBy = "sale", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<LotSaleTruck> trucks = new ArrayList<>();

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
