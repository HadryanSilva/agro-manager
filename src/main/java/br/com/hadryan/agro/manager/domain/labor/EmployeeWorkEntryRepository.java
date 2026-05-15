package br.com.hadryan.agro.manager.domain.labor;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmployeeWorkEntryRepository extends JpaRepository<EmployeeWorkEntry, UUID>,
        JpaSpecificationExecutor<EmployeeWorkEntry> {

    boolean existsByEmployeeIdAndWorkDate(UUID employeeId, LocalDate workDate);

    boolean existsByEmployeeIdAndWorkDateAndIdNot(UUID employeeId, LocalDate workDate, UUID id);

    Optional<EmployeeWorkEntry> findByIdAndAccountId(UUID id, UUID accountId);

    long countByPaymentId(UUID paymentId);

    @Modifying
    @Query("DELETE FROM EmployeeWorkEntry e WHERE e.account.id = :accountId")
    void deleteByAccountId(@Param("accountId") UUID accountId);

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
