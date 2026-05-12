package br.com.hadryan.agro.manager.domain.trading;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositório de fornecedores do modo atravessador.
 */
public interface TradingSupplierRepository extends JpaRepository<TradingSupplier, UUID> {

    // Listagem por conta ordenada por nome — sem N+1 (campos simples)
    List<TradingSupplier> findByAccountIdOrderByNameAsc(UUID accountId);

    // Busca por nome parcial (case-insensitive) para autocomplete
    @Query("SELECT s FROM TradingSupplier s WHERE s.account.id = :accountId AND LOWER(s.name) LIKE LOWER(CONCAT('%', :name, '%')) ORDER BY s.name")
    List<TradingSupplier> searchByName(@Param("accountId") UUID accountId, @Param("name") String name);

    // Busca individual validando o tenant
    Optional<TradingSupplier> findByIdAndAccountId(UUID id, UUID accountId);

    // Verifica se há lotes vinculados antes de excluir
    @Query("SELECT COUNT(pl) > 0 FROM PurchaseLot pl WHERE pl.supplier.id = :supplierId")
    boolean hasAssociatedLots(@Param("supplierId") UUID supplierId);

    // Contagem direta — evita carregar a lista completa só para pegar o tamanho
    long countByAccountId(UUID accountId);
}
