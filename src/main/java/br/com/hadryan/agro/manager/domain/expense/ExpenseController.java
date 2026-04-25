package br.com.hadryan.agro.manager.domain.expense;

import br.com.hadryan.agro.manager.infra.security.UserPrincipal;
import br.com.hadryan.agro.manager.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Endpoints para gerenciamento de despesas de uma lavoura.
 * Todas as operações verificam membership do usuário na conta
 * e que a lavoura pertence à conta informada no path.
 */
@RestController
@RequestMapping("/accounts/{accountId}/farms/{farmId}/expenses")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;

    @PostMapping
    public ResponseEntity<ApiResponse<ExpenseResponse>> create(
            @PathVariable UUID accountId,
            @PathVariable UUID farmId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ExpenseRequest request) {

        ExpenseResponse response = expenseService.create(accountId, farmId, principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Despesa registrada com sucesso", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ExpenseResponse>>> findAll(
            @PathVariable UUID accountId,
            @PathVariable UUID farmId,
            @AuthenticationPrincipal UserPrincipal principal) {

        List<ExpenseResponse> expenses = expenseService.findAll(accountId, farmId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(expenses));
    }

    @GetMapping("/{expenseId}")
    public ResponseEntity<ApiResponse<ExpenseResponse>> findById(
            @PathVariable UUID accountId,
            @PathVariable UUID farmId,
            @PathVariable UUID expenseId,
            @AuthenticationPrincipal UserPrincipal principal) {

        ExpenseResponse expense = expenseService.findById(accountId, farmId, principal.getId(), expenseId);
        return ResponseEntity.ok(ApiResponse.success(expense));
    }

    @PutMapping("/{expenseId}")
    public ResponseEntity<ApiResponse<ExpenseResponse>> update(
            @PathVariable UUID accountId,
            @PathVariable UUID farmId,
            @PathVariable UUID expenseId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ExpenseRequest request) {

        ExpenseResponse response = expenseService.update(accountId, farmId, principal.getId(), expenseId, request);
        return ResponseEntity.ok(ApiResponse.success("Despesa atualizada com sucesso", response));
    }

    @DeleteMapping("/{expenseId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable UUID accountId,
            @PathVariable UUID farmId,
            @PathVariable UUID expenseId,
            @AuthenticationPrincipal UserPrincipal principal) {

        expenseService.delete(accountId, farmId, principal.getId(), expenseId);
        return ResponseEntity.ok(ApiResponse.success("Despesa removida com sucesso", null));
    }

    /**
     * Marca a despesa como paga registrando a data atual como data de pagamento.
     */
    @PatchMapping("/{expenseId}/pay")
    public ResponseEntity<ApiResponse<ExpenseResponse>> markAsPaid(
            @PathVariable UUID accountId,
            @PathVariable UUID farmId,
            @PathVariable UUID expenseId,
            @AuthenticationPrincipal UserPrincipal principal) {

        ExpenseResponse response = expenseService.markAsPaid(accountId, farmId, principal.getId(), expenseId);
        return ResponseEntity.ok(ApiResponse.success("Despesa marcada como paga", response));
    }
}