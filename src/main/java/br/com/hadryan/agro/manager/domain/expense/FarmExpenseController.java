package br.com.hadryan.agro.manager.domain.expense;

import br.com.hadryan.agro.manager.infra.security.UserPrincipal;
import br.com.hadryan.agro.manager.shared.dto.ApiResponse;
import br.com.hadryan.agro.manager.shared.dto.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

/**
 * Endpoints para gerenciamento de despesas vinculadas a uma lavoura específica.
 * Todas as operações requerem que o usuário seja membro da conta.
 */
@RestController
@RequestMapping("/accounts/{accountId}/farms/{farmId}/expenses")
@RequiredArgsConstructor
public class FarmExpenseController {

    private final ExpenseService expenseService;

    @PostMapping
    public ResponseEntity<ApiResponse<ExpenseResponse>> create(
            @PathVariable UUID accountId,
            @PathVariable UUID farmId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ExpenseRequest request) {

        ExpenseResponse response = expenseService.create(accountId, farmId, principal.getId(), request);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity.created(location)
                .body(ApiResponse.success("Despesa registrada com sucesso", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ExpenseResponse>>> findAll(
            @PathVariable UUID accountId,
            @PathVariable UUID farmId,
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20, sort = "competenceDate", direction = Sort.Direction.DESC) Pageable pageable) {

        PageResponse<ExpenseResponse> expenses = expenseService.findAll(accountId, farmId, principal.getId(), pageable);
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

    /** Retorna 204 No Content — sem body, conforme semântica REST para DELETE. */
    @DeleteMapping("/{expenseId}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID accountId,
            @PathVariable UUID farmId,
            @PathVariable UUID expenseId,
            @AuthenticationPrincipal UserPrincipal principal) {

        expenseService.delete(accountId, farmId, principal.getId(), expenseId);
        return ResponseEntity.noContent().build();
    }

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