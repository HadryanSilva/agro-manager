package br.com.hadryan.agro.manager.domain.farm;

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
 * Endpoints do histórico de atividades de uma lavoura.
 */
@RestController
@RequestMapping("/accounts/{accountId}/farms/{farmId}/activities")
@RequiredArgsConstructor
public class FarmActivityController {

    private final FarmActivityService activityService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<FarmActivityResponse>>> getActivities(
            @PathVariable UUID accountId,
            @PathVariable UUID farmId,
            @AuthenticationPrincipal UserPrincipal principal) {

        List<FarmActivityResponse> activities =
                activityService.getActivities(accountId, farmId, principal.getId());

        return ResponseEntity.ok(ApiResponse.success(activities));
    }

    @PostMapping("/notes")
    public ResponseEntity<ApiResponse<FarmActivityResponse>> addNote(
            @PathVariable UUID accountId,
            @PathVariable UUID farmId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody NoteRequest request) {

        FarmActivityResponse activity =
                activityService.addNote(accountId, farmId, principal.getId(), request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Anotação registrada com sucesso", activity));
    }
}