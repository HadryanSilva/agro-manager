package br.com.hadryan.agro.manager.domain.user;

import br.com.hadryan.agro.manager.infra.security.UserPrincipal;
import br.com.hadryan.agro.manager.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users/me/preferences")
@RequiredArgsConstructor
public class UserPreferencesController {

    private final UserPreferencesService userPreferencesService;

    @GetMapping
    public ResponseEntity<ApiResponse<UserPreferencesResponse>> get(
            @AuthenticationPrincipal UserPrincipal principal) {
        UserPreferencesResponse prefs = userPreferencesService.getOrCreate(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(prefs));
    }

    @PatchMapping
    public ResponseEntity<ApiResponse<UserPreferencesResponse>> update(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UserPreferencesRequest request) {
        UserPreferencesResponse prefs = userPreferencesService.update(principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(prefs));
    }
}
