package br.com.hadryan.agro.manager.domain.trading;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositório de vendas de lotes.
 */
public interface LotSaleRepository extends JpaRepository<LotSale, UUID> {

    // Vendas de um lote com caminhões carregados via JOIN FETCH (evita N+1 no detalhe do lote)
    @Query("SELECT ls FROM LotSale ls LEFT JOIN FETCH ls.trucks t WHERE ls.lot.id = :lotId ORDER BY ls.saleDate DESC")
    List<LotSale> findByLotIdWithTrucks(@Param("lotId") UUID lotId);

    // Busca individual validando account e lote
    Optional<LotSale> findByIdAndAccountId(UUID id, UUID accountId);

    // Total de Kg vendido de um lote (soma de todos os caminhões de todas as vendas)
    @Query("SELECT COALESCE(SUM(t.quantityKg), 0) FROM LotSaleTruck t WHERE t.sale.lot.id = :lotId")
    BigDecimal sumSoldKgByLotId(@Param("lotId") UUID lotId);

    // Total de Kg vendido por conta — para o dashboard
    @Query("SELECT COALESCE(SUM(t.quantityKg), 0) FROM LotSaleTruck t JOIN t.sale ls WHERE ls.account.id = :accountId")
    BigDecimal sumSoldKgByAccountId(@Param("accountId") UUID accountId);

    // Receita total por conta (Kg x preco/Kg de cada venda) — para o dashboard
    @Query("SELECT COALESCE(SUM(t.quantityKg * ls.pricePerKg), 0) FROM LotSaleTruck t JOIN t.sale ls WHERE ls.account.id = :accountId")
    BigDecimal sumRevenueByAccountId(@Param("accountId") UUID accountId);

    // Totais de Kg vendido agrupados por lote — resolve N+1 na listagem
    @Query("SELECT t.sale.lot.id, COALESCE(SUM(t.quantityKg), 0) FROM LotSaleTruck t WHERE t.sale.lot.id IN :lotIds GROUP BY t.sale.lot.id")
    List<Object[]> sumSoldKgByLotIds(@Param("lotIds") List<UUID> lotIds);

    // Verifica existência de vendas sem carregar a coleção — usado antes de deletar um lote
    boolean existsByLotId(UUID lotId);
}