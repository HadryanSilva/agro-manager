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

@RestController
@RequestMapping("/accounts/{accountId}/trading/orders")
@RequiredArgsConstructor
public class CustomerOrderController {

    private final CustomerOrderService orderService;

    @PostMapping
    public ResponseEntity<ApiResponse<CustomerOrderResponse>> create(
            @PathVariable UUID accountId,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody @Valid CustomerOrderRequest request) {

        CustomerOrderResponse response = orderService.create(accountId, principal.getId(), request);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity.created(location).body(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CustomerOrderResponse>>> listAll(
            @PathVariable UUID accountId,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) CustomerOrderStatus status) {

        return ResponseEntity.ok(ApiResponse.success(
                orderService.listAll(accountId, principal.getId(), status)));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<CustomerOrderResponse>> findById(
            @PathVariable UUID accountId,
            @PathVariable UUID orderId,
            @AuthenticationPrincipal UserPrincipal principal) {

        return ResponseEntity.ok(ApiResponse.success(
                orderService.findById(accountId, principal.getId(), orderId)));
    }

    @PutMapping("/{orderId}")
    public ResponseEntity<ApiResponse<CustomerOrderResponse>> update(
            @PathVariable UUID accountId,
            @PathVariable UUID orderId,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody @Valid CustomerOrderRequest request) {

        return ResponseEntity.ok(ApiResponse.success(
                orderService.update(accountId, principal.getId(), orderId, request)));
    }

    @DeleteMapping("/{orderId}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID accountId,
            @PathVariable UUID orderId,
            @AuthenticationPrincipal UserPrincipal principal) {

        orderService.delete(accountId, principal.getId(), orderId);
        return ResponseEntity.noContent().build();
    }
}
