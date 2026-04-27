package br.com.hadryan.agro.manager.domain.expense;

import br.com.hadryan.agro.manager.domain.account.AccountMemberRepository;
import br.com.hadryan.agro.manager.domain.account.AccountRepository;
import br.com.hadryan.agro.manager.domain.farm.Farm;
import br.com.hadryan.agro.manager.domain.farm.FarmActivityService;
import br.com.hadryan.agro.manager.domain.farm.FarmActivityType;
import br.com.hadryan.agro.manager.domain.farm.FarmRepository;
import br.com.hadryan.agro.manager.shared.exception.BusinessException;
import br.com.hadryan.agro.manager.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Serviço de gerenciamento de despesas por lavoura.
 * Todas as operações validam membership do usuário na conta
 * e que a lavoura pertence à conta antes de prosseguir.
 */
@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final FarmRepository farmRepository;
    private final AccountRepository accountRepository;
    private final AccountMemberRepository accountMemberRepository;
    private final FarmActivityService activityService;

    @Transactional
    public ExpenseResponse create(UUID accountId, UUID farmId, UUID userId, ExpenseRequest request) {
        Farm farm = findFarmAndValidate(accountId, farmId, userId);

        Expense expense = Expense.builder()
                .farm(farm)
                .description(request.description())
                .category(request.category())
                .value(request.value())
                .competenceDate(request.competenceDate())
                .paymentDate(request.paymentDate())
                .notes(request.notes())
                .build();

        ExpenseResponse saved = ExpenseResponse.from(expenseRepository.save(expense));
        activityService.record(
                farmId, userId,
                FarmActivityType.EXPENSE_CREATED,
                "Despesa registrada: " + request.description() + " — " +
                        (request.category() == ExpenseCategory.INSUMO ? "Insumo" : "Serviço") +
                        " — R$ " + request.value(),
                saved.id()
        );
        return saved;
    }

    @Transactional(readOnly = true)
    public List<ExpenseResponse> findAll(UUID accountId, UUID farmId, UUID userId) {
        findFarmAndValidate(accountId, farmId, userId);
        return expenseRepository.findByFarmIdOrderByCompetenceDateDesc(farmId)
                .stream()
                .map(ExpenseResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ExpenseResponse findById(UUID accountId, UUID farmId, UUID userId, UUID expenseId) {
        findFarmAndValidate(accountId, farmId, userId);
        return ExpenseResponse.from(findExpense(expenseId, farmId));
    }

    @Transactional
    public ExpenseResponse update(UUID accountId, UUID farmId, UUID userId, UUID expenseId, ExpenseRequest request) {
        findFarmAndValidate(accountId, farmId, userId);
        Expense expense = findExpense(expenseId, farmId);

        expense.setDescription(request.description());
        expense.setCategory(request.category());
        expense.setValue(request.value());
        expense.setCompetenceDate(request.competenceDate());
        expense.setPaymentDate(request.paymentDate());
        expense.setNotes(request.notes());

        ExpenseResponse updated = ExpenseResponse.from(expenseRepository.save(expense));
        activityService.record(
                farmId, userId,
                FarmActivityType.EXPENSE_UPDATED,
                "Despesa atualizada: " + request.description(),
                updated.id()
        );
        return updated;
    }

    @Transactional
    public void delete(UUID accountId, UUID farmId, UUID userId, UUID expenseId) {
        findFarmAndValidate(accountId, farmId, userId);
        Expense expense = findExpense(expenseId, farmId);
        expenseRepository.delete(expense);
        String expenseDesc = expense.getDescription();
        expenseRepository.delete(expense);
        activityService.record(
                farmId, userId,
                FarmActivityType.EXPENSE_DELETED,
                "Despesa removida: " + expenseDesc,
                expenseId
        );
    }

    /**
     * Registra o pagamento de uma despesa com a data atual.
     * Idempotente — se já estiver paga, apenas atualiza a data para hoje.
     */
    @Transactional
    public ExpenseResponse markAsPaid(UUID accountId, UUID farmId, UUID userId, UUID expenseId) {
        findFarmAndValidate(accountId, farmId, userId);
        Expense expense = findExpense(expenseId, farmId);
        expense.setPaymentDate(LocalDate.now());
        ExpenseResponse paid = ExpenseResponse.from(expenseRepository.save(expense));
        activityService.record(
                farmId, userId,
                FarmActivityType.EXPENSE_PAID,
                "Despesa marcada como paga: " + expense.getDescription(),
                paid.id()
        );
        return paid;
    }

    // ── Utilitários privados ──────────────────────────────────────────────────

    private Farm findFarmAndValidate(UUID accountId, UUID farmId, UUID userId) {
        // Valida existência da conta
        accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Conta", "id", accountId));

        // Valida membership do usuário autenticado
        if (!accountMemberRepository.existsByAccountIdAndUserId(accountId, userId)) {
            throw new BusinessException("Acesso negado a esta conta", HttpStatus.FORBIDDEN);
        }

        // Valida que a lavoura pertence à conta
        return farmRepository.findByIdAndAccountId(farmId, accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Lavoura", "id", farmId));
    }

    private Expense findExpense(UUID expenseId, UUID farmId) {
        return expenseRepository.findByIdAndFarmId(expenseId, farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Despesa", "id", expenseId));
    }
}