package br.com.hadryan.agro.manager.user.controller;

import br.com.hadryan.agro.manager.user.domain.User;
import br.com.hadryan.agro.manager.user.dto.ChangePasswordRequest;
import br.com.hadryan.agro.manager.user.dto.ChangeRoleRequest;
import br.com.hadryan.agro.manager.user.dto.CreateUserRequest;
import br.com.hadryan.agro.manager.user.dto.UpdateUserRequest;
import br.com.hadryan.agro.manager.user.dto.UserResponse;
import br.com.hadryan.agro.manager.user.service.UserService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST surface for {@link User}.
 *
 * <p>Access control is not yet wired — every endpoint here is open. The
 * Security task will restrict who can call what (e.g. only OWNERs can
 * change roles; users can always change their own profile and password).
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService service;

    @PostMapping
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        User created = service.createWithPassword(
                request.accountId(),
                request.email(),
                request.name(),
                request.password(),
                request.role()
        );
        return ResponseEntity
                .created(URI.create("/api/users/" + created.getId()))
                .body(UserResponse.from(created));
    }

    @GetMapping("/{id}")
    public UserResponse get(@PathVariable Long id) {
        return UserResponse.from(service.findById(id));
    }

    @GetMapping
    public List<UserResponse> list(@RequestParam Long accountId) {
        return service.findByAccount(accountId).stream()
                .map(UserResponse::from)
                .toList();
    }

    @PutMapping("/{id}")
    public UserResponse update(@PathVariable Long id,
                               @Valid @RequestBody UpdateUserRequest request) {
        return UserResponse.from(service.rename(id, request.name()));
    }

    @PostMapping("/{id}/password")
    public UserResponse changePassword(@PathVariable Long id,
                                       @Valid @RequestBody ChangePasswordRequest request) {
        return UserResponse.from(service.changePassword(id, request.newPassword()));
    }

    @PostMapping("/{id}/role")
    public UserResponse changeRole(@PathVariable Long id,
                                   @Valid @RequestBody ChangeRoleRequest request) {
        return UserResponse.from(service.changeRole(id, request.role()));
    }

    @PostMapping("/{id}/deactivate")
    public UserResponse deactivate(@PathVariable Long id) {
        return UserResponse.from(service.deactivate(id));
    }

    @PostMapping("/{id}/activate")
    public UserResponse activate(@PathVariable Long id) {
        return UserResponse.from(service.activate(id));
    }
}