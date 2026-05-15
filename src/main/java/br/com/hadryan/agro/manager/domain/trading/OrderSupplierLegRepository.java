package br.com.hadryan.agro.manager.domain.trading;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface OrderSupplierLegRepository extends JpaRepository<OrderSupplierLeg, UUID> {

    Optional<OrderSupplierLeg> findByIdAndOrderId(UUID id, UUID orderId);

    boolean existsBySupplierId(UUID supplierId);

    @Modifying
    @Query("DELETE FROM OrderSupplierLeg l WHERE l.order.account.id = :accountId")
    void deleteByOrderAccountId(UUID accountId);
}
