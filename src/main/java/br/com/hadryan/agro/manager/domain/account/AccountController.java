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

/**
 * Endpoints autenticados para gerenciamento de contas.
 * Todos os endpoints exigem um JWT válido no header Authorization.
 */
@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    /**
     * Cria uma nova conta e automaticamente torna o usuário autenticado seu OWNER.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<AccountResponse>> createAccount(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateAccountRequest request) {

        AccountResponse response = accountService.createAccount(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Conta criada com sucesso", response));
    }

    /**
     * Retorna todas as contas das quais o usuário autenticado é membro.
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<AccountResponse>>> getUserAccounts(
            @AuthenticationPrincipal UserPrincipal principal) {

        List<AccountResponse> accounts = accountService.getUserAccounts(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(accounts));
    }
}