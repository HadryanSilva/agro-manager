package br.com.hadryan.agro.manager.domain.expense;

import java.util.List;
import java.util.UUID;

/** Contrato do serviço de gerenciamento de despesas (lavoura e gerais). */
public interface ExpenseService {

    ExpenseResponse create(UUID accountId, UUID farmId, UUID userId, ExpenseRequest request);
    List<ExpenseResponse> findAll(UUID accountId, UUID farmId, UUID userId);
    ExpenseResponse findById(UUID accountId, UUID farmId, UUID userId, UUID expenseId);
    ExpenseResponse update(UUID accountId, UUID farmId, UUID userId, UUID expenseId, ExpenseRequest request);
    void delete(UUID accountId, UUID farmId, UUID userId, UUID expenseId);
    ExpenseResponse markAsPaid(UUID accountId, UUID farmId, UUID userId, UUID expenseId);

    ExpenseResponse createGeneral(UUID accountId, UUID userId, ExpenseRequest request);
    ExpenseResponse findGeneralById(UUID accountId, UUID userId, UUID expenseId);
    ExpenseResponse updateGeneral(UUID accountId, UUID userId, UUID expenseId, ExpenseRequest request);
    void deleteGeneral(UUID accountId, UUID userId, UUID expenseId);
    ExpenseResponse markGeneralAsPaid(UUID accountId, UUID userId, UUID expenseId);
}