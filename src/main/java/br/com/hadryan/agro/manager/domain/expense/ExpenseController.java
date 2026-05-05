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
 * Endpoints para gerenciamento de despesas.
 *
 * /accounts/{id}/farms/{id}/expenses  — despesas vinculadas a uma lavoura
 * /accounts/{id}/expenses             — despesas gerais da conta (sem lavoura)
 */
@RestController
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;

    // ── Despesas de lavoura ───────────────────────────────────────────────────

    @PostMapping("/accounts/{accountId}/farms/{farmId}/expenses")
    public ResponseEntity<ApiResponse<ExpenseResponse>> create(
            @PathVariable UUID accountId,
            @PathVariable UUID farmId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ExpenseRequest request) {

        ExpenseResponse response = expenseService.create(accountId, farmId, principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Despesa registrada com sucesso", response));
    }

    @GetMapping("/accounts/{accountId}/farms/{farmId}/expenses")
    public ResponseEntity<ApiResponse<List<ExpenseResponse>>> findAll(
            @PathVariable UUID accountId,
            @PathVariable UUID farmId,
            @AuthenticationPrincipal UserPrincipal principal) {

        List<ExpenseResponse> expenses = expenseService.findAll(accountId, farmId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(expenses));
    }

    @GetMapping("/accounts/{accountId}/farms/{farmId}/expenses/{expenseId}")
    public ResponseEntity<ApiResponse<ExpenseResponse>> findById(
            @PathVariable UUID accountId,
            @PathVariable UUID farmId,
            @PathVariable UUID expenseId,
            @AuthenticationPrincipal UserPrincipal principal) {

        ExpenseResponse expense = expenseService.findById(accountId, farmId, principal.getId(), expenseId);
        return ResponseEntity.ok(ApiResponse.success(expense));
    }

    @PutMapping("/accounts/{accountId}/farms/{farmId}/expenses/{expenseId}")
    public ResponseEntity<ApiResponse<ExpenseResponse>> update(
            @PathVariable UUID accountId,
            @PathVariable UUID farmId,
            @PathVariable UUID expenseId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ExpenseRequest request) {

        ExpenseResponse response = expenseService.update(accountId, farmId, principal.getId(), expenseId, request);
        return ResponseEntity.ok(ApiResponse.success("Despesa atualizada com sucesso", response));
    }

    @DeleteMapping("/accounts/{accountId}/farms/{farmId}/expenses/{expenseId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable UUID accountId,
            @PathVariable UUID farmId,
            @PathVariable UUID expenseId,
            @AuthenticationPrincipal UserPrincipal principal) {

        expenseService.delete(accountId, farmId, principal.getId(), expenseId);
        return ResponseEntity.ok(ApiResponse.success("Despesa removida com sucesso", null));
    }

    @PatchMapping("/accounts/{accountId}/farms/{farmId}/expenses/{expenseId}/pay")
    public ResponseEntity<ApiResponse<ExpenseResponse>> markAsPaid(
            @PathVariable UUID accountId,
            @PathVariable UUID farmId,
            @PathVariable UUID expenseId,
            @AuthenticationPrincipal UserPrincipal principal) {

        ExpenseResponse response = expenseService.markAsPaid(accountId, farmId, principal.getId(), expenseId);
        return ResponseEntity.ok(ApiResponse.success("Despesa marcada como paga", response));
    }

    // ── Despesas gerais da conta (sem lavoura) ────────────────────────────────

    @PostMapping("/accounts/{accountId}/expenses")
    public ResponseEntity<ApiResponse<ExpenseResponse>> createGeneral(
            @PathVariable UUID accountId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ExpenseRequest request) {

        ExpenseResponse response = expenseService.createGeneral(accountId, principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Despesa registrada com sucesso", response));
    }

    @GetMapping("/accounts/{accountId}/expenses/{expenseId}")
    public ResponseEntity<ApiResponse<ExpenseResponse>> findGeneralById(
            @PathVariable UUID accountId,
            @PathVariable UUID expenseId,
            @AuthenticationPrincipal UserPrincipal principal) {

        ExpenseResponse expense = expenseService.findGeneralById(accountId, principal.getId(), expenseId);
        return ResponseEntity.ok(ApiResponse.success(expense));
    }

    @PutMapping("/accounts/{accountId}/expenses/{expenseId}")
    public ResponseEntity<ApiResponse<ExpenseResponse>> updateGeneral(
            @PathVariable UUID accountId,
            @PathVariable UUID expenseId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ExpenseRequest request) {

        ExpenseResponse response = expenseService.updateGeneral(accountId, principal.getId(), expenseId, request);
        return ResponseEntity.ok(ApiResponse.success("Despesa atualizada com sucesso", response));
    }

    @DeleteMapping("/accounts/{accountId}/expenses/{expenseId}")
    public ResponseEntity<ApiResponse<Void>> deleteGeneral(
            @PathVariable UUID accountId,
            @PathVariable UUID expenseId,
            @AuthenticationPrincipal UserPrincipal principal) {

        expenseService.deleteGeneral(accountId, principal.getId(), expenseId);
        return ResponseEntity.ok(ApiResponse.success("Despesa removida com sucesso", null));
    }

    @PatchMapping("/accounts/{accountId}/expenses/{expenseId}/pay")
    public ResponseEntity<ApiResponse<ExpenseResponse>> markGeneralAsPaid(
            @PathVariable UUID accountId,
            @PathVariable UUID expenseId,
            @AuthenticationPrincipal UserPrincipal principal) {

        ExpenseResponse response = expenseService.markGeneralAsPaid(accountId, principal.getId(), expenseId);
        return ResponseEntity.ok(ApiResponse.success("Despesa marcada como paga", response));
    }
}