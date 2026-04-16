package br.com.hadryan.agro.manager.account.controller;

import br.com.hadryan.agro.manager.account.domain.Account;
import br.com.hadryan.agro.manager.account.dto.AccountResponse;
import br.com.hadryan.agro.manager.account.dto.CreateAccountRequest;
import br.com.hadryan.agro.manager.account.dto.UpdateAccountRequest;
import br.com.hadryan.agro.manager.account.service.AccountService;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoints for {@link Account}.
 *
 * <p>Scope of this task: open endpoints without authentication. A later task
 * adds Spring Security so that:
 * <ul>
 *   <li>Account creation is part of the registration flow ({@code
 *       /auth/register}) rather than a public POST.</li>
 *   <li>GET/PUT require the caller to belong to the target account (enforced
 *       transparently by the Hibernate tenant filter).</li>
 *   <li>Suspend/activate become admin-only.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService service;

    @PostMapping
    public ResponseEntity<AccountResponse> create(@Valid @RequestBody CreateAccountRequest request) {
        Account account = service.create(request.name());
        AccountResponse body = AccountResponse.from(account);
        return ResponseEntity
                .created(URI.create("/api/accounts/" + account.getId()))
                .body(body);
    }

    @GetMapping("/{id}")
    public AccountResponse get(@PathVariable Long id) {
        return AccountResponse.from(service.findById(id));
    }

    @PutMapping("/{id}")
    public AccountResponse update(@PathVariable Long id,
                                  @Valid @RequestBody UpdateAccountRequest request) {
        return AccountResponse.from(service.rename(id, request.name()));
    }

    @PostMapping("/{id}/suspend")
    public AccountResponse suspend(@PathVariable Long id) {
        return AccountResponse.from(service.suspend(id));
    }

    @PostMapping("/{id}/activate")
    public AccountResponse activate(@PathVariable Long id) {
        return AccountResponse.from(service.activate(id));
    }
}