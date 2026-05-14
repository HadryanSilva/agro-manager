package br.com.hadryan.agro.manager.domain.trading;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Caminhão que compõe a entrega de um lote de compra.
 * Cada caminhão tem sua própria placa e volume em Kg carregado.
 */
@Entity
@Table(name = "purchase_trucks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseTruck {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lot_id", nullable = false)
    private PurchaseLot lot;

    @Column(name = "truck_plate", nullable = false, length = 10)
    private String truckPlate;

    @Column(name = "quantity_kg", nullable = false, precision = 12, scale = 2)
    private BigDecimal quantityKg;

    @Column(name = "freight_value", precision = 12, scale = 2)
    private BigDecimal freightValue;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
