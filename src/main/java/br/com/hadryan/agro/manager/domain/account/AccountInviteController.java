package br.com.hadryan.agro.manager.domain.account;

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
 * Endpoints de gerenciamento de convites nominais por e-mail.
 *
 * /accounts/{id}/invites — criação, listagem e revogação (OWNER/ADMIN)
 * /invites/{token}       — detalhes públicos (GET) e aceite autenticado (POST)
 */
@RestController
@RequiredArgsConstructor
public class AccountInviteController {

    private final AccountInviteService inviteService;

    // ── Gestão de convites (requer membership na conta) ───────────────────────

    @PostMapping("/accounts/{accountId}/invites")
    public ResponseEntity<ApiResponse<AccountInviteResponse>> createInvite(
            @PathVariable UUID accountId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody AccountInviteRequest request) {

        AccountInviteResponse response = inviteService.createInvite(
                accountId, principal.getId(), request);

        URI location = ServletUriComponentsBuilder
                .fromCurrentContextPath()
                .path("/accounts/{accountId}/invites/{id}")
                .buildAndExpand(accountId, response.id())
                .toUri();

        return ResponseEntity.created(location)
                .body(ApiResponse.success(
                        "Convite enviado com sucesso para " + request.email(), response));
    }

    @GetMapping("/accounts/{accountId}/invites")
    public ResponseEntity<ApiResponse<List<AccountInviteResponse>>> listInvites(
            @PathVariable UUID accountId,
            @AuthenticationPrincipal UserPrincipal principal) {

        List<AccountInviteResponse> invites = inviteService.listActiveInvites(accountId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(invites));
    }

    /** Retorna 204 No Content — sem body, conforme semântica REST para DELETE. */
    @DeleteMapping("/accounts/{accountId}/invites/{inviteId}")
    public ResponseEntity<Void> revokeInvite(
            @PathVariable UUID accountId,
            @PathVariable UUID inviteId,
            @AuthenticationPrincipal UserPrincipal principal) {

        inviteService.revokeInvite(accountId, principal.getId(), inviteId);
        return ResponseEntity.noContent().build();
    }

    // ── Fluxo público de convite ──────────────────────────────────────────────

    /**
     * Retorna detalhes do convite pelo token.
     * Endpoint liberado no SecurityConfig (GET /invites/**) para
     * que o frontend exiba informações antes do login.
     */
    @GetMapping("/invites/{token}")
    public ResponseEntity<ApiResponse<AccountInviteResponse>> getInviteDetails(
            @PathVariable UUID token) {

        AccountInviteResponse response = inviteService.getInviteDetails(token);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Aceita o convite e adiciona o usuário autenticado como membro da conta.
     * Valida que o e-mail do usuário autenticado coincide com o e-mail do convite.
     */
    @PostMapping("/invites/{token}/accept")
    public ResponseEntity<ApiResponse<AccountMemberResponse>> acceptInvite(
            @PathVariable UUID token,
            @AuthenticationPrincipal UserPrincipal principal) {

        AccountMemberResponse response = inviteService.acceptInvite(token, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Convite aceito com sucesso", response));
    }
}