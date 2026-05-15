# Employee Daily Work Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add an employee daily-work module that tracks pending labor days and creates paid service expenses when an employee is paid.

**Architecture:** Add a new `domain/labor` package with employee, work-entry, and payment resources. Pending work entries live only in labor tables; the payment service groups unpaid entries by farm/general destination and creates regular `Expense` rows inside one transaction.

**Tech Stack:** Java 25, Spring Boot, Spring MVC, Spring Data JPA, Bean Validation, Flyway, PostgreSQL, JUnit 5, MockMvc.

---

## File Structure

- Create `src/main/resources/db/migration/V24__create_labor_tables.sql`
  - Owns schema for employees, employee work entries, payments, and payment-expense links.
- Create `src/main/java/br/com/hadryan/agro/manager/domain/labor/Employee.java`
  - JPA entity for a worker owned by an account.
- Create `src/main/java/br/com/hadryan/agro/manager/domain/labor/EmployeeRepository.java`
  - Account-scoped employee queries.
- Create `src/main/java/br/com/hadryan/agro/manager/domain/labor/EmployeeRequest.java`
  - Create/update employee payload.
- Create `src/main/java/br/com/hadryan/agro/manager/domain/labor/EmployeeResponse.java`
  - Employee API response.
- Create `src/main/java/br/com/hadryan/agro/manager/domain/labor/EmployeeService.java`
  - Employee CRUD and activation service.
- Create `src/main/java/br/com/hadryan/agro/manager/domain/labor/EmployeeController.java`
  - REST endpoints under `/accounts/{accountId}/employees`.
- Create `src/main/java/br/com/hadryan/agro/manager/domain/labor/EmployeeWorkEntry.java`
  - JPA entity for one worked day.
- Create `src/main/java/br/com/hadryan/agro/manager/domain/labor/EmployeeWorkEntryRepository.java`
  - Query, duplicate detection, and pending-entry lookup.
- Create `src/main/java/br/com/hadryan/agro/manager/domain/labor/EmployeeWorkEntryRequest.java`
  - Create/update work-entry payload.
- Create `src/main/java/br/com/hadryan/agro/manager/domain/labor/EmployeeWorkEntryResponse.java`
  - Work-entry API response.
- Create `src/main/java/br/com/hadryan/agro/manager/domain/labor/EmployeeWorkEntryService.java`
  - Work-entry creation, listing, update, and delete rules.
- Create `src/main/java/br/com/hadryan/agro/manager/domain/labor/EmployeeWorkEntryController.java`
  - REST endpoints under `/accounts/{accountId}/employee-work-entries`.
- Create `src/main/java/br/com/hadryan/agro/manager/domain/labor/EmployeePayment.java`
  - JPA entity for one employee payment.
- Create `src/main/java/br/com/hadryan/agro/manager/domain/labor/EmployeePaymentExpense.java`
  - JPA entity linking payments to generated expenses.
- Create `src/main/java/br/com/hadryan/agro/manager/domain/labor/EmployeePaymentExpenseId.java`
  - Composite id for the payment-expense join entity.
- Create `src/main/java/br/com/hadryan/agro/manager/domain/labor/EmployeePaymentRepository.java`
  - Payment queries.
- Create `src/main/java/br/com/hadryan/agro/manager/domain/labor/EmployeePaymentExpenseRepository.java`
  - Persist generated expense links.
- Create `src/main/java/br/com/hadryan/agro/manager/domain/labor/EmployeePaymentRequest.java`
  - Payment payload.
- Create `src/main/java/br/com/hadryan/agro/manager/domain/labor/EmployeePaymentResponse.java`
  - Payment response with generated expense references.
- Create `src/main/java/br/com/hadryan/agro/manager/domain/labor/GeneratedExpenseResponse.java`
  - Small DTO for generated expense id, farm id/name, and amount.
- Create `src/main/java/br/com/hadryan/agro/manager/domain/labor/EmployeePaymentService.java`
  - Transactional payment workflow and expense generation.
- Create `src/main/java/br/com/hadryan/agro/manager/domain/labor/EmployeePaymentController.java`
  - REST endpoints under `/accounts/{accountId}/employee-payments`.
- Create `src/test/java/br/com/hadryan/agro/manager/EmployeeLaborIntegrationTest.java`
  - End-to-end tests for employees, work entries, payments, generated expenses, and account isolation.

## Implementation Tasks

### Task 1: Add Labor Schema

**Files:**
- Create: `src/main/resources/db/migration/V24__create_labor_tables.sql`

- [ ] **Step 1: Write the Flyway migration**

Create `src/main/resources/db/migration/V24__create_labor_tables.sql`:

