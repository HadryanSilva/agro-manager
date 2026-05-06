package br.com.hadryan.agro.manager.domain.account;

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
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Conta criada com sucesso", response));
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
     */
    @DeleteMapping("/{accountId}")
    public ResponseEntity<ApiResponse<Void>> deleteAccount(
            @PathVariable UUID accountId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody DeleteAccountRequest request) {

        accountService.deleteAccount(accountId, principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Conta excluída permanentemente", null));
    }
}