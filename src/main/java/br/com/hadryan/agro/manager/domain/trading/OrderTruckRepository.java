package br.com.hadryan.agro.manager.domain.trading;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OrderTruckRepository extends JpaRepository<OrderTruck, UUID> {

    List<OrderTruck> findByLegOrderId(UUID orderId);
}
