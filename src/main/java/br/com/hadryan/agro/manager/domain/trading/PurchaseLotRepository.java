package br.com.hadryan.agro.manager.domain.trading;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositório de lotes de compra.
 */
public interface PurchaseLotRepository extends JpaRepository<PurchaseLot, UUID> {

    // ── Listagens paginadas ───────────────────────────────────────────────────

    // Listagem por conta com supplier e customerOrder carregados via JOIN FETCH (evita N+1)
    @Query(value = "SELECT pl FROM PurchaseLot pl JOIN FETCH pl.supplier s JOIN FETCH pl.customerOrder co WHERE pl.account.id = :accountId",
            countQuery = "SELECT COUNT(pl) FROM PurchaseLot pl WHERE pl.account.id = :accountId")
    Page<PurchaseLot> findByAccountIdWithSupplier(@Param("accountId") UUID accountId, Pageable pageable);

    // Listagem filtrada por status com supplier e customerOrder
    @Query(value = "SELECT pl FROM PurchaseLot pl JOIN FETCH pl.supplier s JOIN FETCH pl.customerOrder co WHERE pl.account.id = :accountId AND pl.status = :status",
            countQuery = "SELECT COUNT(pl) FROM PurchaseLot pl WHERE pl.account.id = :accountId AND pl.status = :status")
    Page<PurchaseLot> findByAccountIdAndStatusWithSupplier(@Param("accountId") UUID accountId,
                                                           @Param("status") PurchaseLotStatus status,
                                                           Pageable pageable);

    // Listagem por fornecedor com supplier e customerOrder
    @Query(value = "SELECT pl FROM PurchaseLot pl JOIN FETCH pl.supplier s JOIN FETCH pl.customerOrder co WHERE pl.account.id = :accountId AND pl.supplier.id = :supplierId",
            countQuery = "SELECT COUNT(pl) FROM PurchaseLot pl WHERE pl.account.id = :accountId AND pl.supplier.id = :supplierId")
    Page<PurchaseLot> findByAccountIdAndSupplierIdWithSupplier(@Param("accountId") UUID accountId,
                                                               @Param("supplierId") UUID supplierId,
                                                               Pageable pageable);

    // ── Detalhe (correção: busca separada para cada coleção — evita MultipleBagFetchException) ──

    // Carrega supplier + customerOrder + caminhões de compra. As vendas são carregadas separadamente pelo serviço.
    @Query("SELECT pl FROM PurchaseLot pl " +
            "JOIN FETCH pl.supplier s " +
            "JOIN FETCH pl.customerOrder co " +
            "LEFT JOIN FETCH pl.trucks t " +
            "WHERE pl.id = :id AND pl.account.id = :accountId")
    Optional<PurchaseLot> findWithTrucksByIdAndAccountId(@Param("id") UUID id,
                                                         @Param("accountId") UUID accountId);

    // Busca simples por id e account (sem joins — para operações de escrita)
    Optional<PurchaseLot> findByIdAndAccountId(UUID id, UUID accountId);

    // ── Agregações — evitam acesso a coleções lazy ────────────────────────────

    // Total de Kg comprado de um lote (soma dos caminhões de compra)
    @Query("SELECT COALESCE(SUM(t.quantityKg), 0) FROM PurchaseTruck t WHERE t.lot.id = :lotId")
    BigDecimal sumPurchasedKgByLotId(@Param("lotId") UUID lotId);

    // Total de Kg comprado por conta — para o dashboard
    @Query("SELECT COALESCE(SUM(t.quantityKg), 0) FROM PurchaseTruck t JOIN t.lot pl WHERE pl.account.id = :accountId")
    BigDecimal sumPurchasedKgByAccountId(@Param("accountId") UUID accountId);

    // Custo total por conta (Kg x preco/Kg de cada lote) — para o dashboard
    @Query("SELECT COALESCE(SUM(t.quantityKg * pl.pricePerKg), 0) FROM PurchaseTruck t JOIN t.lot pl WHERE pl.account.id = :accountId")
    BigDecimal sumPurchasedCostByAccountId(@Param("accountId") UUID accountId);

    // Totais de Kg comprado agrupados por lote — resolve N+1 na listagem
    @Query("SELECT t.lot.id, COALESCE(SUM(t.quantityKg), 0) FROM PurchaseTruck t WHERE t.lot.id IN :lotIds GROUP BY t.lot.id")
    List<Object[]> sumPurchasedKgByLotIds(@Param("lotIds") List<UUID> lotIds);

    // ── Contagens para o dashboard ────────────────────────────────────────────

    long countByAccountId(UUID accountId);

    long countByAccountIdAndStatus(UUID accountId, PurchaseLotStatus status);

    // Verifica se existe lote vinculado a um pedido — para derivar status FULFILLED
    boolean existsByCustomerOrderId(UUID customerOrderId);

    // Retorna IDs de todos os pedidos com lote na conta — resolve status em bulk (sem N+1)
    @Query("SELECT pl.customerOrder.id FROM PurchaseLot pl WHERE pl.account.id = :accountId")
    List<UUID> findCustomerOrderIdsByAccountId(@Param("accountId") UUID accountId);
}