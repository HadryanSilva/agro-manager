package br.com.hadryan.agro.manager.domain.trading;

import br.com.hadryan.agro.manager.infra.security.UserPrincipal;
import br.com.hadryan.agro.manager.shared.dto.ApiResponse;
import br.com.hadryan.agro.manager.shared.dto.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

/**
 * Endpoints para gerenciamento de lotes de compra e suas vendas.
 * Todas as operações requerem que o usuário seja membro da conta.
 */
@RestController
@RequestMapping("/accounts/{accountId}/trading")
@RequiredArgsConstructor
public class PurchaseLotController {

    private final PurchaseLotService lotService;

    // ── Dashboard ─────────────────────────────────────────────────────────────

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<TradingDashboardResponse>> getDashboard(
            @PathVariable UUID accountId,
            @AuthenticationPrincipal UserPrincipal principal) {

        return ResponseEntity.ok(ApiResponse.success(
                lotService.getDashboard(accountId, principal.getId())));
    }

    // ── Lotes ─────────────────────────────────────────────────────────────────

    @PostMapping("/purchases")
    public ResponseEntity<ApiResponse<PurchaseLotDetailResponse>> createLot(
            @PathVariable UUID accountId,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody @Valid PurchaseLotRequest request) {

        PurchaseLotDetailResponse response = lotService.createLot(accountId, principal.getId(), request);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity.created(location).body(ApiResponse.success(response));
    }

    @GetMapping("/purchases")
    public ResponseEntity<ApiResponse<PageResponse<PurchaseLotSummaryResponse>>> listLots(
            @PathVariable UUID accountId,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) PurchaseLotStatus status,
            @RequestParam(required = false) UUID supplierId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        // Limita o tamanho máximo da página para evitar sobrecarga
        int safeSize = Math.min(size, 100);

        return ResponseEntity.ok(ApiResponse.success(
                lotService.listLots(accountId, principal.getId(), status, supplierId, page, safeSize)));
    }

    @GetMapping("/purchases/{lotId}")
    public ResponseEntity<ApiResponse<PurchaseLotDetailResponse>> getLotDetail(
            @PathVariable UUID accountId,
            @PathVariable UUID lotId,
            @AuthenticationPrincipal UserPrincipal principal) {

        return ResponseEntity.ok(ApiResponse.success(
                lotService.getLotDetail(accountId, principal.getId(), lotId)));
    }

    @PutMapping("/purchases/{lotId}")
    public ResponseEntity<ApiResponse<PurchaseLotDetailResponse>> updateLot(
            @PathVariable UUID accountId,
            @PathVariable UUID lotId,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody @Valid PurchaseLotRequest request) {

        return ResponseEntity.ok(ApiResponse.success(
                lotService.updateLot(accountId, principal.getId(), lotId, request)));
    }

    @PatchMapping("/purchases/{lotId}/close")
    public ResponseEntity<Void> closeLot(
            @PathVariable UUID accountId,
            @PathVariable UUID lotId,
            @AuthenticationPrincipal UserPrincipal principal) {

        lotService.closeLot(accountId, principal.getId(), lotId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/purchases/{lotId}")
    public ResponseEntity<Void> deleteLot(
            @PathVariable UUID accountId,
            @PathVariable UUID lotId,
            @AuthenticationPrincipal UserPrincipal principal) {

        lotService.deleteLot(accountId, principal.getId(), lotId);
        return ResponseEntity.noContent().build();
    }

    // ── Vendas ────────────────────────────────────────────────────────────────

    @PostMapping("/purchases/{lotId}/sales")
    public ResponseEntity<ApiResponse<LotSaleResponse>> createSale(
            @PathVariable UUID accountId,
            @PathVariable UUID lotId,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody @Valid LotSaleRequest request) {

        LotSaleResponse response = lotService.createSale(accountId, principal.getId(), lotId, request);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity.created(location).body(ApiResponse.success(response));
    }

    @DeleteMapping("/purchases/{lotId}/sales/{saleId}")
    public ResponseEntity<Void> deleteSale(
            @PathVariable UUID accountId,
            @PathVariable UUID lotId,
            @PathVariable UUID saleId,
            @AuthenticationPrincipal UserPrincipal principal) {

        lotService.deleteSale(accountId, principal.getId(), lotId, saleId);
        return ResponseEntity.noContent().build();
    }
}
