package br.com.hadryan.agro.manager.domain.labor;

import br.com.hadryan.agro.manager.domain.account.Account;
import br.com.hadryan.agro.manager.domain.account.AccountMemberRepository;
import br.com.hadryan.agro.manager.domain.account.AccountRepository;
import br.com.hadryan.agro.manager.domain.expense.Expense;
import br.com.hadryan.agro.manager.domain.expense.ExpenseCategory;
import br.com.hadryan.agro.manager.domain.expense.ExpenseRepository;
import br.com.hadryan.agro.manager.domain.farm.Farm;
import br.com.hadryan.agro.manager.shared.dto.PageResponse;
import br.com.hadryan.agro.manager.shared.exception.BusinessException;
import br.com.hadryan.agro.manager.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmployeePaymentService {

    private final EmployeePaymentRepository paymentRepository;
    private final EmployeePaymentExpenseRepository paymentExpenseRepository;
    private final EmployeeWorkEntryRepository workEntryRepository;
    private final EmployeeService employeeService;
    private final ExpenseRepository expenseRepository;
    private final AccountRepository accountRepository;
    private final AccountMemberRepository accountMemberRepository;

    @Transactional
    public EmployeePaymentResponse pay(UUID accountId, UUID userId, EmployeePaymentRequest request) {
        if (request.periodEnd().isBefore(request.periodStart())) {
            throw new BusinessException("Fim do periodo nao pode ser anterior ao inicio");
        }

        Account account = validateAndGetAccount(accountId, userId);
        Employee employee = employeeService.findEmployee(request.employeeId(), accountId);
        List<EmployeeWorkEntry> entries = workEntryRepository.findPendingForPayment(
                accountId, employee.getId(), request.periodStart(), request.periodEnd());

        if (entries.isEmpty()) {
            throw new BusinessException("Nao ha diarias pendentes para o periodo informado");
        }

        BigDecimal total = entries.stream()
                .map(EmployeeWorkEntry::getDailyRate)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        EmployeePayment payment = paymentRepository.save(EmployeePayment.builder()
                .account(account)
                .employee(employee)
                .periodStart(request.periodStart())
                .periodEnd(request.periodEnd())
                .paymentDate(request.paymentDate())
                .totalAmount(total)
                .notes(request.notes())
                .build());

        List<GeneratedExpenseResponse> generatedExpenses = new ArrayList<>();
        Map<UUID, List<EmployeeWorkEntry>> entriesByFarm = entries.stream()
                .filter(entry -> entry.getFarm() != null)
                .collect(Collectors.groupingBy(entry -> entry.getFarm().getId(), LinkedHashMap::new, Collectors.toList()));

        for (List<EmployeeWorkEntry> farmEntries : entriesByFarm.values()) {
            generatedExpenses.add(createExpenseForGroup(account, farmEntries.get(0).getFarm(), employee, payment, request, farmEntries));
        }

        List<EmployeeWorkEntry> generalEntries = entries.stream()
                .filter(entry -> entry.getFarm() == null)
                .toList();
        if (!generalEntries.isEmpty()) {
            generatedExpenses.add(createExpenseForGroup(account, null, employee, payment, request, generalEntries));
        }

        entries.forEach(entry -> entry.setPaymentId(payment.getId()));
        workEntryRepository.saveAll(entries);

        return EmployeePaymentResponse.from(payment, entries.size(), generatedExpenses);
    }

    @Transactional(readOnly = true)
    public PageResponse<EmployeePaymentResponse> list(
            UUID accountId,
            UUID userId,
            UUID employeeId,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable) {
        validateMembership(accountId, userId);
        Page<EmployeePaymentResponse> payments = paymentRepository
                .findPayments(accountId, employeeId, startDate, endDate, pageable)
                .map(payment -> EmployeePaymentResponse.from(
                        payment,
                        workEntryRepository.countByPaymentId(payment.getId()),
                        generatedExpensesFor(payment.getId())));
        return PageResponse.of(payments, BigDecimal.ZERO);
    }

    @Transactional(readOnly = true)
    public EmployeePaymentResponse findById(UUID accountId, UUID userId, UUID paymentId) {
        validateMembership(accountId, userId);
        EmployeePayment payment = paymentRepository.findByIdAndAccountId(paymentId, accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Pagamento", "id", paymentId));
        return EmployeePaymentResponse.from(
                payment,
                workEntryRepository.countByPaymentId(payment.getId()),
                generatedExpensesFor(payment.getId()));
    }

    private GeneratedExpenseResponse createExpenseForGroup(
            Account account,
            Farm farm,
            Employee employee,
            EmployeePayment payment,
            EmployeePaymentRequest request,
            List<EmployeeWorkEntry> entries) {
        BigDecimal amount = entries.stream()
                .map(EmployeeWorkEntry::getDailyRate)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Expense expense = Expense.builder()
                .account(account)
                .farm(farm)
                .description("Diarias de %s - %s a %s".formatted(
                        employee.getName(), request.periodStart(), request.periodEnd()))
                .category(ExpenseCategory.SERVICO)
                .value(amount)
                .competenceDate(request.periodEnd())
                .paymentDate(request.paymentDate())
                .notes(buildExpenseNotes(request.notes(), entries))
                .build();

        Expense savedExpense = expenseRepository.save(expense);
        paymentExpenseRepository.save(EmployeePaymentExpense.builder()
                .id(new EmployeePaymentExpenseId(payment.getId(), savedExpense.getId()))
                .payment(payment)
                .expense(savedExpense)
                .build());

        return new GeneratedExpenseResponse(
                savedExpense.getId(),
                farm != null ? farm.getId() : null,
                farm != null ? farm.getName() : null,
                amount
        );
    }

    private List<GeneratedExpenseResponse> generatedExpensesFor(UUID paymentId) {
        return paymentExpenseRepository.findByPaymentId(paymentId).stream()
                .map(link -> new GeneratedExpenseResponse(
                        link.getExpense().getId(),
                        link.getExpense().getFarm() != null ? link.getExpense().getFarm().getId() : null,
                        link.getExpense().getFarm() != null ? link.getExpense().getFarm().getName() : null,
                        link.getExpense().getValue()
                ))
                .toList();
    }

    private String buildExpenseNotes(String paymentNotes, List<EmployeeWorkEntry> entries) {
        String dates = entries.stream()
                .map(entry -> entry.getWorkDate().toString())
                .collect(Collectors.joining(", "));
        if (paymentNotes == null || paymentNotes.isBlank()) {
            return "Diarias incluidas: " + dates;
        }
        return paymentNotes + "\nDiarias incluidas: " + dates;
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
