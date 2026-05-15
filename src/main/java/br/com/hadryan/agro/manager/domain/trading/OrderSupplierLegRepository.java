package br.com.hadryan.agro.manager.domain.trading;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OrderSupplierLegRepository extends JpaRepository<OrderSupplierLeg, UUID> {

    Optional<OrderSupplierLeg> findByIdAndOrderId(UUID id, UUID orderId);

    boolean existsBySupplierId(UUID supplierId);
}
