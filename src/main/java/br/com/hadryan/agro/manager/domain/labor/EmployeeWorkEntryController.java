package br.com.hadryan.agro.manager.domain.labor;

import br.com.hadryan.agro.manager.infra.security.UserPrincipal;
import br.com.hadryan.agro.manager.shared.dto.ApiResponse;
import br.com.hadryan.agro.manager.shared.dto.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/accounts/{accountId}/employee-work-entries")
@RequiredArgsConstructor
public class EmployeeWorkEntryController {

    private final EmployeeWorkEntryService workEntryService;

    @PostMapping
    public ResponseEntity<ApiResponse<EmployeeWorkEntryResponse>> create(
            @PathVariable UUID accountId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody EmployeeWorkEntryRequest request) {
        EmployeeWorkEntryResponse response = workEntryService.create(accountId, principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<EmployeeWorkEntryResponse>>> list(
            @PathVariable UUID accountId,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) UUID employeeId,
            @RequestParam(required = false) UUID farmId,
            @RequestParam(required = false) Boolean paid,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int safeSize = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, safeSize, Sort.by(Sort.Direction.DESC, "workDate"));
        PageResponse<EmployeeWorkEntryResponse> response = workEntryService.list(
                accountId, principal.getId(), employeeId, farmId, paid, startDate, endDate, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{entryId}")
    public ResponseEntity<ApiResponse<EmployeeWorkEntryResponse>> findById(
            @PathVariable UUID accountId,
            @PathVariable UUID entryId,
            @AuthenticationPrincipal UserPrincipal principal) {
        EmployeeWorkEntryResponse response = workEntryService.findById(accountId, principal.getId(), entryId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{entryId}")
    public ResponseEntity<ApiResponse<EmployeeWorkEntryResponse>> update(
            @PathVariable UUID accountId,
            @PathVariable UUID entryId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody EmployeeWorkEntryRequest request) {
        EmployeeWorkEntryResponse response = workEntryService.update(accountId, principal.getId(), entryId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{entryId}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID accountId,
            @PathVariable UUID entryId,
            @AuthenticationPrincipal UserPrincipal principal) {
        workEntryService.delete(accountId, principal.getId(), entryId);
        return ResponseEntity.noContent().build();
    }
}