```sql
CREATE TABLE employees (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id  UUID NOT NULL REFERENCES accounts (id) ON DELETE CASCADE,
    name        VARCHAR(150) NOT NULL,
    daily_rate  DECIMAL(12, 2) NOT NULL,
    active      BOOLEAN NOT NULL DEFAULT TRUE,
    notes       TEXT,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_employees_daily_rate_positive CHECK (daily_rate > 0)
);

CREATE INDEX idx_employees_account_name ON employees (account_id, name);
CREATE INDEX idx_employees_account_active ON employees (account_id, active);

CREATE TABLE employee_payments (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id    UUID NOT NULL REFERENCES accounts (id) ON DELETE CASCADE,
    employee_id   UUID NOT NULL REFERENCES employees (id) ON DELETE RESTRICT,
    period_start  DATE NOT NULL,
    period_end    DATE NOT NULL,
    payment_date  DATE NOT NULL,
    total_amount  DECIMAL(12, 2) NOT NULL,
    notes         TEXT,
    created_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_employee_payments_period CHECK (period_end >= period_start),
    CONSTRAINT chk_employee_payments_total_positive CHECK (total_amount > 0)
);

CREATE INDEX idx_employee_payments_account_date ON employee_payments (account_id, payment_date DESC);
CREATE INDEX idx_employee_payments_employee_period ON employee_payments (employee_id, period_start, period_end);

CREATE TABLE employee_work_entries (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id    UUID NOT NULL REFERENCES accounts (id) ON DELETE CASCADE,
    employee_id   UUID NOT NULL REFERENCES employees (id) ON DELETE RESTRICT,
    farm_id       UUID REFERENCES farms (id) ON DELETE RESTRICT,
    work_date     DATE NOT NULL,
    daily_rate    DECIMAL(12, 2) NOT NULL,
    payment_id    UUID REFERENCES employee_payments (id) ON DELETE RESTRICT,
    notes         TEXT,
    created_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT uk_employee_work_entries_employee_date UNIQUE (employee_id, work_date),
    CONSTRAINT chk_employee_work_entries_daily_rate_positive CHECK (daily_rate > 0)
);

CREATE INDEX idx_employee_work_entries_account_date ON employee_work_entries (account_id, work_date DESC);
CREATE INDEX idx_employee_work_entries_employee_date ON employee_work_entries (employee_id, work_date DESC);
CREATE INDEX idx_employee_work_entries_farm_date ON employee_work_entries (farm_id, work_date DESC);
CREATE INDEX idx_employee_work_entries_pending ON employee_work_entries (employee_id, work_date)
    WHERE payment_id IS NULL;

CREATE TABLE employee_payment_expenses (
    payment_id  UUID NOT NULL REFERENCES employee_payments (id) ON DELETE CASCADE,
    expense_id  UUID NOT NULL REFERENCES expenses (id) ON DELETE RESTRICT,

    PRIMARY KEY (payment_id, expense_id),
    CONSTRAINT uk_employee_payment_expenses_expense UNIQUE (expense_id)
);
```

- [ ] **Step 2: Run schema verification**

Run:

```powershell
C:\Workspace\Backend\Java\agro-manager\gradlew.bat test --tests br.com.hadryan.agro.manager.AgroManagerApplicationTests
```

Expected: the application context starts and Flyway applies `V24__create_labor_tables.sql` without migration errors.

- [ ] **Step 3: Commit schema**

Run:

```powershell
git add src/main/resources/db/migration/V24__create_labor_tables.sql
git commit -m "feat(labor): add daily work schema"
```

### Task 2: Add Employee API

**Files:**
- Create: `src/main/java/br/com/hadryan/agro/manager/domain/labor/Employee.java`
- Create: `src/main/java/br/com/hadryan/agro/manager/domain/labor/EmployeeRepository.java`
- Create: `src/main/java/br/com/hadryan/agro/manager/domain/labor/EmployeeRequest.java`
- Create: `src/main/java/br/com/hadryan/agro/manager/domain/labor/EmployeeResponse.java`
- Create: `src/main/java/br/com/hadryan/agro/manager/domain/labor/EmployeeService.java`
- Create: `src/main/java/br/com/hadryan/agro/manager/domain/labor/EmployeeController.java`
- Create/Modify: `src/test/java/br/com/hadryan/agro/manager/EmployeeLaborIntegrationTest.java`

- [ ] **Step 1: Write failing employee CRUD tests**

Create `src/test/java/br/com/hadryan/agro/manager/EmployeeLaborIntegrationTest.java`:

```java
package br.com.hadryan.agro.manager;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Mao de obra - funcionarios, diarias e pagamentos")
class EmployeeLaborIntegrationTest extends MockMvcIntegrationTestBase {

    private record LaborContext(String token, String accountId, String farmId) {}

    private LaborContext setup() throws Exception {
        String token = registerAndGetToken(uniqueEmail(), "senha12345");
        String accountId = createAccount(token, "Fazenda Labor");
        String farmId = createFarm(token, accountId, "Lavoura Labor");
        return new LaborContext(token, accountId, farmId);
    }

    @Test
    @DisplayName("Deve criar, listar, atualizar, desativar e ativar funcionario")
    void shouldManageEmployeeLifecycle() throws Exception {
        var ctx = setup();

        String employeeId = createEmployee(ctx.token(), ctx.accountId(), "Joao Diarista", 120.00);

        mockMvc.perform(get("/accounts/" + ctx.accountId() + "/employees")
                        .header("Authorization", "Bearer " + ctx.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(employeeId))
                .andExpect(jsonPath("$.data[0].name").value("Joao Diarista"))
                .andExpect(jsonPath("$.data[0].dailyRate").value(120.00))
                .andExpect(jsonPath("$.data[0].active").value(true));

        Map<String, Object> updatePayload = new HashMap<>();
        updatePayload.put("name", "Joao Atualizado");
        updatePayload.put("dailyRate", 130.00);
        updatePayload.put("notes", "Equipe de colheita");

        mockMvc.perform(put("/accounts/" + ctx.accountId() + "/employees/" + employeeId)
                        .header("Authorization", "Bearer " + ctx.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatePayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Joao Atualizado"))
                .andExpect(jsonPath("$.data.dailyRate").value(130.00))
                .andExpect(jsonPath("$.data.notes").value("Equipe de colheita"));

        mockMvc.perform(patch("/accounts/" + ctx.accountId() + "/employees/" + employeeId + "/deactivate")
                        .header("Authorization", "Bearer " + ctx.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active").value(false));

        mockMvc.perform(patch("/accounts/" + ctx.accountId() + "/employees/" + employeeId + "/activate")
                        .header("Authorization", "Bearer " + ctx.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active").value(true));
    }

    private String createEmployee(String token, String accountId, String name, double dailyRate) throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("name", name);
        payload.put("dailyRate", dailyRate);

        MvcResult result = mockMvc.perform(post("/accounts/" + accountId + "/employees")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("id").asText();
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```powershell
C:\Workspace\Backend\Java\agro-manager\gradlew.bat test --tests br.com.hadryan.agro.manager.EmployeeLaborIntegrationTest
```

Expected: FAIL with 404 for `/accounts/{accountId}/employees`.

- [ ] **Step 3: Implement employee entity and DTOs**

Create `Employee.java`:

```java
package br.com.hadryan.agro.manager.domain.labor;

