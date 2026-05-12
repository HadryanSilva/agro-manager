package br.com.hadryan.agro.manager.domain.trading;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * Caminhão de entrega de um pedido de venda.
 * O comprador solicita X caminhões — cada um tem placa e peso registrados.
 */
@Entity
@Table(name = "lot_sale_trucks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LotSaleTruck {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_id", nullable = false)
    private LotSale sale;

    @Column(name = "truck_plate", nullable = false, length = 10)
    private String truckPlate;

    @Column(name = "quantity_kg", nullable = false, precision = 12, scale = 2)
    private BigDecimal quantityKg;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now(ZoneOffset.UTC);
    }
}
