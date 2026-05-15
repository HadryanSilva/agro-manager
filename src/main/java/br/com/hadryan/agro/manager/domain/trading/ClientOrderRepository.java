package br.com.hadryan.agro.manager.domain.trading;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClientOrderRepository extends JpaRepository<ClientOrder, UUID> {

    boolean existsByClientId(UUID clientId);

    Optional<ClientOrder> findByIdAndAccountId(UUID id, UUID accountId);

    @Query("SELECT o FROM ClientOrder o JOIN FETCH o.client WHERE o.account.id = :accountId ORDER BY o.orderDate DESC")
    Page<ClientOrder> findByAccountIdWithClient(@Param("accountId") UUID accountId, Pageable pageable);

    @Query("SELECT o FROM ClientOrder o JOIN FETCH o.client WHERE o.account.id = :accountId AND o.status = :status ORDER BY o.orderDate DESC")
    Page<ClientOrder> findByAccountIdAndStatusWithClient(@Param("accountId") UUID accountId, @Param("status") ClientOrderStatus status, Pageable pageable);

    @Query("SELECT o FROM ClientOrder o JOIN FETCH o.client WHERE o.account.id = :accountId AND o.client.id = :clientId ORDER BY o.orderDate DESC")
    Page<ClientOrder> findByAccountIdAndClientIdWithClient(@Param("accountId") UUID accountId, @Param("clientId") UUID clientId, Pageable pageable);

    @Query("SELECT o FROM ClientOrder o JOIN FETCH o.client WHERE o.account.id = :accountId AND o.status = :status AND o.client.id = :clientId ORDER BY o.orderDate DESC")
    Page<ClientOrder> findByAccountIdAndStatusAndClientIdWithClient(@Param("accountId") UUID accountId, @Param("status") ClientOrderStatus status, @Param("clientId") UUID clientId, Pageable pageable);

    @Query("SELECT DISTINCT o FROM ClientOrder o JOIN FETCH o.legs l JOIN FETCH l.supplier WHERE o.id = :orderId AND o.account.id = :accountId")
    Optional<ClientOrder> findWithLegsByIdAndAccountId(@Param("orderId") UUID orderId, @Param("accountId") UUID accountId);

    @Query("SELECT t.leg.order.id, COALESCE(SUM(t.quantityKg), 0) FROM OrderTruck t WHERE t.leg.order.id IN :orderIds GROUP BY t.leg.order.id")
    List<Object[]> sumTotalKgByOrderIds(@Param("orderIds") List<UUID> orderIds);

    @Query("SELECT t.leg.order.id, COALESCE(SUM(t.quantityKg * t.leg.supplierPricePerKg), 0) + COALESCE(SUM(t.freightValue), 0) FROM OrderTruck t WHERE t.leg.order.id IN :orderIds GROUP BY t.leg.order.id")
    List<Object[]> sumTotalCostByOrderIds(@Param("orderIds") List<UUID> orderIds);

    long countByAccountId(UUID accountId);

    long countByAccountIdAndStatus(UUID accountId, ClientOrderStatus status);

    @Modifying
    @Query("DELETE FROM ClientOrder o WHERE o.account.id = :accountId")
    void deleteByAccountId(@Param("accountId") UUID accountId);

    @Query("SELECT COALESCE(SUM(t.quantityKg), 0) FROM OrderTruck t WHERE t.leg.order.account.id = :accountId")
    BigDecimal sumTotalKgByAccountId(@Param("accountId") UUID accountId);

    @Query("SELECT COALESCE(SUM(t.quantityKg * t.leg.order.clientPricePerKg), 0) FROM OrderTruck t WHERE t.leg.order.account.id = :accountId")
    BigDecimal sumRevenueByAccountId(@Param("accountId") UUID accountId);

    @Query("SELECT COALESCE(SUM(t.quantityKg * t.leg.supplierPricePerKg), 0) FROM OrderTruck t WHERE t.leg.order.account.id = :accountId")
    BigDecimal sumProductCostByAccountId(@Param("accountId") UUID accountId);

    @Query("SELECT COALESCE(SUM(t.freightValue), 0) FROM OrderTruck t WHERE t.leg.order.account.id = :accountId")
    BigDecimal sumFreightCostByAccountId(@Param("accountId") UUID accountId);
}