import br.com.hadryan.agro.manager.domain.account.Account;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "employees")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "daily_rate", nullable = false, precision = 12, scale = 2)
    private BigDecimal dailyRate;

    @Column(nullable = false)
    private boolean active;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
```

Create `EmployeeRequest.java`:

```java
package br.com.hadryan.agro.manager.domain.labor;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record EmployeeRequest(
        @NotBlank(message = "Nome e obrigatorio")
        @Size(max = 150, message = "Nome deve ter no maximo 150 caracteres")
        String name,

        @NotNull(message = "Valor da diaria e obrigatorio")
        @DecimalMin(value = "0.01", message = "Valor da diaria deve ser maior que zero")
        BigDecimal dailyRate,

        String notes
) {}
```

Create `EmployeeResponse.java`:

```java
package br.com.hadryan.agro.manager.domain.labor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record EmployeeResponse(
        UUID id,
        String name,
        BigDecimal dailyRate,
        boolean active,
        String notes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static EmployeeResponse from(Employee employee) {
        return new EmployeeResponse(
                employee.getId(),
                employee.getName(),
                employee.getDailyRate(),
                employee.isActive(),
                employee.getNotes(),
                employee.getCreatedAt(),
                employee.getUpdatedAt()
        );
    }
}
```

Create `EmployeeRepository.java`:

```java
package br.com.hadryan.agro.manager.domain.labor;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, UUID> {
    List<Employee> findByAccountIdOrderByNameAsc(UUID accountId);
    List<Employee> findByAccountIdAndActiveOrderByNameAsc(UUID accountId, boolean active);
    Optional<Employee> findByIdAndAccountId(UUID id, UUID accountId);
}
```

- [ ] **Step 4: Implement employee service and controller**

Create `EmployeeService.java`:

```java
package br.com.hadryan.agro.manager.domain.labor;

