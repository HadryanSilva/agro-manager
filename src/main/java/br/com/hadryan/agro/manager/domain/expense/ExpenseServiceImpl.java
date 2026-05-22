package br.com.hadryan.agro.manager.domain.expense;

import br.com.hadryan.agro.manager.domain.account.Account;
import br.com.hadryan.agro.manager.domain.account.AccountMemberRepository;
import br.com.hadryan.agro.manager.domain.account.AccountRepository;
import br.com.hadryan.agro.manager.domain.farm.Farm;
import br.com.hadryan.agro.manager.domain.farm.FarmActivityServiceImpl;
import br.com.hadryan.agro.manager.domain.farm.FarmActivityType;
import br.com.hadryan.agro.manager.domain.farm.FarmRepository;
import br.com.hadryan.agro.manager.shared.exception.BusinessException;
import br.com.hadryan.agro.manager.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.hadryan.agro.manager.shared.dto.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Serviço de gerenciamento de despesas.
 * Suporta dois tipos:
 *  - Despesas de lavoura: farmId obrigatório no path
 *  - Despesas gerais da conta: farmId nulo
 * Todas as operações validam membership do usuário na conta.
 */
@Service
@RequiredArgsConstructor
public class ExpenseServiceImpl implements  ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final FarmRepository farmRepository;
    private final AccountRepository accountRepository;
    private final AccountMemberRepository accountMemberRepository;
    private final FarmActivityServiceImpl activityService;

    // ── Despesas de lavoura (farmId obrigatório) ──────────────────────────────

    @Transactional
    public ExpenseResponse create(UUID accountId, UUID farmId, UUID userId, ExpenseRequest request) {
        Farm farm = findFarmAndValidate(accountId, farmId, userId);

        validateCreditPurchase(request);

        Expense expense = Expense.builder()
                .account(farm.getAccount())  // sempre presente — garante o tenant correto
                .farm(farm)
                .description(request.description())
                .category(request.category())
                .value(request.value())
                .competenceDate(request.competenceDate())
                .paymentDate(request.paymentDate())
                .creditPurchase(Boolean.TRUE.equals(request.creditPurchase()))
                .dueDate(Boolean.TRUE.equals(request.creditPurchase()) ? request.dueDate() : null)
                .notes(request.notes())
                .build();

        ExpenseResponse saved = ExpenseResponse.from(expenseRepository.save(expense));

        // Usa String.formatted() para maior legibilidade na montagem da descrição
        String categoryLabel = request.category() == ExpenseCategory.INSUMO ? "Insumo" : "Serviço";
        activityService.record(
                farmId, userId,
                FarmActivityType.EXPENSE_CREATED,
                "Despesa registrada: %s — %s — R$ %s".formatted(
                        request.description(), categoryLabel, request.value()),
                saved.id()
        );
        return saved;
    }

    @Transactional(readOnly = true)
    public PageResponse<ExpenseResponse> findAll(UUID accountId, UUID farmId, UUID userId, Pageable pageable) {
        findFarmAndValidate(accountId, farmId, userId);
        Page<ExpenseResponse> page = expenseRepository
                .findByFarmIdOrderByCompetenceDateDesc(farmId, pageable)
                .map(ExpenseResponse::from);
        return PageResponse.of(page, expenseRepository.sumValueByFarmId(farmId));
    }

    @Transactional(readOnly = true)
    public ExpenseResponse findById(UUID accountId, UUID farmId, UUID userId, UUID expenseId) {
        findFarmAndValidate(accountId, farmId, userId);
        return ExpenseResponse.from(findExpense(expenseId, farmId));
    }

    @Transactional
    public ExpenseResponse update(UUID accountId, UUID farmId, UUID userId, UUID expenseId, ExpenseRequest request) {
        findFarmAndValidate(accountId, farmId, userId);
        validateCreditPurchase(request);
        Expense expense = findExpense(expenseId, farmId);

        expense.setDescription(request.description());
        expense.setCategory(request.category());
        expense.setValue(request.value());
        expense.setCompetenceDate(request.competenceDate());
        expense.setPaymentDate(request.paymentDate());
        expense.setCreditPurchase(Boolean.TRUE.equals(request.creditPurchase()));
        expense.setDueDate(Boolean.TRUE.equals(request.creditPurchase()) ? request.dueDate() : null);
        expense.setNotes(request.notes());

        ExpenseResponse updated = ExpenseResponse.from(expenseRepository.save(expense));
        activityService.record(
                farmId, userId,
                FarmActivityType.EXPENSE_UPDATED,
                "Despesa atualizada: %s".formatted(request.description()),
                updated.id()
        );
        return updated;
    }

    @Transactional
    public void delete(UUID accountId, UUID farmId, UUID userId, UUID expenseId) {
        findFarmAndValidate(accountId, farmId, userId);
        Expense expense = findExpense(expenseId, farmId);
        String expenseDesc = expense.getDescription();
        expenseRepository.delete(expense);
        activityService.record(
                farmId, userId,
                FarmActivityType.EXPENSE_DELETED,
                "Despesa removida: %s".formatted(expenseDesc),
                expenseId
        );
    }

    @Transactional
    public ExpenseResponse markAsPaid(UUID accountId, UUID farmId, UUID userId, UUID expenseId) {
        findFarmAndValidate(accountId, farmId, userId);
        Expense expense = findExpense(expenseId, farmId);
        expense.setPaymentDate(LocalDate.now());
        ExpenseResponse paid = ExpenseResponse.from(expenseRepository.save(expense));
        activityService.record(
                farmId, userId,
                FarmActivityType.EXPENSE_PAID,
                "Despesa marcada como paga: %s".formatted(expense.getDescription()),
                paid.id()
        );
        return paid;
    }

    // ── Despesas gerais da conta (sem lavoura) ────────────────────────────────

    @Transactional
    public ExpenseResponse createGeneral(UUID accountId, UUID userId, ExpenseRequest request) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Conta", "id", accountId));

        if (!accountMemberRepository.existsByAccountIdAndUserId(accountId, userId)) {
            throw new BusinessException("Acesso negado a esta conta", HttpStatus.FORBIDDEN);
        }

        validateCreditPurchase(request);

        Expense expense = Expense.builder()
                .account(account)
                .farm(null)
                .description(request.description())
                .category(request.category())
                .value(request.value())
                .competenceDate(request.competenceDate())
                .paymentDate(request.paymentDate())
                .creditPurchase(Boolean.TRUE.equals(request.creditPurchase()))
                .dueDate(Boolean.TRUE.equals(request.creditPurchase()) ? request.dueDate() : null)
                .notes(request.notes())
                .build();

        return ExpenseResponse.from(expenseRepository.save(expense));
    }

    @Transactional(readOnly = true)
    public ExpenseResponse findGeneralById(UUID accountId, UUID userId, UUID expenseId) {
        validateMembership(accountId, userId);
        return ExpenseResponse.from(findGeneralExpense(expenseId, accountId));
    }

    @Transactional
    public ExpenseResponse updateGeneral(UUID accountId, UUID userId, UUID expenseId, ExpenseRequest request) {
        validateMembership(accountId, userId);
        validateCreditPurchase(request);
        Expense expense = findGeneralExpense(expenseId, accountId);

        expense.setDescription(request.description());
        expense.setCategory(request.category());
        expense.setValue(request.value());
        expense.setCompetenceDate(request.competenceDate());
        expense.setPaymentDate(request.paymentDate());
        expense.setCreditPurchase(Boolean.TRUE.equals(request.creditPurchase()));
        expense.setDueDate(Boolean.TRUE.equals(request.creditPurchase()) ? request.dueDate() : null);
        expense.setNotes(request.notes());

        return ExpenseResponse.from(expenseRepository.save(expense));
    }

    @Transactional
    public void deleteGeneral(UUID accountId, UUID userId, UUID expenseId) {
        validateMembership(accountId, userId);
        Expense expense = findGeneralExpense(expenseId, accountId);
        expenseRepository.delete(expense);
    }

    @Transactional
    public ExpenseResponse markGeneralAsPaid(UUID accountId, UUID userId, UUID expenseId) {
        validateMembership(accountId, userId);
        Expense expense = findGeneralExpense(expenseId, accountId);
        expense.setPaymentDate(LocalDate.now());
        return ExpenseResponse.from(expenseRepository.save(expense));
    }

    // ── Utilitários privados ──────────────────────────────────────────────────

    private Farm findFarmAndValidate(UUID accountId, UUID farmId, UUID userId) {
        accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Conta", "id", accountId));

        if (!accountMemberRepository.existsByAccountIdAndUserId(accountId, userId)) {
            throw new BusinessException("Acesso negado a esta conta", HttpStatus.FORBIDDEN);
        }

        return farmRepository.findByIdAndAccountId(farmId, accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Lavoura", "id", farmId));
    }

    private void validateMembership(UUID accountId, UUID userId) {
        accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Conta", "id", accountId));

        if (!accountMemberRepository.existsByAccountIdAndUserId(accountId, userId)) {
            throw new BusinessException("Acesso negado a esta conta", HttpStatus.FORBIDDEN);
        }
    }

    private Expense findExpense(UUID expenseId, UUID farmId) {
        return expenseRepository.findByIdAndFarmId(expenseId, farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Despesa", "id", expenseId));
    }

    private Expense findGeneralExpense(UUID expenseId, UUID accountId) {
        return expenseRepository.findByIdAndAccountIdAndFarmIsNull(expenseId, accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Despesa", "id", expenseId));
    }

    private void validateCreditPurchase(ExpenseRequest request) {
        if (Boolean.TRUE.equals(request.creditPurchase()) && request.dueDate() == null) {
            throw new BusinessException("Data de vencimento obrigatória para compra no prazo", HttpStatus.BAD_REQUEST);
        }
    }

    @Transactional(readOnly = true)
    public List<ExpenseResponse> findUpcoming(UUID accountId, UUID userId, int days) {
        validateMembership(accountId, userId);
        LocalDate today = LocalDate.now();
        LocalDate until = today.plusDays(days);
        return expenseRepository.findUpcomingCreditExpenses(accountId, today, until)
                .stream()
                .map(ExpenseResponse::from)
                .toList();
    }
}