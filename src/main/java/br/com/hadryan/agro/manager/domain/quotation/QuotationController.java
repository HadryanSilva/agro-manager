package br.com.hadryan.agro.manager.domain.quotation;

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
 * Endpoints de gerenciamento de cotações de insumos.
 */
@RestController
@RequestMapping("/accounts/{accountId}/quotations")
@RequiredArgsConstructor
public class QuotationController {

    private final QuotationService quotationService;

    @PostMapping
    public ResponseEntity<ApiResponse<QuotationResponse>> create(
            @PathVariable UUID accountId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody QuotationRequest request) {

        QuotationResponse response = quotationService.create(accountId, principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Cotação registrada com sucesso", response));
    }

    /**
     * Lista cotações agrupadas por produto com métricas de economia.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<QuotationGroupResponse>>> listGrouped(
            @PathVariable UUID accountId,
            @AuthenticationPrincipal UserPrincipal principal) {

        List<QuotationGroupResponse> groups = quotationService.listGrouped(accountId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(groups));
    }

    /**
     * Retorna nomes de produtos já cadastrados para autocomplete.
     */
    @GetMapping("/products")
    public ResponseEntity<ApiResponse<List<String>>> getProductSuggestions(
            @PathVariable UUID accountId,
            @AuthenticationPrincipal UserPrincipal principal) {

        List<String> products = quotationService.getProductSuggestions(accountId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    @PutMapping("/{quotationId}")
    public ResponseEntity<ApiResponse<QuotationResponse>> update(
            @PathVariable UUID accountId,
            @PathVariable UUID quotationId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody QuotationRequest request) {

        QuotationResponse response = quotationService.update(accountId, principal.getId(), quotationId, request);
        return ResponseEntity.ok(ApiResponse.success("Cotação atualizada com sucesso", response));
    }

    @DeleteMapping("/{quotationId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable UUID accountId,
            @PathVariable UUID quotationId,
            @AuthenticationPrincipal UserPrincipal principal) {

        quotationService.delete(accountId, principal.getId(), quotationId);
        return ResponseEntity.ok(ApiResponse.success("Cotação removida com sucesso", null));
    }
}