package br.com.hadryan.agro.manager.domain.trading;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface OrderTruckRepository extends JpaRepository<OrderTruck, UUID> {

    List<OrderTruck> findByLegOrderId(UUID orderId);

    @Modifying
    @Query("DELETE FROM OrderTruck t WHERE t.leg.order.account.id = :accountId")
    void deleteByLegOrderAccountId(UUID accountId);
}
