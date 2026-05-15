package br.com.hadryan.agro.manager.domain.labor;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EmployeePaymentExpenseRepository extends JpaRepository<EmployeePaymentExpense, EmployeePaymentExpenseId> {

    @Query("""
            SELECT link FROM EmployeePaymentExpense link
            JOIN FETCH link.expense expense
            LEFT JOIN FETCH expense.farm
            WHERE link.payment.id = :paymentId
            """)
    List<EmployeePaymentExpense> findByPaymentId(@Param("paymentId") UUID paymentId);
}
