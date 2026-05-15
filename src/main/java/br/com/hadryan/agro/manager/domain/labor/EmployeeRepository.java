package br.com.hadryan.agro.manager.domain.labor;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, UUID> {

    List<Employee> findByAccountIdOrderByNameAsc(UUID accountId);

    List<Employee> findByAccountIdAndActiveOrderByNameAsc(UUID accountId, boolean active);

    Optional<Employee> findByIdAndAccountId(UUID id, UUID accountId);
}