import br.com.hadryan.agro.manager.domain.account.Account;
import br.com.hadryan.agro.manager.domain.account.AccountMemberRepository;
import br.com.hadryan.agro.manager.domain.account.AccountRepository;
import br.com.hadryan.agro.manager.shared.exception.BusinessException;
import br.com.hadryan.agro.manager.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final AccountRepository accountRepository;
    private final AccountMemberRepository accountMemberRepository;

    @Transactional
    public EmployeeResponse create(UUID accountId, UUID userId, EmployeeRequest request) {
        Account account = validateAndGetAccount(accountId, userId);
        Employee employee = Employee.builder()
                .account(account)
                .name(request.name().trim())
                .dailyRate(request.dailyRate())
                .active(true)
                .notes(request.notes())
                .build();
        return EmployeeResponse.from(employeeRepository.save(employee));
    }

    @Transactional(readOnly = true)
    public List<EmployeeResponse> list(UUID accountId, UUID userId, Boolean active) {
        validateMembership(accountId, userId);
        List<Employee> employees = active == null
                ? employeeRepository.findByAccountIdOrderByNameAsc(accountId)
                : employeeRepository.findByAccountIdAndActiveOrderByNameAsc(accountId, active);
        return employees.stream().map(EmployeeResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public EmployeeResponse findById(UUID accountId, UUID userId, UUID employeeId) {
        validateMembership(accountId, userId);
        return EmployeeResponse.from(findEmployee(employeeId, accountId));
    }

    @Transactional
    public EmployeeResponse update(UUID accountId, UUID userId, UUID employeeId, EmployeeRequest request) {
        validateMembership(accountId, userId);
        Employee employee = findEmployee(employeeId, accountId);
        employee.setName(request.name().trim());
        employee.setDailyRate(request.dailyRate());
        employee.setNotes(request.notes());
        return EmployeeResponse.from(employeeRepository.save(employee));
    }

    @Transactional
    public EmployeeResponse activate(UUID accountId, UUID userId, UUID employeeId) {
        validateMembership(accountId, userId);
        Employee employee = findEmployee(employeeId, accountId);
        employee.setActive(true);
        return EmployeeResponse.from(employeeRepository.save(employee));
    }

    @Transactional
    public EmployeeResponse deactivate(UUID accountId, UUID userId, UUID employeeId) {
        validateMembership(accountId, userId);
        Employee employee = findEmployee(employeeId, accountId);
        employee.setActive(false);
        return EmployeeResponse.from(employeeRepository.save(employee));
    }

    private Account validateAndGetAccount(UUID accountId, UUID userId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Conta", "id", accountId));
        if (!accountMemberRepository.existsByAccountIdAndUserId(accountId, userId)) {
            throw new BusinessException("Acesso negado a esta conta", HttpStatus.FORBIDDEN);
        }
        return account;
    }

    private void validateMembership(UUID accountId, UUID userId) {
        if (!accountRepository.existsById(accountId)) {
            throw new ResourceNotFoundException("Conta", "id", accountId);
        }
        if (!accountMemberRepository.existsByAccountIdAndUserId(accountId, userId)) {
            throw new BusinessException("Acesso negado a esta conta", HttpStatus.FORBIDDEN);
        }
    }

    Employee findEmployee(UUID employeeId, UUID accountId) {
        return employeeRepository.findByIdAndAccountId(employeeId, accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Funcionario", "id", employeeId));
    }
}
```

Create `EmployeeController.java`:

```java
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
        return ResponseEntity.ok(ApiResponse.success(employeeService.list(accountId, principal.getId(), active)));
    }

    @GetMapping("/{employeeId}")
    public ResponseEntity<ApiResponse<EmployeeResponse>> findById(
            @PathVariable UUID accountId,
            @PathVariable UUID employeeId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(employeeService.findById(accountId, principal.getId(), employeeId)));
    }

    @PutMapping("/{employeeId}")
    public ResponseEntity<ApiResponse<EmployeeResponse>> update(
            @PathVariable UUID accountId,
            @PathVariable UUID employeeId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody EmployeeRequest request) {
        return ResponseEntity.ok(ApiResponse.success(employeeService.update(accountId, principal.getId(), employeeId, request)));
    }

    @PatchMapping("/{employeeId}/activate")
    public ResponseEntity<ApiResponse<EmployeeResponse>> activate(
            @PathVariable UUID accountId,
            @PathVariable UUID employeeId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(employeeService.activate(accountId, principal.getId(), employeeId)));
    }

    @PatchMapping("/{employeeId}/deactivate")
    public ResponseEntity<ApiResponse<EmployeeResponse>> deactivate(
            @PathVariable UUID accountId,
            @PathVariable UUID employeeId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(employeeService.deactivate(accountId, principal.getId(), employeeId)));
    }
}
```

- [ ] **Step 5: Run employee test**

Run:

```powershell
C:\Workspace\Backend\Java\agro-manager\gradlew.bat test --tests br.com.hadryan.agro.manager.EmployeeLaborIntegrationTest
```

Expected: PASS for the employee lifecycle test.

- [ ] **Step 6: Commit employee API**

Run:

```powershell
git add src/main/java/br/com/hadryan/agro/manager/domain/labor src/test/java/br/com/hadryan/agro/manager/EmployeeLaborIntegrationTest.java
git commit -m "feat(labor): add employee management"
```

### Task 3: Add Work Entry API

**Files:**
- Create: `src/main/java/br/com/hadryan/agro/manager/domain/labor/EmployeeWorkEntry.java`
- Create: `src/main/java/br/com/hadryan/agro/manager/domain/labor/EmployeeWorkEntryRepository.java`
- Create: `src/main/java/br/com/hadryan/agro/manager/domain/labor/EmployeeWorkEntryRequest.java`
- Create: `src/main/java/br/com/hadryan/agro/manager/domain/labor/EmployeeWorkEntryResponse.java`
- Create: `src/main/java/br/com/hadryan/agro/manager/domain/labor/EmployeeWorkEntryService.java`
- Create: `src/main/java/br/com/hadryan/agro/manager/domain/labor/EmployeeWorkEntryController.java`
- Modify: `src/test/java/br/com/hadryan/agro/manager/EmployeeLaborIntegrationTest.java`

- [ ] **Step 1: Add failing work-entry tests**

Append these tests and helpers to `EmployeeLaborIntegrationTest` before the final closing brace:

```java
    @Test
    @DisplayName("Deve criar diaria geral e diaria vinculada a lavoura")
    void shouldCreateGeneralAndFarmWorkEntries() throws Exception {
        var ctx = setup();
        String employeeId = createEmployee(ctx.token(), ctx.accountId(), "Maria Diarista", 100.00);

        String generalEntryId = createWorkEntry(ctx.token(), ctx.accountId(), employeeId, null, "2026-05-11", null);
        String farmEntryId = createWorkEntry(ctx.token(), ctx.accountId(), employeeId, ctx.farmId(), "2026-05-12", 140.00);

        mockMvc.perform(get("/accounts/" + ctx.accountId() + "/employee-work-entries")
                        .param("employeeId", employeeId)
                        .param("paid", "false")
                        .header("Authorization", "Bearer " + ctx.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.content[0].id").value(farmEntryId))
                .andExpect(jsonPath("$.data.content[0].farmId").value(ctx.farmId()))
                .andExpect(jsonPath("$.data.content[0].dailyRate").value(140.00))
                .andExpect(jsonPath("$.data.content[0].paid").value(false))
                .andExpect(jsonPath("$.data.content[1].id").value(generalEntryId))
                .andExpect(jsonPath("$.data.content[1].farmId").doesNotExist())
                .andExpect(jsonPath("$.data.content[1].dailyRate").value(100.00));
    }

    @Test
    @DisplayName("Deve rejeitar diaria duplicada e diaria para funcionario inativo")
    void shouldRejectDuplicateAndInactiveEmployeeWorkEntry() throws Exception {
        var ctx = setup();
        String employeeId = createEmployee(ctx.token(), ctx.accountId(), "Pedro Diarista", 90.00);
        createWorkEntry(ctx.token(), ctx.accountId(), employeeId, null, "2026-05-13", null);

        Map<String, Object> duplicatePayload = new HashMap<>();
        duplicatePayload.put("employeeId", employeeId);
        duplicatePayload.put("workDate", "2026-05-13");

        mockMvc.perform(post("/accounts/" + ctx.accountId() + "/employee-work-entries")
                        .header("Authorization", "Bearer " + ctx.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicatePayload)))
                .andExpect(status().isConflict());

        mockMvc.perform(patch("/accounts/" + ctx.accountId() + "/employees/" + employeeId + "/deactivate")
                        .header("Authorization", "Bearer " + ctx.token()))
                .andExpect(status().isOk());

        Map<String, Object> inactivePayload = new HashMap<>();
        inactivePayload.put("employeeId", employeeId);
        inactivePayload.put("workDate", "2026-05-14");

        mockMvc.perform(post("/accounts/" + ctx.accountId() + "/employee-work-entries")
                        .header("Authorization", "Bearer " + ctx.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inactivePayload)))
                .andExpect(status().isBadRequest());
    }

    private String createWorkEntry(String token, String accountId, String employeeId, String farmId,
                                   String workDate, Double dailyRate) throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("employeeId", employeeId);
        if (farmId != null) {
            payload.put("farmId", farmId);
        }
        payload.put("workDate", workDate);
        if (dailyRate != null) {
            payload.put("dailyRate", dailyRate);
        }

        MvcResult result = mockMvc.perform(post("/accounts/" + accountId + "/employee-work-entries")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("id").asText();
    }
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```powershell
C:\Workspace\Backend\Java\agro-manager\gradlew.bat test --tests br.com.hadryan.agro.manager.EmployeeLaborIntegrationTest
```

