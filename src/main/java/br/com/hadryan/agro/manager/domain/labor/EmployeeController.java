package br.com.hadryan.agro.manager.domain.labor;

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

@RestController
@RequestMapping("/accounts/{accountId}/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    @PostMapping
    public ResponseEntity<ApiResponse<EmployeeResponse>> create(
            @PathVariable UUID accountId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody EmployeeRequest request) {
        EmployeeResponse response = employeeService.create(accountId, principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<EmployeeResponse>>> list(
            @PathVariable UUID accountId,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Boolean active) {
        List<EmployeeResponse> employees = employeeService.list(accountId, principal.getId(), active);
        return ResponseEntity.ok(ApiResponse.success(employees));
    }

    @GetMapping("/{employeeId}")
    public ResponseEntity<ApiResponse<EmployeeResponse>> findById(
            @PathVariable UUID accountId,
            @PathVariable UUID employeeId,
            @AuthenticationPrincipal UserPrincipal principal) {
        EmployeeResponse employee = employeeService.findById(accountId, principal.getId(), employeeId);
        return ResponseEntity.ok(ApiResponse.success(employee));
    }

    @PutMapping("/{employeeId}")
    public ResponseEntity<ApiResponse<EmployeeResponse>> update(
            @PathVariable UUID accountId,
            @PathVariable UUID employeeId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody EmployeeRequest request) {
        EmployeeResponse response = employeeService.update(accountId, principal.getId(), employeeId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/{employeeId}/activate")
    public ResponseEntity<ApiResponse<EmployeeResponse>> activate(
            @PathVariable UUID accountId,
            @PathVariable UUID employeeId,
            @AuthenticationPrincipal UserPrincipal principal) {
        EmployeeResponse response = employeeService.activate(accountId, principal.getId(), employeeId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/{employeeId}/deactivate")
    public ResponseEntity<ApiResponse<EmployeeResponse>> deactivate(
            @PathVariable UUID accountId,
            @PathVariable UUID employeeId,
            @AuthenticationPrincipal UserPrincipal principal) {
        EmployeeResponse response = employeeService.deactivate(accountId, principal.getId(), employeeId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
