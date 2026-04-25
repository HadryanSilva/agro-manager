package br.com.hadryan.agro.manager.domain.account;

import br.com.hadryan.agro.manager.infra.security.UserPrincipal;
import br.com.hadryan.agro.manager.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Endpoints de gerenciamento de membros de uma conta.
 * Listar: qualquer membro. Alterar papel / Remover: OWNER e ADMIN.
 */
@RestController
@RequestMapping("/accounts/{accountId}/members")
@RequiredArgsConstructor
public class AccountMemberController {

    private final AccountMemberService memberService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<AccountMemberResponse>>> listMembers(
            @PathVariable UUID accountId,
            @AuthenticationPrincipal UserPrincipal principal) {

        List<AccountMemberResponse> members = memberService.listMembers(accountId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(members));
    }

    @PutMapping("/{memberId}/role")
    public ResponseEntity<ApiResponse<AccountMemberResponse>> updateRole(
            @PathVariable UUID accountId,
            @PathVariable UUID memberId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody AccountMemberRoleRequest request) {

        AccountMemberResponse response = memberService.updateRole(accountId, principal.getId(), memberId, request);
        return ResponseEntity.ok(ApiResponse.success("Papel atualizado com sucesso", response));
    }

    @DeleteMapping("/{memberId}")
    public ResponseEntity<ApiResponse<Void>> removeMember(
            @PathVariable UUID accountId,
            @PathVariable UUID memberId,
            @AuthenticationPrincipal UserPrincipal principal) {

        memberService.removeMember(accountId, principal.getId(), memberId);
        return ResponseEntity.ok(ApiResponse.success("Membro removido com sucesso", null));
    }
}