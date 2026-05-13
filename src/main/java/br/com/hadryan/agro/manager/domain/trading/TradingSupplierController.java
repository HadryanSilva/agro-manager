package br.com.hadryan.agro.manager.domain.trading;

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

/**
 * Endpoints para gerenciamento de fornecedores do modo comprador.
 * Todas as operações requerem que o usuário seja membro da conta.
 */
@RestController
@RequestMapping("/accounts/{accountId}/trading/suppliers")
@RequiredArgsConstructor
public class TradingSupplierController {

    private final TradingSupplierService supplierService;

    @PostMapping
    public ResponseEntity<ApiResponse<TradingSupplierResponse>> create(
            @PathVariable UUID accountId,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody @Valid TradingSupplierRequest request) {

        TradingSupplierResponse response = supplierService.create(accountId, principal.getId(), request);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity.created(location).body(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<TradingSupplierResponse>>> listAll(
            @PathVariable UUID accountId,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String search) {

        List<TradingSupplierResponse> suppliers = (search != null && !search.isBlank())
                ? supplierService.search(accountId, principal.getId(), search)
                : supplierService.listAll(accountId, principal.getId());

        return ResponseEntity.ok(ApiResponse.success(suppliers));
    }

    @GetMapping("/{supplierId}")
    public ResponseEntity<ApiResponse<TradingSupplierResponse>> findById(
            @PathVariable UUID accountId,
            @PathVariable UUID supplierId,
            @AuthenticationPrincipal UserPrincipal principal) {

        return ResponseEntity.ok(ApiResponse.success(
                supplierService.findById(accountId, principal.getId(), supplierId)));
    }

    @PutMapping("/{supplierId}")
    public ResponseEntity<ApiResponse<TradingSupplierResponse>> update(
            @PathVariable UUID accountId,
            @PathVariable UUID supplierId,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody @Valid TradingSupplierRequest request) {

        return ResponseEntity.ok(ApiResponse.success(
                supplierService.update(accountId, principal.getId(), supplierId, request)));
    }

    @DeleteMapping("/{supplierId}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID accountId,
            @PathVariable UUID supplierId,
            @AuthenticationPrincipal UserPrincipal principal) {

        supplierService.delete(accountId, principal.getId(), supplierId);
        return ResponseEntity.noContent().build();
    }
}
