package br.com.hadryan.agro.manager.domain.labor;

import br.com.hadryan.agro.manager.domain.account.Account;
import br.com.hadryan.agro.manager.domain.account.AccountMemberRepository;
import br.com.hadryan.agro.manager.domain.account.AccountRepository;
import br.com.hadryan.agro.manager.shared.exception.BusinessException;
import br.com.hadryan.agro.manager.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final AccountRepository accountRepository;
    private final AccountMemberRepository accountMemberRepository;

    @Transactional
    public EmployeeResponse create(UUID accountId, UUID userId, EmployeeRequest request) {
        Account account = validateAndGetAccount(accountId, userId);
        Employee employee = Employee.builder()
                .account(account)
                .name(request.name().trim())
                .dailyRate(request.dailyRate())
                .active(true)
                .notes(request.notes())
                .build();
        return EmployeeResponse.from(employeeRepository.save(employee));
    }

    @Transactional(readOnly = true)
    public List<EmployeeResponse> list(UUID accountId, UUID userId, Boolean active) {
        validateMembership(accountId, userId);
        List<Employee> employees = active == null
                ? employeeRepository.findByAccountIdOrderByNameAsc(accountId)
                : employeeRepository.findByAccountIdAndActiveOrderByNameAsc(accountId, active);
        return employees.stream().map(EmployeeResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public EmployeeResponse findById(UUID accountId, UUID userId, UUID employeeId) {
        validateMembership(accountId, userId);
        return EmployeeResponse.from(findEmployee(employeeId, accountId));
    }

    @Transactional
    public EmployeeResponse update(UUID accountId, UUID userId, UUID employeeId, EmployeeRequest request) {
        validateMembership(accountId, userId);
        Employee employee = findEmployee(employeeId, accountId);
        employee.setName(request.name().trim());
        employee.setDailyRate(request.dailyRate());
        employee.setNotes(request.notes());
        return EmployeeResponse.from(employeeRepository.save(employee));
    }

    @Transactional
    public EmployeeResponse activate(UUID accountId, UUID userId, UUID employeeId) {
        validateMembership(accountId, userId);
        Employee employee = findEmployee(employeeId, accountId);
        employee.setActive(true);
        return EmployeeResponse.from(employeeRepository.save(employee));
    }

    @Transactional
    public EmployeeResponse deactivate(UUID accountId, UUID userId, UUID employeeId) {
        validateMembership(accountId, userId);
        Employee employee = findEmployee(employeeId, accountId);
        employee.setActive(false);
        return EmployeeResponse.from(employeeRepository.save(employee));
    }

    Employee findEmployee(UUID employeeId, UUID accountId) {
        return employeeRepository.findByIdAndAccountId(employeeId, accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Funcionario", "id", employeeId));
    }

    private Account validateAndGetAccount(UUID accountId, UUID userId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Conta", "id", accountId));
        if (!accountMemberRepository.existsByAccountIdAndUserId(accountId, userId)) {
            throw new BusinessException("Acesso negado a esta conta", HttpStatus.FORBIDDEN);
        }
        return account;
    }

    private void validateMembership(UUID accountId, UUID userId) {
        if (!accountRepository.existsById(accountId)) {
            throw new ResourceNotFoundException("Conta", "id", accountId);
        }
        if (!accountMemberRepository.existsByAccountIdAndUserId(accountId, userId)) {
            throw new BusinessException("Acesso negado a esta conta", HttpStatus.FORBIDDEN);
        }
    }
}
