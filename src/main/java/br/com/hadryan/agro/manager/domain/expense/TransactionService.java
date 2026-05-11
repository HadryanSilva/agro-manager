package br.com.hadryan.agro.manager.domain.expense;

import br.com.hadryan.agro.manager.shared.dto.PageResponse;

import java.time.LocalDate;
import java.util.UUID;

/** Contrato do serviço de listagem consolidada de transações. */
public interface TransactionService {

    PageResponse<TransactionResponse> getTransactions(
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
    );
}