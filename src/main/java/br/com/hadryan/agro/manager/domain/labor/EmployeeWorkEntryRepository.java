package br.com.hadryan.agro.manager.domain.labor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmployeeWorkEntryRepository extends JpaRepository<EmployeeWorkEntry, UUID> {

    boolean existsByEmployeeIdAndWorkDate(UUID employeeId, LocalDate workDate);

    boolean existsByEmployeeIdAndWorkDateAndIdNot(UUID employeeId, LocalDate workDate, UUID id);

    Optional<EmployeeWorkEntry> findByIdAndAccountId(UUID id, UUID accountId);

    long countByPaymentId(UUID paymentId);

    @Modifying
    @Query("DELETE FROM EmployeeWorkEntry e WHERE e.account.id = :accountId")
    void deleteByAccountId(@Param("accountId") UUID accountId);

    @Query(
            value = """
                    SELECT e FROM EmployeeWorkEntry e
                    JOIN FETCH e.employee emp
                    LEFT JOIN FETCH e.farm f
                    WHERE e.account.id = :accountId
                      AND (:employeeId IS NULL OR emp.id = :employeeId)
                      AND (:farmId IS NULL OR f.id = :farmId)
                      AND (:paid IS NULL OR
                           (:paid = true AND e.paymentId IS NOT NULL) OR
                           (:paid = false AND e.paymentId IS NULL))
                      AND (:startDate IS NULL OR e.workDate >= :startDate)
                      AND (:endDate IS NULL OR e.workDate <= :endDate)
                    ORDER BY e.workDate DESC, e.createdAt DESC
                    """,
            countQuery = """
                    SELECT COUNT(e) FROM EmployeeWorkEntry e
                    JOIN e.employee emp
                    LEFT JOIN e.farm f
                    WHERE e.account.id = :accountId
                      AND (:employeeId IS NULL OR emp.id = :employeeId)
                      AND (:farmId IS NULL OR f.id = :farmId)
                      AND (:paid IS NULL OR
                           (:paid = true AND e.paymentId IS NOT NULL) OR
                           (:paid = false AND e.paymentId IS NULL))
                      AND (:startDate IS NULL OR e.workDate >= :startDate)
                      AND (:endDate IS NULL OR e.workDate <= :endDate)
                    """
    )
    Page<EmployeeWorkEntry> findEntries(
            @Param("accountId") UUID accountId,
            @Param("employeeId") UUID employeeId,
            @Param("farmId") UUID farmId,
            @Param("paid") Boolean paid,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable
    );

    @Query("""
            SELECT e FROM EmployeeWorkEntry e
            LEFT JOIN FETCH e.farm
            WHERE e.account.id = :accountId
              AND e.employee.id = :employeeId
              AND e.paymentId IS NULL
              AND e.workDate BETWEEN :periodStart AND :periodEnd
            ORDER BY e.workDate ASC
            """)
    List<EmployeeWorkEntry> findPendingForPayment(
            @Param("accountId") UUID accountId,
            @Param("employeeId") UUID employeeId,
            @Param("periodStart") LocalDate periodStart,
            @Param("periodEnd") LocalDate periodEnd
    );
}
