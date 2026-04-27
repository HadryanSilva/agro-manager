package br.com.hadryan.agro.manager.domain.expense;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, UUID> {

    // Lista todas as despesas de uma lavoura ordenadas pela data de competência mais recente
    List<Expense> findByFarmIdOrderByCompetenceDateDesc(UUID farmId);

    // Busca uma despesa garantindo que pertence à lavoura informada
    Optional<Expense> findByIdAndFarmId(UUID id, UUID farmId);

    // Soma total das despesas de uma lavoura
    @Query("SELECT COALESCE(SUM(e.value), 0) FROM Expense e WHERE e.farm.id = :farmId")
    BigDecimal sumValueByFarmId(UUID farmId);

    // Soma apenas das despesas já pagas de uma lavoura
    @Query("SELECT COALESCE(SUM(e.value), 0) FROM Expense e WHERE e.farm.id = :farmId AND e.paymentDate IS NOT NULL")
    BigDecimal sumPaidValueByFarmId(UUID farmId);

    // Soma total de todas as despesas de uma conta
    @Query("SELECT COALESCE(SUM(e.value), 0) FROM Expense e WHERE e.farm.account.id = :accountId")
    BigDecimal sumValueByAccountId(UUID accountId);

    // Soma das despesas pagas de uma conta
    @Query("SELECT COALESCE(SUM(e.value), 0) FROM Expense e WHERE e.farm.account.id = :accountId AND e.paymentDate IS NOT NULL")
    BigDecimal sumPaidValueByAccountId(UUID accountId);

    /**
     * Listagem paginada de transações com filtros opcionais.
     * Todos os parâmetros de filtro são opcionais — quando nulos, o critério é ignorado.
     */
    @Query("""
            SELECT e FROM Expense e
            JOIN FETCH e.farm f
            WHERE f.account.id = :accountId
              AND (:farmId      IS NULL OR f.id           = :farmId)
              AND (:category    IS NULL OR e.category     = :category)
              AND (:paid        IS NULL OR
                   (:paid = true  AND e.paymentDate IS NOT NULL) OR
                   (:paid = false AND e.paymentDate IS NULL))
              AND (:startDate   IS NULL OR e.competenceDate >= :startDate)
              AND (:endDate     IS NULL OR e.competenceDate <= :endDate)
            ORDER BY e.competenceDate DESC, e.createdAt DESC
            """)
    Page<Expense> findTransactions(
            @Param("accountId") UUID accountId,
            @Param("farmId")    UUID farmId,
            @Param("category")  ExpenseCategory category,
            @Param("paid")      Boolean paid,
            @Param("startDate") LocalDate startDate,
            @Param("endDate")   LocalDate endDate,
            Pageable pageable
    );

    /**
     * Soma total das transações filtradas — usada para o totalizador da página atual.
     */
    @Query("""
            SELECT COALESCE(SUM(e.value), 0) FROM Expense e
            JOIN e.farm f
            WHERE f.account.id = :accountId
              AND (:farmId      IS NULL OR f.id           = :farmId)
              AND (:category    IS NULL OR e.category     = :category)
              AND (:paid        IS NULL OR
                   (:paid = true  AND e.paymentDate IS NOT NULL) OR
                   (:paid = false AND e.paymentDate IS NULL))
              AND (:startDate   IS NULL OR e.competenceDate >= :startDate)
              AND (:endDate     IS NULL OR e.competenceDate <= :endDate)
            """)
    BigDecimal sumTransactions(
            @Param("accountId") UUID accountId,
            @Param("farmId")    UUID farmId,
            @Param("category")  ExpenseCategory category,
            @Param("paid")      Boolean paid,
            @Param("startDate") LocalDate startDate,
            @Param("endDate")   LocalDate endDate
    );
}