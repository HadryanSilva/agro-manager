package br.com.hadryan.agro.manager.domain.expense;

import br.com.hadryan.agro.manager.infra.security.UserPrincipal;
import br.com.hadryan.agro.manager.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;

/**
 * Endpoints para gerenciamento de despesas gerais da conta (sem lavoura vinculada).
 */
@RestController
@RequestMapping("/accounts/{accountId}/expenses")
@RequiredArgsConstructor
public class GeneralExpenseController {

    private final ExpenseService expenseService;

    @Value("${app.notifications.default-days-ahead:7}")
    private int defaultDaysAhead;

    @PostMapping
    public ResponseEntity<ApiResponse<ExpenseResponse>> createGeneral(
            @PathVariable UUID accountId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ExpenseRequest request) {

        ExpenseResponse response = expenseService.createGeneral(accountId, principal.getId(), request);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity.created(location)
                .body(ApiResponse.success("Despesa registrada com sucesso", response));
    }

    @GetMapping("/{expenseId}")
    public ResponseEntity<ApiResponse<ExpenseResponse>> findGeneralById(
            @PathVariable UUID accountId,
            @PathVariable UUID expenseId,
            @AuthenticationPrincipal UserPrincipal principal) {

        ExpenseResponse expense = expenseService.findGeneralById(accountId, principal.getId(), expenseId);
        return ResponseEntity.ok(ApiResponse.success(expense));
    }

    @PutMapping("/{expenseId}")
    public ResponseEntity<ApiResponse<ExpenseResponse>> updateGeneral(
            @PathVariable UUID accountId,
            @PathVariable UUID expenseId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ExpenseRequest request) {

        ExpenseResponse response = expenseService.updateGeneral(accountId, principal.getId(), expenseId, request);
        return ResponseEntity.ok(ApiResponse.success("Despesa atualizada com sucesso", response));
    }

    /** Retorna 204 No Content — sem body, conforme semântica REST para DELETE. */
    @DeleteMapping("/{expenseId}")
    public ResponseEntity<Void> deleteGeneral(
            @PathVariable UUID accountId,
            @PathVariable UUID expenseId,
            @AuthenticationPrincipal UserPrincipal principal) {

        expenseService.deleteGeneral(accountId, principal.getId(), expenseId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{expenseId}/pay")
    public ResponseEntity<ApiResponse<ExpenseResponse>> markGeneralAsPaid(
            @PathVariable UUID accountId,
            @PathVariable UUID expenseId,
            @AuthenticationPrincipal UserPrincipal principal) {

        ExpenseResponse response = expenseService.markGeneralAsPaid(accountId, principal.getId(), expenseId);
        return ResponseEntity.ok(ApiResponse.success("Despesa marcada como paga", response));
    }

    @GetMapping("/upcoming")
    public ResponseEntity<ApiResponse<List<ExpenseResponse>>> upcoming(
            @PathVariable UUID accountId,
            @RequestParam(required = false) Integer days,
            @AuthenticationPrincipal UserPrincipal principal) {
        int d = (days != null) ? days : defaultDaysAhead;
        List<ExpenseResponse> result = expenseService.findUpcoming(accountId, principal.getId(), d);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}