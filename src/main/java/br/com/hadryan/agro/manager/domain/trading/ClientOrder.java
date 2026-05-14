package br.com.hadryan.agro.manager.domain.trading;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "client_orders")
@Getter @Setter @NoArgsConstructor
public class ClientOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private TradingClient client;
}