Expected: FAIL with 404 for `/accounts/{accountId}/employee-work-entries`.

- [ ] **Step 3: Implement work-entry entity, DTOs, and repository**

Create `EmployeeWorkEntry.java`, `EmployeeWorkEntryRequest.java`, `EmployeeWorkEntryResponse.java`, and `EmployeeWorkEntryRepository.java` using these exact contracts:

```java
// EmployeeWorkEntryRequest.java
package br.com.hadryan.agro.manager.domain.labor;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record EmployeeWorkEntryRequest(
        @NotNull(message = "Funcionario e obrigatorio")
        UUID employeeId,
        UUID farmId,
        @NotNull(message = "Data de trabalho e obrigatoria")
        LocalDate workDate,
        @DecimalMin(value = "0.01", message = "Valor da diaria deve ser maior que zero")
        BigDecimal dailyRate,
        String notes
) {}
```

```java
// EmployeeWorkEntryResponse.java
package br.com.hadryan.agro.manager.domain.labor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record EmployeeWorkEntryResponse(
        UUID id,
        UUID employeeId,
        String employeeName,
        UUID farmId,
        String farmName,
        LocalDate workDate,
        BigDecimal dailyRate,
        boolean paid,
        UUID paymentId,
        String notes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static EmployeeWorkEntryResponse from(EmployeeWorkEntry entry) {
        return new EmployeeWorkEntryResponse(
                entry.getId(),
                entry.getEmployee().getId(),
                entry.getEmployee().getName(),
                entry.getFarm() != null ? entry.getFarm().getId() : null,
                entry.getFarm() != null ? entry.getFarm().getName() : null,
                entry.getWorkDate(),
                entry.getDailyRate(),
                entry.isPaid(),
                entry.getPayment() != null ? entry.getPayment().getId() : null,
                entry.getNotes(),
                entry.getCreatedAt(),
                entry.getUpdatedAt()
        );
    }
}
```

```java
// EmployeeWorkEntryRepository.java
package br.com.hadryan.agro.manager.domain.labor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmployeeWorkEntryRepository extends JpaRepository<EmployeeWorkEntry, UUID> {

    boolean existsByEmployeeIdAndWorkDate(UUID employeeId, LocalDate workDate);

    boolean existsByEmployeeIdAndWorkDateAndIdNot(UUID employeeId, LocalDate workDate, UUID id);

    Optional<EmployeeWorkEntry> findByIdAndAccountId(UUID id, UUID accountId);

    @Query("""
            SELECT e FROM EmployeeWorkEntry e
            JOIN FETCH e.employee emp
            LEFT JOIN FETCH e.farm f
            LEFT JOIN FETCH e.payment p
            WHERE e.account.id = :accountId
              AND (:employeeId IS NULL OR emp.id = :employeeId)
              AND (:farmId IS NULL OR f.id = :farmId)
              AND (:paid IS NULL OR
                   (:paid = true AND p IS NOT NULL) OR
                   (:paid = false AND p IS NULL))
              AND (:startDate IS NULL OR e.workDate >= :startDate)
              AND (:endDate IS NULL OR e.workDate <= :endDate)
            ORDER BY e.workDate DESC, e.createdAt DESC
            """)
    Page<EmployeeWorkEntry> findEntries(
            @Param("accountId") UUID accountId,
            @Param("employeeId") UUID employeeId,
            @Param("farmId") UUID farmId,
            @Param("paid") Boolean paid,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable
    );

    @Query("""
            SELECT e FROM EmployeeWorkEntry e
            LEFT JOIN FETCH e.farm
            WHERE e.account.id = :accountId
              AND e.employee.id = :employeeId
              AND e.payment IS NULL
              AND e.workDate BETWEEN :periodStart AND :periodEnd
            ORDER BY e.workDate ASC
            """)
    List<EmployeeWorkEntry> findPendingForPayment(
            @Param("accountId") UUID accountId,
            @Param("employeeId") UUID employeeId,
            @Param("periodStart") LocalDate periodStart,
            @Param("periodEnd") LocalDate periodEnd
    );
}
```

Use `EmployeeWorkEntry.java` with fields from the design: `id`, `account`, `employee`, optional `farm`, `workDate`, `dailyRate`, optional `payment`, `notes`, timestamps, and `isPaid()` returning `payment != null`.

- [ ] **Step 4: Implement work-entry service and controller**

Create `EmployeeWorkEntryService.java` with these public methods:

