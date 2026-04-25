package br.com.hadryan.agro.manager.domain.user;

import br.com.hadryan.agro.manager.infra.security.UserPrincipal;
import br.com.hadryan.agro.manager.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoints autenticados para leitura e atualização do perfil do usuário logado.
 */
@RestController
@RequestMapping("/users/me")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;

    @GetMapping
    public ResponseEntity<ApiResponse<UserProfileResponse>> getProfile(
            @AuthenticationPrincipal UserPrincipal principal) {

        UserProfileResponse profile = userProfileService.getProfile(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateProfileRequest request) {

        UserProfileResponse profile = userProfileService.updateProfile(principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Perfil atualizado com sucesso", profile));
    }

    /**
     * Altera a senha — disponível apenas para contas com authProvider LOCAL.
     * Para contas OAuth2, o serviço lança BusinessException com 400.
     */
    @PutMapping("/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ChangePasswordRequest request) {

        userProfileService.changePassword(principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Senha alterada com sucesso", null));
    }
}