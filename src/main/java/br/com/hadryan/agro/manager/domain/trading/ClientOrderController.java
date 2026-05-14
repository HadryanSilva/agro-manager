package br.com.hadryan.agro.manager.domain.trading;

import br.com.hadryan.agro.manager.infra.security.UserPrincipal;
import br.com.hadryan.agro.manager.shared.dto.ApiResponse;
import br.com.hadryan.agro.manager.shared.dto.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/accounts/{accountId}/trading")
@RequiredArgsConstructor
public class ClientOrderController {

    private final ClientOrderService orderService;

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<TradingDashboardResponse>> getDashboard(
            @PathVariable UUID accountId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                orderService.getDashboard(accountId, principal.getId())));
    }

    @PostMapping("/orders")
    public ResponseEntity<ApiResponse<ClientOrderDetailResponse>> create(
            @PathVariable UUID accountId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ClientOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                orderService.createOrder(accountId, principal.getId(), request)));
    }

    @GetMapping("/orders")
    public ResponseEntity<ApiResponse<PageResponse<ClientOrderSummaryResponse>>> list(
            @PathVariable UUID accountId,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) ClientOrderStatus status,
            @RequestParam(required = false) UUID clientId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                orderService.listOrders(accountId, principal.getId(), status, clientId, page, size)));
    }

    @GetMapping("/orders/{orderId}")
    public ResponseEntity<ApiResponse<ClientOrderDetailResponse>> getDetail(
            @PathVariable UUID accountId,
            @PathVariable UUID orderId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                orderService.getOrderDetail(accountId, principal.getId(), orderId)));
    }

    @PutMapping("/orders/{orderId}")
    public ResponseEntity<ApiResponse<ClientOrderDetailResponse>> update(
            @PathVariable UUID accountId,
            @PathVariable UUID orderId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ClientOrderRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                orderService.updateOrder(accountId, principal.getId(), orderId, request)));
    }

    @PatchMapping("/orders/{orderId}/close")
    public ResponseEntity<Void> close(
            @PathVariable UUID accountId,
            @PathVariable UUID orderId,
            @AuthenticationPrincipal UserPrincipal principal) {
        orderService.closeOrder(accountId, principal.getId(), orderId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/orders/{orderId}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID accountId,
            @PathVariable UUID orderId,
            @AuthenticationPrincipal UserPrincipal principal) {
        orderService.deleteOrder(accountId, principal.getId(), orderId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/orders/{orderId}/legs")
    public ResponseEntity<ApiResponse<ClientOrderDetailResponse>> addLeg(
            @PathVariable UUID accountId,
            @PathVariable UUID orderId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody OrderSupplierLegRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                orderService.addLeg(accountId, principal.getId(), orderId, request)));
    }

    @DeleteMapping("/orders/{orderId}/legs/{legId}")
    public ResponseEntity<Void> removeLeg(
            @PathVariable UUID accountId,
            @PathVariable UUID orderId,
            @PathVariable UUID legId,
            @AuthenticationPrincipal UserPrincipal principal) {
        orderService.removeLeg(accountId, principal.getId(), orderId, legId);
        return ResponseEntity.noContent().build();
    }
}