```java
public EmployeeWorkEntryResponse create(UUID accountId, UUID userId, EmployeeWorkEntryRequest request)
public PageResponse<EmployeeWorkEntryResponse> list(UUID accountId, UUID userId, UUID employeeId, UUID farmId, Boolean paid, LocalDate startDate, LocalDate endDate, Pageable pageable)
public EmployeeWorkEntryResponse findById(UUID accountId, UUID userId, UUID entryId)
public EmployeeWorkEntryResponse update(UUID accountId, UUID userId, UUID entryId, EmployeeWorkEntryRequest request)
public void delete(UUID accountId, UUID userId, UUID entryId)
```

Implement these rules inside the service:

```java
if (!employee.isActive()) {
    throw new BusinessException("Funcionario inativo nao pode receber diaria");
}
if (entryRepository.existsByEmployeeIdAndWorkDate(employee.getId(), request.workDate())) {
    throw new BusinessException("Funcionario ja possui diaria nesta data", HttpStatus.CONFLICT);
}
if (entry.isPaid()) {
    throw new BusinessException("Diaria paga nao pode ser alterada", HttpStatus.CONFLICT);
}
```

Create `EmployeeWorkEntryController.java` with:

```java
@RestController
@RequestMapping("/accounts/{accountId}/employee-work-entries")
@RequiredArgsConstructor
public class EmployeeWorkEntryController {
    private final EmployeeWorkEntryService workEntryService;
}
```

Add `POST`, `GET`, `GET /{entryId}`, `PUT /{entryId}`, and `DELETE /{entryId}` methods matching the service signatures. Return `201 Created` for create, `200 OK` for reads/updates, and `204 No Content` for delete.

- [ ] **Step 5: Run work-entry tests**

Run:

```powershell
C:\Workspace\Backend\Java\agro-manager\gradlew.bat test --tests br.com.hadryan.agro.manager.EmployeeLaborIntegrationTest
```

Expected: PASS for employee lifecycle and work-entry tests.

- [ ] **Step 6: Commit work-entry API**

Run:

```powershell
git add src/main/java/br/com/hadryan/agro/manager/domain/labor src/test/java/br/com/hadryan/agro/manager/EmployeeLaborIntegrationTest.java
git commit -m "feat(labor): add daily work entries"
```

### Task 4: Add Payment API And Expense Generation

**Files:**
- Create: `src/main/java/br/com/hadryan/agro/manager/domain/labor/EmployeePayment.java`
- Create: `src/main/java/br/com/hadryan/agro/manager/domain/labor/EmployeePaymentExpense.java`
- Create: `src/main/java/br/com/hadryan/agro/manager/domain/labor/EmployeePaymentExpenseId.java`
- Create: `src/main/java/br/com/hadryan/agro/manager/domain/labor/EmployeePaymentRepository.java`
- Create: `src/main/java/br/com/hadryan/agro/manager/domain/labor/EmployeePaymentExpenseRepository.java`
- Create: `src/main/java/br/com/hadryan/agro/manager/domain/labor/EmployeePaymentRequest.java`
- Create: `src/main/java/br/com/hadryan/agro/manager/domain/labor/GeneratedExpenseResponse.java`
- Create: `src/main/java/br/com/hadryan/agro/manager/domain/labor/EmployeePaymentResponse.java`
- Create: `src/main/java/br/com/hadryan/agro/manager/domain/labor/EmployeePaymentService.java`
- Create: `src/main/java/br/com/hadryan/agro/manager/domain/labor/EmployeePaymentController.java`
- Modify: `src/test/java/br/com/hadryan/agro/manager/EmployeeLaborIntegrationTest.java`

- [ ] **Step 1: Add failing payment tests**

Append these tests to `EmployeeLaborIntegrationTest`:

```java
    @Test
    @DisplayName("Deve pagar periodo misto e gerar despesas separadas")
    void shouldPayMixedPeriodAndGenerateSeparateExpenses() throws Exception {
        var ctx = setup();
        String employeeId = createEmployee(ctx.token(), ctx.accountId(), "Ana Diarista", 100.00);
        createWorkEntry(ctx.token(), ctx.accountId(), employeeId, null, "2026-05-11", null);
        createWorkEntry(ctx.token(), ctx.accountId(), employeeId, ctx.farmId(), "2026-05-12", 150.00);

        Map<String, Object> paymentPayload = new HashMap<>();
        paymentPayload.put("employeeId", employeeId);
        paymentPayload.put("periodStart", "2026-05-11");
        paymentPayload.put("periodEnd", "2026-05-12");
        paymentPayload.put("paymentDate", "2026-05-15");
        paymentPayload.put("notes", "Pagamento semanal");

        MvcResult result = mockMvc.perform(post("/accounts/" + ctx.accountId() + "/employee-payments")
                        .header("Authorization", "Bearer " + ctx.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paymentPayload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.employeeId").value(employeeId))
                .andExpect(jsonPath("$.data.totalAmount").value(250.00))
                .andExpect(jsonPath("$.data.paidEntriesCount").value(2))
                .andExpect(jsonPath("$.data.generatedExpenses.length()").value(2))
                .andReturn();

        String paymentId = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("id").asText();

        mockMvc.perform(get("/accounts/" + ctx.accountId() + "/employee-work-entries")
                        .param("employeeId", employeeId)
                        .param("paid", "true")
                        .header("Authorization", "Bearer " + ctx.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.content[0].paymentId").value(paymentId))
                .andExpect(jsonPath("$.data.content[1].paymentId").value(paymentId));

        mockMvc.perform(get("/accounts/" + ctx.accountId() + "/farms/" + ctx.farmId() + "/report")
                        .header("Authorization", "Bearer " + ctx.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalServicos").value(150.00))
                .andExpect(jsonPath("$.data.totalPaid").value(150.00));

        mockMvc.perform(get("/accounts/" + ctx.accountId() + "/transactions")
                        .param("general", "true")
                        .header("Authorization", "Bearer " + ctx.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].category").value("SERVICO"))
                .andExpect(jsonPath("$.data.content[0].value").value(100.00))
                .andExpect(jsonPath("$.data.content[0].paid").value(true));
    }

    @Test
    @DisplayName("Deve rejeitar pagamento sem diarias pendentes")
    void shouldRejectPaymentWithoutPendingEntries() throws Exception {
        var ctx = setup();
        String employeeId = createEmployee(ctx.token(), ctx.accountId(), "Carlos Diarista", 100.00);

        Map<String, Object> paymentPayload = new HashMap<>();
        paymentPayload.put("employeeId", employeeId);
        paymentPayload.put("periodStart", "2026-05-11");
        paymentPayload.put("periodEnd", "2026-05-12");
        paymentPayload.put("paymentDate", "2026-05-15");

        mockMvc.perform(post("/accounts/" + ctx.accountId() + "/employee-payments")
                        .header("Authorization", "Bearer " + ctx.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paymentPayload)))
                .andExpect(status().isBadRequest());
    }
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```powershell
C:\Workspace\Backend\Java\agro-manager\gradlew.bat test --tests br.com.hadryan.agro.manager.EmployeeLaborIntegrationTest
```

Expected: FAIL with 404 for `/accounts/{accountId}/employee-payments`.

- [ ] **Step 3: Implement payment DTOs and repositories**

Create:

```java
// EmployeePaymentRequest.java
package br.com.hadryan.agro.manager.domain.labor;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record EmployeePaymentRequest(
        @NotNull(message = "Funcionario e obrigatorio")
        UUID employeeId,
        @NotNull(message = "Inicio do periodo e obrigatorio")
        LocalDate periodStart,
        @NotNull(message = "Fim do periodo e obrigatorio")
        LocalDate periodEnd,
        @NotNull(message = "Data de pagamento e obrigatoria")
        LocalDate paymentDate,
        String notes
) {}
```

```java
// GeneratedExpenseResponse.java
package br.com.hadryan.agro.manager.domain.labor;

