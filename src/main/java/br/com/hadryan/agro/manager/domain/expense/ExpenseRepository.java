package br.com.hadryan.agro.manager.domain.expense;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, UUID> {

    // Lista todas as despesas de uma lavoura ordenadas pela data de competência mais recente
    List<Expense> findByFarmIdOrderByCompetenceDateDesc(UUID farmId);

    // Busca uma despesa garantindo que pertence à lavoura informada
    Optional<Expense> findByIdAndFarmId(UUID id, UUID farmId);

    // Soma total das despesas de uma lavoura (usado no dashboard futuro)
    @Query("SELECT COALESCE(SUM(e.value), 0) FROM Expense e WHERE e.farm.id = :farmId")
    BigDecimal sumValueByFarmId(UUID farmId);

    // Soma apenas das despesas já pagas de uma lavoura
    @Query("SELECT COALESCE(SUM(e.value), 0) FROM Expense e WHERE e.farm.id = :farmId AND e.paymentDate IS NOT NULL")
    BigDecimal sumPaidValueByFarmId(UUID farmId);
}