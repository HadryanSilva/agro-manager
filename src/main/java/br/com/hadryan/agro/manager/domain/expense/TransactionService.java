package br.com.hadryan.agro.manager.domain.expense;

import br.com.hadryan.agro.manager.domain.account.AccountMemberRepository;
import br.com.hadryan.agro.manager.domain.account.AccountRepository;
import br.com.hadryan.agro.manager.shared.dto.PageResponse;
import br.com.hadryan.agro.manager.shared.exception.BusinessException;
import br.com.hadryan.agro.manager.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Serviço de listagem consolidada de transações de uma conta.
 * Todos os filtros são opcionais — a ausência retorna todos os registros.
 * O filtro 'general' permite isolar despesas gerais da conta.
 */
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final ExpenseRepository expenseRepository;
    private final AccountRepository accountRepository;
    private final AccountMemberRepository accountMemberRepository;

    @Transactional(readOnly = true)
    public PageResponse<TransactionResponse> getTransactions(
            UUID accountId,
            UUID userId,
            UUID farmId,
            Boolean general,
            ExpenseCategory category,
            Boolean paid,
            LocalDate startDate,
            LocalDate endDate,
            int page,
            int size
    ) {
        accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Conta", "id", accountId));

        if (!accountMemberRepository.existsByAccountIdAndUserId(accountId, userId)) {
            throw new BusinessException("Acesso negado a esta conta", HttpStatus.FORBIDDEN);
        }

        Pageable pageable = PageRequest.of(page, size);

        Page<Expense> resultPage = expenseRepository.findTransactions(
                accountId, farmId, general, category, paid, startDate, endDate, pageable);

        BigDecimal totalFiltered = expenseRepository.sumTransactions(
                accountId, farmId, general, category, paid, startDate, endDate);

        return PageResponse.of(
                resultPage.map(TransactionResponse::from),
                totalFiltered
        );
    }
}