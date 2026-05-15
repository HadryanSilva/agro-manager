package br.com.hadryan.agro.manager.domain.labor;

import br.com.hadryan.agro.manager.domain.account.Account;
import br.com.hadryan.agro.manager.domain.account.AccountMemberRepository;
import br.com.hadryan.agro.manager.domain.account.AccountRepository;
import br.com.hadryan.agro.manager.domain.farm.Farm;
import br.com.hadryan.agro.manager.domain.farm.FarmRepository;
import br.com.hadryan.agro.manager.shared.dto.PageResponse;
import br.com.hadryan.agro.manager.shared.exception.BusinessException;
import br.com.hadryan.agro.manager.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmployeeWorkEntryService {

    private final EmployeeWorkEntryRepository entryRepository;
    private final EmployeeService employeeService;
    private final FarmRepository farmRepository;
    private final AccountRepository accountRepository;
    private final AccountMemberRepository accountMemberRepository;

    @Transactional
    public EmployeeWorkEntryResponse create(UUID accountId, UUID userId, EmployeeWorkEntryRequest request) {
        Account account = validateAndGetAccount(accountId, userId);
        Employee employee = employeeService.findEmployee(request.employeeId(), accountId);
        validateActive(employee);
        if (entryRepository.existsByEmployeeIdAndWorkDate(employee.getId(), request.workDate())) {
            throw new BusinessException("Funcionario ja possui diaria nesta data", HttpStatus.CONFLICT);
        }

        EmployeeWorkEntry entry = EmployeeWorkEntry.builder()
                .account(account)
                .employee(employee)
                .farm(findFarm(accountId, request.farmId()))
                .workDate(request.workDate())
                .dailyRate(resolveDailyRate(employee, request.dailyRate()))
                .notes(request.notes())
                .build();

        return EmployeeWorkEntryResponse.from(entryRepository.save(entry));
    }

    @Transactional(readOnly = true)
    public PageResponse<EmployeeWorkEntryResponse> list(
            UUID accountId,
            UUID userId,
            UUID employeeId,
            UUID farmId,
            Boolean paid,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable) {
        validateMembership(accountId, userId);
        Page<EmployeeWorkEntryResponse> entries = entryRepository
                .findAll(entryFilters(accountId, employeeId, farmId, paid, startDate, endDate), pageable)
                .map(EmployeeWorkEntryResponse::from);
        return PageResponse.of(entries, BigDecimal.ZERO);
    }

    @Transactional(readOnly = true)
    public EmployeeWorkEntryResponse findById(UUID accountId, UUID userId, UUID entryId) {
        validateMembership(accountId, userId);
        return EmployeeWorkEntryResponse.from(findEntry(entryId, accountId));
    }

    @Transactional
    public EmployeeWorkEntryResponse update(UUID accountId, UUID userId, UUID entryId, EmployeeWorkEntryRequest request) {
        validateMembership(accountId, userId);
        EmployeeWorkEntry entry = findEntry(entryId, accountId);
        validateUnpaid(entry, "Diaria paga nao pode ser alterada");

        Employee employee = employeeService.findEmployee(request.employeeId(), accountId);
        validateActive(employee);
        if (entryRepository.existsByEmployeeIdAndWorkDateAndIdNot(employee.getId(), request.workDate(), entryId)) {
            throw new BusinessException("Funcionario ja possui diaria nesta data", HttpStatus.CONFLICT);
        }

        entry.setEmployee(employee);
        entry.setFarm(findFarm(accountId, request.farmId()));
        entry.setWorkDate(request.workDate());
        entry.setDailyRate(resolveDailyRate(employee, request.dailyRate()));
        entry.setNotes(request.notes());
        return EmployeeWorkEntryResponse.from(entryRepository.save(entry));
    }

    @Transactional
    public void delete(UUID accountId, UUID userId, UUID entryId) {
        validateMembership(accountId, userId);
        EmployeeWorkEntry entry = findEntry(entryId, accountId);
        validateUnpaid(entry, "Diaria paga nao pode ser excluida");
        entryRepository.delete(entry);
    }

    EmployeeWorkEntry findEntry(UUID entryId, UUID accountId) {
        return entryRepository.findByIdAndAccountId(entryId, accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Diaria", "id", entryId));
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

    private Farm findFarm(UUID accountId, UUID farmId) {
        if (farmId == null) {
            return null;
        }
        return farmRepository.findByIdAndAccountId(farmId, accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Lavoura", "id", farmId));
    }

    private BigDecimal resolveDailyRate(Employee employee, BigDecimal requestedDailyRate) {
        return requestedDailyRate != null ? requestedDailyRate : employee.getDailyRate();
    }

    private void validateActive(Employee employee) {
        if (!employee.isActive()) {
            throw new BusinessException("Funcionario inativo nao pode receber diaria");
        }
    }

    private void validateUnpaid(EmployeeWorkEntry entry, String message) {
        if (entry.isPaid()) {
            throw new BusinessException(message, HttpStatus.CONFLICT);
        }
    }

    private Specification<EmployeeWorkEntry> entryFilters(
            UUID accountId,
            UUID employeeId,
            UUID farmId,
            Boolean paid,
            LocalDate startDate,
            LocalDate endDate) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.equal(root.get("account").get("id"), accountId));
            if (employeeId != null) {
                predicates.add(criteriaBuilder.equal(root.get("employee").get("id"), employeeId));
            }
            if (farmId != null) {
                predicates.add(criteriaBuilder.equal(root.get("farm").get("id"), farmId));
            }
            if (paid != null) {
                predicates.add(paid
                        ? criteriaBuilder.isNotNull(root.get("paymentId"))
                        : criteriaBuilder.isNull(root.get("paymentId")));
            }
            if (startDate != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("workDate"), startDate));
            }
            if (endDate != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("workDate"), endDate));
            }
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }
}
