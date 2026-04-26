package br.com.hadryan.agro.manager.domain.farm;

import br.com.hadryan.agro.manager.infra.security.UserPrincipal;
import br.com.hadryan.agro.manager.shared.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Endpoint para geração do relatório financeiro de uma lavoura.
 * Requer membership do usuário na conta.
 */
@RestController
@RequestMapping("/accounts/{accountId}/farms/{farmId}/report")
@RequiredArgsConstructor
public class FarmReportController {

    private final FarmReportService farmReportService;

    @GetMapping
    public ResponseEntity<ApiResponse<FarmReportResponse>> getReport(
            @PathVariable UUID accountId,
            @PathVariable UUID farmId,
            @AuthenticationPrincipal UserPrincipal principal) {

        FarmReportResponse report = farmReportService.getReport(accountId, farmId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(report));
    }
}