import java.math.BigDecimal;
import java.util.UUID;

public record GeneratedExpenseResponse(
        UUID expenseId,
        UUID farmId,
        String farmName,
        BigDecimal amount
) {}
```

```java
// EmployeePaymentResponse.java
package br.com.hadryan.agro.manager.domain.labor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record EmployeePaymentResponse(
        UUID id,
        UUID employeeId,
        String employeeName,
        LocalDate periodStart,
        LocalDate periodEnd,
        LocalDate paymentDate,
        BigDecimal totalAmount,
        long paidEntriesCount,
        String notes,
        List<GeneratedExpenseResponse> generatedExpenses,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
```

Create payment entities with the schema fields and `@PrePersist/@PreUpdate` timestamp hooks. `EmployeePaymentExpenseId` must be `@Embeddable` with `UUID paymentId` and `UUID expenseId`; `EmployeePaymentExpense` must use `@EmbeddedId`, `@MapsId("paymentId")`, and `@MapsId("expenseId")`.

- [ ] **Step 4: Implement payment workflow**

Create `EmployeePaymentService.java` with `@Transactional` on `pay(...)`.

Core implementation shape:

```java
public EmployeePaymentResponse pay(UUID accountId, UUID userId, EmployeePaymentRequest request) {
    if (request.periodEnd().isBefore(request.periodStart())) {
        throw new BusinessException("Fim do periodo nao pode ser anterior ao inicio");
    }

    Account account = validateAndGetAccount(accountId, userId);
    Employee employee = employeeService.findEmployee(request.employeeId(), accountId);
    List<EmployeeWorkEntry> entries = workEntryRepository.findPendingForPayment(
            accountId, employee.getId(), request.periodStart(), request.periodEnd());

    if (entries.isEmpty()) {
        throw new BusinessException("Nao ha diarias pendentes para o periodo informado");
    }

    BigDecimal total = entries.stream()
            .map(EmployeeWorkEntry::getDailyRate)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    EmployeePayment payment = paymentRepository.save(EmployeePayment.builder()
            .account(account)
            .employee(employee)
            .periodStart(request.periodStart())
            .periodEnd(request.periodEnd())
            .paymentDate(request.paymentDate())
            .totalAmount(total)
            .notes(request.notes())
            .build());

    Map<UUID, List<EmployeeWorkEntry>> byFarm = entries.stream()
            .filter(e -> e.getFarm() != null)
            .collect(Collectors.groupingBy(e -> e.getFarm().getId(), LinkedHashMap::new, Collectors.toList()));
    List<EmployeeWorkEntry> generalEntries = entries.stream()
            .filter(e -> e.getFarm() == null)
            .toList();

    List<GeneratedExpenseResponse> generated = new ArrayList<>();
    for (List<EmployeeWorkEntry> group : byFarm.values()) {
        generated.add(createExpenseForGroup(account, group.getFirst().getFarm(), employee, payment, request, group));
    }
    if (!generalEntries.isEmpty()) {
        generated.add(createExpenseForGroup(account, null, employee, payment, request, generalEntries));
    }

    entries.forEach(entry -> entry.setPayment(payment));
    workEntryRepository.saveAll(entries);

    return toResponse(payment, entries.size(), generated);
}
```

Use direct `Expense` creation through `ExpenseRepository.save(...)` so the payment service can create farm and general expenses in the same transaction. Set:

```java
Expense expense = Expense.builder()
        .account(account)
        .farm(farm)
        .description("Diarias de %s - %s a %s".formatted(employee.getName(), request.periodStart(), request.periodEnd()))
        .category(ExpenseCategory.SERVICO)
        .value(amount)
        .competenceDate(request.periodEnd())
        .paymentDate(request.paymentDate())
        .notes(buildExpenseNotes(request.notes(), group))
        .build();
```

After saving the expense, persist `EmployeePaymentExpense` and return `GeneratedExpenseResponse`.

- [ ] **Step 5: Implement payment controller**

Create `EmployeePaymentController.java`:

```java
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
}
```

Add `GET /accounts/{accountId}/employee-payments` and `GET /accounts/{accountId}/employee-payments/{paymentId}` after the create flow passes. The list endpoint should return `PageResponse<EmployeePaymentResponse>` with `BigDecimal.ZERO` as `totalValue` unless a payment totalizer is added in the repository.

- [ ] **Step 6: Run payment tests**

Run:

```powershell
C:\Workspace\Backend\Java\agro-manager\gradlew.bat test --tests br.com.hadryan.agro.manager.EmployeeLaborIntegrationTest
```

Expected: PASS for employee, work-entry, and payment tests.

- [ ] **Step 7: Commit payment API**

Run:

```powershell
git add src/main/java/br/com/hadryan/agro/manager/domain/labor src/test/java/br/com/hadryan/agro/manager/EmployeeLaborIntegrationTest.java
git commit -m "feat(labor): pay daily work as expenses"
```

### Task 5: Harden Edge Cases And Run Full Verification

**Files:**
- Modify: `src/test/java/br/com/hadryan/agro/manager/EmployeeLaborIntegrationTest.java`
- Modify: `src/main/java/br/com/hadryan/agro/manager/domain/labor/EmployeeWorkEntryService.java`
- Modify: `src/main/java/br/com/hadryan/agro/manager/domain/labor/EmployeePaymentService.java`

- [ ] **Step 1: Add paid-entry lock and account-isolation tests**

Append:

```java
    @Test
    @DisplayName("Nao deve editar ou excluir diaria paga")
    void shouldBlockUpdateAndDeletePaidWorkEntry() throws Exception {
        var ctx = setup();
        String employeeId = createEmployee(ctx.token(), ctx.accountId(), "Luiz Diarista", 100.00);
        String entryId = createWorkEntry(ctx.token(), ctx.accountId(), employeeId, null, "2026-05-11", null);
        payEmployee(ctx.token(), ctx.accountId(), employeeId, "2026-05-11", "2026-05-11", "2026-05-15");

        Map<String, Object> updatePayload = new HashMap<>();
        updatePayload.put("employeeId", employeeId);
        updatePayload.put("workDate", "2026-05-11");
        updatePayload.put("dailyRate", 110.00);

        mockMvc.perform(put("/accounts/" + ctx.accountId() + "/employee-work-entries/" + entryId)
                        .header("Authorization", "Bearer " + ctx.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatePayload)))
                .andExpect(status().isConflict());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/accounts/" + ctx.accountId() + "/employee-work-entries/" + entryId)
                        .header("Authorization", "Bearer " + ctx.token()))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Deve isolar funcionarios entre contas")
    void shouldIsolateEmployeesAcrossAccounts() throws Exception {
        var ctxA = setup();
        var ctxB = setup();
        String employeeA = createEmployee(ctxA.token(), ctxA.accountId(), "Conta A", 100.00);

        mockMvc.perform(get("/accounts/" + ctxB.accountId() + "/employees/" + employeeA)
                        .header("Authorization", "Bearer " + ctxB.token()))
                .andExpect(status().isNotFound());
    }

    private String payEmployee(String token, String accountId, String employeeId,
                               String periodStart, String periodEnd, String paymentDate) throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("employeeId", employeeId);
        payload.put("periodStart", periodStart);
        payload.put("periodEnd", periodEnd);
        payload.put("paymentDate", paymentDate);

        MvcResult result = mockMvc.perform(post("/accounts/" + accountId + "/employee-payments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("id").asText();
    }
```

- [ ] **Step 2: Run focused labor tests**

Run:

```powershell
C:\Workspace\Backend\Java\agro-manager\gradlew.bat test --tests br.com.hadryan.agro.manager.EmployeeLaborIntegrationTest
```

Expected: PASS.

- [ ] **Step 3: Run full backend tests**

Run:

```powershell
C:\Workspace\Backend\Java\agro-manager\gradlew.bat test
```

Expected: PASS for all tests.

- [ ] **Step 4: Run full project check**

Run:

```powershell
C:\Workspace\Backend\Java\agro-manager\gradlew.bat check
```

Expected: PASS.

- [ ] **Step 5: Commit hardening**

Run:

```powershell
git add src/main/java/br/com/hadryan/agro/manager/domain/labor src/test/java/br/com/hadryan/agro/manager/EmployeeLaborIntegrationTest.java
git commit -m "test(labor): cover payment edge cases"
```

## Self-Review

- Spec coverage:
  - Employee registration, active flag, and default daily rate are covered in Task 2.
  - One entry per employee/date, optional farm, rate override, and pending status are covered in Task 3.
  - Employee-period payment, grouping by farm/general, generated paid expenses, and payment-entry linking are covered in Task 4.
  - Paid-entry immutability and account isolation are covered in Task 5.
- Completion-language scan:
  - The plan contains no incomplete markers or deferred sections.
- Type consistency:
  - Endpoint paths match the approved spec.
  - DTO field names use Java camelCase and map to SQL snake_case through entity columns.
  - Payment links use a relational join entity, not description parsing.

## Execution Handoff

Plan complete and saved to `docs/superpowers/plans/2026-05-15-employee-daily-work.md`. Two execution options:

1. Subagent-Driven (recommended) - dispatch a fresh subagent per task, review between tasks, fast iteration.

2. Inline Execution - execute tasks in this session using executing-plans, batch execution with checkpoints.

Which approach?
