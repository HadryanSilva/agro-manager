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
@RequestMapping("/accounts/{accountId}/employee-payments")
@RequiredArgsConstructor
public class EmployeePaymentController {

    private final EmployeePaymentService paymentService;

    @PostMapping
    public ResponseEntity<ApiResponse<EmployeePaymentResponse>> pay(
            @PathVariable UUID accountId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody EmployeePaymentRequest request) {
        EmployeePaymentResponse response = paymentService.pay(accountId, principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<EmployeePaymentResponse>>> list(
            @PathVariable UUID accountId,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) UUID employeeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int safeSize = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, safeSize, Sort.by(Sort.Direction.DESC, "paymentDate"));
        PageResponse<EmployeePaymentResponse> response = paymentService.list(
                accountId, principal.getId(), employeeId, startDate, endDate, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<ApiResponse<EmployeePaymentResponse>> findById(
            @PathVariable UUID accountId,
            @PathVariable UUID paymentId,
            @AuthenticationPrincipal UserPrincipal principal) {
        EmployeePaymentResponse response = paymentService.findById(accountId, principal.getId(), paymentId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
