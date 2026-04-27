package br.com.hadryan.agro.manager.domain.expense;

import br.com.hadryan.agro.manager.infra.security.UserPrincipal;
import br.com.hadryan.agro.manager.shared.dto.ApiResponse;
import br.com.hadryan.agro.manager.shared.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Endpoint para listagem consolidada de transações (despesas) de uma conta.
 * Todos os filtros são opcionais via query params.
 */
@RestController
@RequestMapping("/accounts/{accountId}/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    /**
     * Lista transações paginadas com filtros opcionais.
     *
     * @param farmId    filtra por lavoura específica
     * @param category  INSUMO | SERVICO
     * @param paid      true = apenas pagas | false = apenas pendentes
     * @param startDate data de competência inicial (inclusive)
     * @param endDate   data de competência final (inclusive)
     * @param page      número da página (0-based)
     * @param size      itens por página (padrão 20, máximo 100)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<TransactionResponse>>> getTransactions(
            @PathVariable UUID accountId,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) UUID farmId,
            @RequestParam(required = false) ExpenseCategory category,
            @RequestParam(required = false) Boolean paid,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        // Limita o tamanho máximo de página para evitar sobrecarga
        int safeSize = Math.min(size, 100);

        PageResponse<TransactionResponse> result = transactionService.getTransactions(
                accountId, principal.getId(),
                farmId, category, paid, startDate, endDate,
                page, safeSize
        );

        return ResponseEntity.ok(ApiResponse.success(result));
    }
}