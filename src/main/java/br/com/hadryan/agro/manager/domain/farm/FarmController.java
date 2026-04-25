package br.com.hadryan.agro.manager.domain.farm;

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
 * Endpoints para gerenciamento de lavouras de uma conta.
 * Todas as operações verificam membership do usuário autenticado na conta.
 */
@RestController
@RequestMapping("/accounts/{accountId}/farms")
@RequiredArgsConstructor
public class FarmController {

    private final FarmService farmService;

    @PostMapping
    public ResponseEntity<ApiResponse<FarmResponse>> create(
            @PathVariable UUID accountId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody FarmRequest request) {

        FarmResponse response = farmService.create(accountId, principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Lavoura criada com sucesso", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<FarmResponse>>> findAll(
            @PathVariable UUID accountId,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) FarmStatus status) {

        List<FarmResponse> farms = farmService.findAll(accountId, principal.getId(), status);
        return ResponseEntity.ok(ApiResponse.success(farms));
    }

    @GetMapping("/{farmId}")
    public ResponseEntity<ApiResponse<FarmResponse>> findById(
            @PathVariable UUID accountId,
            @PathVariable UUID farmId,
            @AuthenticationPrincipal UserPrincipal principal) {

        FarmResponse farm = farmService.findById(accountId, principal.getId(), farmId);
        return ResponseEntity.ok(ApiResponse.success(farm));
    }

    @PutMapping("/{farmId}")
    public ResponseEntity<ApiResponse<FarmResponse>> update(
            @PathVariable UUID accountId,
            @PathVariable UUID farmId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody FarmRequest request) {

        FarmResponse response = farmService.update(accountId, principal.getId(), farmId, request);
        return ResponseEntity.ok(ApiResponse.success("Lavoura atualizada com sucesso", response));
    }

    @DeleteMapping("/{farmId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable UUID accountId,
            @PathVariable UUID farmId,
            @AuthenticationPrincipal UserPrincipal principal) {

        farmService.delete(accountId, principal.getId(), farmId);
        return ResponseEntity.ok(ApiResponse.success("Lavoura removida com sucesso", null));
    }
}