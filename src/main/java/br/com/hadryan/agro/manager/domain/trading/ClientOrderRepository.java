package br.com.hadryan.agro.manager.domain.trading;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ClientOrderRepository extends JpaRepository<ClientOrder, UUID> {
    boolean existsByClientId(UUID clientId);
}
