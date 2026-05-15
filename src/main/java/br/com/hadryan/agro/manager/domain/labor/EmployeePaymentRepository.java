package br.com.hadryan.agro.manager.domain.labor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmployeePaymentRepository extends JpaRepository<EmployeePayment, UUID> {

    Optional<EmployeePayment> findByIdAndAccountId(UUID id, UUID accountId);

    @Query(
            value = """
                    SELECT p FROM EmployeePayment p
                    JOIN FETCH p.employee e
                    WHERE p.account.id = :accountId
                      AND (:employeeId IS NULL OR e.id = :employeeId)
                      AND (:startDate IS NULL OR p.paymentDate >= :startDate)
                      AND (:endDate IS NULL OR p.paymentDate <= :endDate)
                    ORDER BY p.paymentDate DESC, p.createdAt DESC
                    """,
            countQuery = """
                    SELECT COUNT(p) FROM EmployeePayment p
                    JOIN p.employee e
                    WHERE p.account.id = :accountId
                      AND (:employeeId IS NULL OR e.id = :employeeId)
                      AND (:startDate IS NULL OR p.paymentDate >= :startDate)
                      AND (:endDate IS NULL OR p.paymentDate <= :endDate)
                    """
    )
    Page<EmployeePayment> findPayments(
            @Param("accountId") UUID accountId,
            @Param("employeeId") UUID employeeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable
    );
}
