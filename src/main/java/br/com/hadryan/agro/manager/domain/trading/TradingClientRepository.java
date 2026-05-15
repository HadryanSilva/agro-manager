package br.com.hadryan.agro.manager.domain.trading;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TradingClientRepository extends JpaRepository<TradingClient, UUID> {

    List<TradingClient> findByAccountIdOrderByNameAsc(UUID accountId);

    @Query("SELECT c FROM TradingClient c WHERE c.account.id = :accountId AND LOWER(c.name) LIKE LOWER(CONCAT('%', :name, '%')) ORDER BY c.name")
    List<TradingClient> searchByName(@Param("accountId") UUID accountId, @Param("name") String name);

    Optional<TradingClient> findByIdAndAccountId(UUID id, UUID accountId);

    long countByAccountId(UUID accountId);
}
