package br.com.hadryan.agro.manager.domain.labor;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmployeePaymentRepository extends JpaRepository<EmployeePayment, UUID>,
        JpaSpecificationExecutor<EmployeePayment> {

    Optional<EmployeePayment> findByIdAndAccountId(UUID id, UUID accountId);

    @Modifying
    @Query("DELETE FROM EmployeePayment p WHERE p.account.id = :accountId")
    void deleteByAccountId(@Param("accountId") UUID accountId);

}
