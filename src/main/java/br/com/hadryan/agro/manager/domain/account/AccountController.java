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
 * Endpoints de gerenciamento de contas do usuário autenticado.
 */
@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    public ResponseEntity<ApiResponse<AccountResponse>> createAccount(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateAccountRequest request) {

        AccountResponse response = accountService.createAccount(principal.getId(), request);

        // Header Location aponta para o recurso criado — padrão REST para 201 Created
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity.created(location)
                .body(ApiResponse.success("Conta criada com sucesso", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AccountResponse>>> getUserAccounts(
            @AuthenticationPrincipal UserPrincipal principal) {

        List<AccountResponse> accounts = accountService.getUserAccounts(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(accounts));
    }

    /**
     * Exclui permanentemente a conta e todos os seus dados.
     * Restrição: apenas o OWNER pode executar esta operação.
     * O body deve conter o nome exato da conta como confirmação.
     * Retorna 204 No Content — sem body, conforme semântica REST para DELETE.
     */
    @DeleteMapping("/{accountId}")
    public ResponseEntity<Void> deleteAccount(
            @PathVariable UUID accountId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody DeleteAccountRequest request) {

        accountService.deleteAccount(accountId, principal.getId(), request);
        return ResponseEntity.noContent().build();
    }
}