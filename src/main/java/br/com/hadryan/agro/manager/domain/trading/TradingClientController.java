package br.com.hadryan.agro.manager.domain.trading;

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

@RestController
@RequestMapping("/accounts/{accountId}/trading/clients")
@RequiredArgsConstructor
public class TradingClientController {

    private final TradingClientService clientService;

    @PostMapping
    public ResponseEntity<ApiResponse<TradingClientResponse>> create(
            @PathVariable UUID accountId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody TradingClientRequest request) {
        TradingClientResponse response = clientService.create(accountId, principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<TradingClientResponse>>> list(
            @PathVariable UUID accountId,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String search) {
        List<TradingClientResponse> clients = (search != null && !search.isBlank())
                ? clientService.search(accountId, principal.getId(), search)
                : clientService.listAll(accountId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(clients));
    }

    @GetMapping("/{clientId}")
    public ResponseEntity<ApiResponse<TradingClientResponse>> getById(
            @PathVariable UUID accountId,
            @PathVariable UUID clientId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                clientService.findById(accountId, principal.getId(), clientId)));
    }

    @PutMapping("/{clientId}")
    public ResponseEntity<ApiResponse<TradingClientResponse>> update(
            @PathVariable UUID accountId,
            @PathVariable UUID clientId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody TradingClientRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                clientService.update(accountId, principal.getId(), clientId, request)));
    }

    @DeleteMapping("/{clientId}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID accountId,
            @PathVariable UUID clientId,
            @AuthenticationPrincipal UserPrincipal principal) {
        clientService.delete(accountId, principal.getId(), clientId);
        return ResponseEntity.noContent().build();
    }
}
