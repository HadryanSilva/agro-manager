package br.com.hadryan.agro.manager.domain.dashboard;

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
 * Endpoint autenticado para obter o resumo agregado da conta.
 * O usuário deve ser membro da conta informada no path.
 */
@RestController
@RequestMapping("/accounts/{accountId}/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * Retorna métricas agregadas da conta: totais por status, áreas,
     * arrendamentos vencendo e lavouras recentes.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<DashboardSummary>> getSummary(
            @PathVariable UUID accountId,
            @AuthenticationPrincipal UserPrincipal principal) {

        DashboardSummary summary = dashboardService.getSummary(accountId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(summary));
    }
}