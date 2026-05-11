package br.com.hadryan.agro.manager.domain.expense;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, UUID> {

    // Lista todas as despesas de uma lavoura (para relatórios — não usar em endpoints de listagem)
    List<Expense> findByFarmIdOrderByCompetenceDateDesc(UUID farmId);

    // Listagem paginada de despesas de uma lavoura
    Page<Expense> findByFarmIdOrderByCompetenceDateDesc(UUID farmId, Pageable pageable);

    // Busca uma despesa garantindo que pertence à lavoura informada
    Optional<Expense> findByIdAndFarmId(UUID id, UUID farmId);

    // Busca uma despesa geral (farm nulo) garantindo que pertence à conta informada
    @Query("SELECT e FROM Expense e WHERE e.id = :id AND e.farm IS NULL AND e.account.id = :accountId")
    Optional<Expense> findByIdAndAccountIdAndFarmIsNull(@Param("id") UUID id, @Param("accountId") UUID accountId);

    // Soma total das despesas de uma lavoura
    @Query("SELECT COALESCE(SUM(e.value), 0) FROM Expense e WHERE e.farm.id = :farmId")
    BigDecimal sumValueByFarmId(UUID farmId);

    // Soma apenas das despesas já pagas de uma lavoura
    @Query("SELECT COALESCE(SUM(e.value), 0) FROM Expense e WHERE e.farm.id = :farmId AND e.paymentDate IS NOT NULL")
    BigDecimal sumPaidValueByFarmId(UUID farmId);

    // Soma total de todas as despesas de uma conta (inclui gerais e de lavouras)
    @Query("SELECT COALESCE(SUM(e.value), 0) FROM Expense e WHERE e.account.id = :accountId")
    BigDecimal sumValueByAccountId(UUID accountId);

    // Soma das despesas pagas de uma conta
    @Query("SELECT COALESCE(SUM(e.value), 0) FROM Expense e WHERE e.account.id = :accountId AND e.paymentDate IS NOT NULL")
    BigDecimal sumPaidValueByAccountId(UUID accountId);

    /**
     * Listagem paginada de transações com filtros opcionais.
     * general=true filtra apenas despesas sem lavoura (gerais da conta).
     * general=false filtra apenas despesas com lavoura.
     * general=null retorna todas.
     */
    @Query(
        value = """
            SELECT e FROM Expense e
            LEFT JOIN FETCH e.farm f
            WHERE e.account.id = :accountId
              AND (:farmId    IS NULL OR f.id         = :farmId)
              AND (:general   IS NULL OR
                   (:general = true  AND e.farm IS NULL) OR
                   (:general = false AND e.farm IS NOT NULL))
              AND (:category  IS NULL OR e.category   = :category)
              AND (:paid      IS NULL OR
                   (:paid = true  AND e.paymentDate IS NOT NULL) OR
                   (:paid = false AND e.paymentDate IS NULL))
              AND (:startDate IS NULL OR e.competenceDate >= :startDate)
              AND (:endDate   IS NULL OR e.competenceDate <= :endDate)
            ORDER BY e.competenceDate DESC, e.createdAt DESC
            """,
        countQuery = """
            SELECT COUNT(e) FROM Expense e
            LEFT JOIN e.farm f
            WHERE e.account.id = :accountId
              AND (:farmId    IS NULL OR f.id         = :farmId)
              AND (:general   IS NULL OR
                   (:general = true  AND e.farm IS NULL) OR
                   (:general = false AND e.farm IS NOT NULL))
              AND (:category  IS NULL OR e.category   = :category)
              AND (:paid      IS NULL OR
                   (:paid = true  AND e.paymentDate IS NOT NULL) OR
                   (:paid = false AND e.paymentDate IS NULL))
              AND (:startDate IS NULL OR e.competenceDate >= :startDate)
              AND (:endDate   IS NULL OR e.competenceDate <= :endDate)
            """
    )
    Page<Expense> findTransactions(
            @Param("accountId") UUID accountId,
            @Param("farmId")    UUID farmId,
            @Param("general")   Boolean general,
            @Param("category")  ExpenseCategory category,
            @Param("paid")      Boolean paid,
            @Param("startDate") LocalDate startDate,
            @Param("endDate")   LocalDate endDate,
            Pageable pageable
    );

    /**
     * Retorna total e total pago por lavoura em uma única query (GROUP BY).
     * Evita N+1 ao montar lista de lavouras recentes no dashboard.
     * Retorna Object[] { farmId, totalValue, totalPaid }.
     */
    @Query("""
            SELECT e.farm.id,
                   COALESCE(SUM(e.value), 0),
                   COALESCE(SUM(CASE WHEN e.paymentDate IS NOT NULL THEN e.value ELSE 0 END), 0)
            FROM Expense e
            WHERE e.farm.id IN :farmIds
            GROUP BY e.farm.id
            """)
    List<Object[]> sumValueAndPaidByFarmIds(@Param("farmIds") Collection<UUID> farmIds);

    /**
     * Soma total das transações filtradas — usada para o totalizador da página.
     */
    @Query("""
            SELECT COALESCE(SUM(e.value), 0) FROM Expense e
            LEFT JOIN e.farm f
            WHERE e.account.id = :accountId
              AND (:farmId    IS NULL OR f.id         = :farmId)
              AND (:general   IS NULL OR
                   (:general = true  AND e.farm IS NULL) OR
                   (:general = false AND e.farm IS NOT NULL))
              AND (:category  IS NULL OR e.category   = :category)
              AND (:paid      IS NULL OR
                   (:paid = true  AND e.paymentDate IS NOT NULL) OR
                   (:paid = false AND e.paymentDate IS NULL))
              AND (:startDate IS NULL OR e.competenceDate >= :startDate)
              AND (:endDate   IS NULL OR e.competenceDate <= :endDate)
            """)
    BigDecimal sumTransactions(
            @Param("accountId") UUID accountId,
            @Param("farmId")    UUID farmId,
            @Param("general")   Boolean general,
            @Param("category")  ExpenseCategory category,
            @Param("paid")      Boolean paid,
            @Param("startDate") LocalDate startDate,
            @Param("endDate")   LocalDate endDate
    );
}