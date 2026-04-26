package br.com.hadryan.agro.manager.domain.farm;

import br.com.hadryan.agro.manager.domain.account.AccountMemberRepository;
import br.com.hadryan.agro.manager.domain.account.AccountRepository;
import br.com.hadryan.agro.manager.domain.expense.Expense;
import br.com.hadryan.agro.manager.domain.expense.ExpenseCategory;
import br.com.hadryan.agro.manager.domain.expense.ExpenseRepository;
import br.com.hadryan.agro.manager.shared.exception.BusinessException;
import br.com.hadryan.agro.manager.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Serviço que gera o relatório financeiro completo de uma lavoura.
 * Todos os cálculos são realizados em memória — o volume de despesas
 * por lavoura é pequeno e não justifica queries adicionais por agregação.
 */
@Service
@RequiredArgsConstructor
public class FarmReportService {

    private final FarmRepository farmRepository;
    private final ExpenseRepository expenseRepository;
    private final AccountRepository accountRepository;
    private final AccountMemberRepository accountMemberRepository;

    @Transactional(readOnly = true)
    public FarmReportResponse getReport(UUID accountId, UUID farmId, UUID userId) {

        // Valida existência da conta e membership do usuário
        accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Conta", "id", accountId));

        if (!accountMemberRepository.existsByAccountIdAndUserId(accountId, userId)) {
            throw new BusinessException("Acesso negado a esta conta", HttpStatus.FORBIDDEN);
        }

        // Valida que a lavoura pertence à conta
        Farm farm = farmRepository.findByIdAndAccountId(farmId, accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Lavoura", "id", farmId));

        // Carrega todas as despesas da lavoura ordenadas por data de competência
        List<Expense> expenses = expenseRepository.findByFarmIdOrderByCompetenceDateDesc(farmId);

        // ── Totais por categoria ──────────────────────────────────────────────
        BigDecimal totalInsumos  = sumByCategory(expenses, ExpenseCategory.INSUMO);
        BigDecimal totalServicos = sumByCategory(expenses, ExpenseCategory.SERVICO);
        BigDecimal totalExpenses = totalInsumos.add(totalServicos);

        // ── Totais por status de pagamento ────────────────────────────────────
        BigDecimal totalPaid    = expenses.stream()
                .filter(Expense::isPaid)
                .map(Expense::getValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalPending = totalExpenses.subtract(totalPaid);

        // ── Custo total: arrendamento + despesas ──────────────────────────────
        BigDecimal leaseValue = farm.getLeaseValue() != null ? farm.getLeaseValue() : BigDecimal.ZERO;
        BigDecimal totalCost  = leaseValue.add(totalExpenses);

        // ── Evolução mensal (agrupamento por competenceDate) ──────────────────
        Map<YearMonth, List<Expense>> byMonth = expenses.stream()
                .collect(Collectors.groupingBy(e ->
                        YearMonth.of(e.getCompetenceDate().getYear(),
                                e.getCompetenceDate().getMonth())));

        List<FarmReportResponse.MonthlyExpense> monthly = byMonth.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    YearMonth ym  = entry.getKey();
                    List<Expense> mes = entry.getValue();

                    BigDecimal mTotal    = sum(mes);
                    BigDecimal mPaid     = mes.stream().filter(Expense::isPaid).map(Expense::getValue).reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal mInsumos  = sumByCategory(mes, ExpenseCategory.INSUMO);
                    BigDecimal mServicos = sumByCategory(mes, ExpenseCategory.SERVICO);

                    // Formata "Jan/2025" usando locale pt-BR
                    String label = ym.getMonth()
                            .getDisplayName(TextStyle.SHORT, new Locale("pt", "BR"))
                            .replace(".", "")
                            + "/" + ym.getYear();

                    return new FarmReportResponse.MonthlyExpense(
                            ym.getYear(),
                            ym.getMonthValue(),
                            capitalize(label),
                            mTotal,
                            mPaid,
                            mTotal.subtract(mPaid),
                            mInsumos,
                            mServicos
                    );
                })
                .toList();

        // ── Lista de despesas para exportação ─────────────────────────────────
        List<FarmReportResponse.ExpenseItem> expenseItems = expenses.stream()
                .map(e -> new FarmReportResponse.ExpenseItem(
                        e.getId(),
                        e.getDescription(),
                        e.getCategory(),
                        e.getValue(),
                        e.getCompetenceDate(),
                        e.getPaymentDate(),
                        e.isPaid(),
                        e.getNotes()
                ))
                .toList();

        return new FarmReportResponse(
                farm.getId(),
                farm.getName(),
                farm.getAreaValue(),
                farm.getAreaUnit(),
                farm.getStatus(),
                farm.getLessorName(),
                farm.getLeaseStartDate(),
                farm.getLeaseEndDate(),
                farm.getLeaseValue(),
                totalExpenses,
                totalInsumos,
                totalServicos,
                totalPaid,
                totalPending,
                totalCost,
                monthly,
                expenseItems
        );
    }

    // ── Utilitários privados ──────────────────────────────────────────────────

    private BigDecimal sum(List<Expense> list) {
        return list.stream().map(Expense::getValue).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumByCategory(List<Expense> list, ExpenseCategory category) {
        return list.stream()
                .filter(e -> e.getCategory() == category)
                .map(Expense::getValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}