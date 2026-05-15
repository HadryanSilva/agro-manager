package br.com.hadryan.agro.manager.domain.trading;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TradingSupplierRepository extends JpaRepository<TradingSupplier, UUID> {

    List<TradingSupplier> findByAccountIdOrderByNameAsc(UUID accountId);

    @Query("SELECT s FROM TradingSupplier s WHERE s.account.id = :accountId AND LOWER(s.name) LIKE LOWER(CONCAT('%', :name, '%')) ORDER BY s.name")
    List<TradingSupplier> searchByName(@Param("accountId") UUID accountId, @Param("name") String name);

    Optional<TradingSupplier> findByIdAndAccountId(UUID id, UUID accountId);

    long countByAccountId(UUID accountId);

    @Query("SELECT COUNT(l) > 0 FROM OrderSupplierLeg l WHERE l.supplier.id = :supplierId")
    boolean hasAssociatedLegs(@Param("supplierId") UUID supplierId);
}
