# Credit Expense + In-App Notification Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add `creditPurchase` + `dueDate` fields to expenses, validate them in the service, expose a `/upcoming` endpoint, and surface upcoming-due expenses as a dashboard card and sidebar badge via a shared Vue composable.

**Architecture:** DB migration adds the two nullable/defaulted columns; backend wires them through entity → request/response DTOs → service validation + repository query → new controller endpoint on the existing `GeneralExpenseController`; frontend adds TypeScript types, a checkbox in the expense form, a singleton `useNotifications` composable, and reads from Dashboard + AppLayout.

**Tech Stack:** Java 21, Spring Boot 3, Flyway, JPA/Hibernate, MockMvc + Testcontainers (integration tests), Vue 3 Composition API, TypeScript.

---

## File Map

| Action | File |
|--------|------|
| Create | `src/main/resources/db/migration/V26__add_credit_fields_to_expenses.sql` |
| Modify | `src/main/java/br/com/hadryan/agro/manager/domain/expense/Expense.java` |
| Modify | `src/main/java/br/com/hadryan/agro/manager/domain/expense/ExpenseRequest.java` |
| Modify | `src/main/java/br/com/hadryan/agro/manager/domain/expense/ExpenseResponse.java` |
| Modify | `src/main/java/br/com/hadryan/agro/manager/domain/expense/ExpenseService.java` |
| Modify | `src/main/java/br/com/hadryan/agro/manager/domain/expense/ExpenseServiceImpl.java` |
| Modify | `src/main/java/br/com/hadryan/agro/manager/domain/expense/ExpenseRepository.java` |
| Modify | `src/main/java/br/com/hadryan/agro/manager/domain/expense/GeneralExpenseController.java` |
| Modify | `src/main/resources/application-dev.yaml` |
| Modify | `src/test/java/br/com/hadryan/agro/manager/ExpenseIntegrationTest.java` |
| Modify | `agro-manager-web/src/services/expenseService.ts` |
| Modify | `agro-manager-web/src/views/expenses/ExpenseFormView.vue` |
| Create | `agro-manager-web/src/composables/useNotifications.ts` |
| Modify | `agro-manager-web/src/views/DashboardView.vue` |
| Modify | `agro-manager-web/src/layouts/AppLayout.vue` |

---

## Task 1: DB Migration V26

**Files:**
- Create: `src/main/resources/db/migration/V26__add_credit_fields_to_expenses.sql`

- [ ] **Step 1: Write the migration**

```sql
ALTER TABLE expenses
  ADD COLUMN is_credit BOOLEAN NOT NULL DEFAULT FALSE,
  ADD COLUMN due_date  DATE;

CREATE INDEX idx_expenses_upcoming
  ON expenses (account_id, due_date)
  WHERE is_credit = TRUE AND payment_date IS NULL;
```

`DEFAULT FALSE` preserves retrocompatibility — all existing rows get `is_credit = false`. `due_date` is nullable in the DB; the service enforces the constraint when `is_credit = true`.

- [ ] **Step 2: Commit**

```bash
git add src/main/resources/db/migration/V26__add_credit_fields_to_expenses.sql
git commit -m "feat(db): add is_credit and due_date columns to expenses table (V26)"
```

---

## Task 2: Integration Tests — Write Failing Tests (TDD)

**Files:**
- Modify: `src/test/java/br/com/hadryan/agro/manager/ExpenseIntegrationTest.java`

- [ ] **Step 1: Add four failing tests to ExpenseIntegrationTest**

Add these tests to the class (after the existing tests). They will fail until the backend is wired.

```java
@Test
@DisplayName("Deve criar despesa como compra no prazo com data de vencimento")
void shouldCreateCreditPurchaseWithDueDate() throws Exception {
    var ctx = setup();

    Map<String, Object> payload = new HashMap<>();
    payload.put("description", "Sementes a prazo");
    payload.put("category", "INSUMO");
    payload.put("value", 5000.00);
    payload.put("competenceDate", "2025-04-01");
    payload.put("creditPurchase", true);
    payload.put("dueDate", "2025-05-15");

    mockMvc.perform(post("/accounts/" + ctx.accountId() + "/farms/" + ctx.farmId() + "/expenses")
                    .header("Authorization", "Bearer " + ctx.token())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.creditPurchase").value(true))
            .andExpect(jsonPath("$.data.dueDate").value("2025-05-15"));
}

@Test
@DisplayName("Deve retornar creditPurchase false em despesas normais")
void shouldReturnFalseForCreditPurchaseOnNormalExpenses() throws Exception {
    var ctx = setup();
    String expenseId = createExpense(ctx.token(), ctx.accountId(), ctx.farmId(),
            "Despesa Normal", "INSUMO", 1000.0, "2025-01-01");

    mockMvc.perform(get("/accounts/" + ctx.accountId() + "/farms/" + ctx.farmId() + "/expenses/" + expenseId)
                    .header("Authorization", "Bearer " + ctx.token()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.creditPurchase").value(false));
}

@Test
@DisplayName("Deve rejeitar compra no prazo sem data de vencimento — retorna 400")
void shouldRejectCreditPurchaseWithoutDueDate() throws Exception {
    var ctx = setup();

    Map<String, Object> payload = new HashMap<>();
    payload.put("description", "Compra sem vencimento");
    payload.put("category", "INSUMO");
    payload.put("value", 1000.00);
    payload.put("competenceDate", "2025-04-01");
    payload.put("creditPurchase", true);
    // dueDate ausente

    mockMvc.perform(post("/accounts/" + ctx.accountId() + "/farms/" + ctx.farmId() + "/expenses")
                    .header("Authorization", "Bearer " + ctx.token())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false));
}

@Test
@DisplayName("Deve retornar despesas próximas do vencimento via /upcoming")
void shouldReturnUpcomingCreditExpenses() throws Exception {
    var ctx = setup();

    // Despesa com vencimento em 3 dias — deve aparecer no /upcoming (janela padrão: 7 dias)
    Map<String, Object> upcoming = new HashMap<>();
    upcoming.put("description", "Insumo com vencimento próximo");
    upcoming.put("category", "INSUMO");
    upcoming.put("value", 3000.00);
    upcoming.put("competenceDate", "2025-04-01");
    upcoming.put("creditPurchase", true);
    upcoming.put("dueDate", java.time.LocalDate.now().plusDays(3).toString());

    mockMvc.perform(post("/accounts/" + ctx.accountId() + "/farms/" + ctx.farmId() + "/expenses")
                    .header("Authorization", "Bearer " + ctx.token())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(upcoming)))
            .andExpect(status().isCreated());

    // Despesa paga não deve aparecer
    Map<String, Object> paid = new HashMap<>();
    paid.put("description", "Despesa Já Paga");
    paid.put("category", "INSUMO");
    paid.put("value", 500.00);
    paid.put("competenceDate", "2025-04-01");
    paid.put("creditPurchase", true);
    paid.put("dueDate", java.time.LocalDate.now().plusDays(2).toString());
    paid.put("paymentDate", "2025-04-15");

    mockMvc.perform(post("/accounts/" + ctx.accountId() + "/farms/" + ctx.farmId() + "/expenses")
                    .header("Authorization", "Bearer " + ctx.token())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(paid)))
            .andExpect(status().isCreated());

    // Despesa sem creditPurchase não deve aparecer
    createExpense(ctx.token(), ctx.accountId(), ctx.farmId(),
            "Despesa Normal", "SERVICO", 200.0, "2025-04-01");

    mockMvc.perform(get("/accounts/" + ctx.accountId() + "/expenses/upcoming")
                    .header("Authorization", "Bearer " + ctx.token()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].description").value("Insumo com vencimento próximo"));
}
```

Also add this import at the top of the file (if not already present):

```java
import java.time.LocalDate;
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
cd /home/hadryan/Dev/projects/agro-manager
./gradlew test --tests "br.com.hadryan.agro.manager.ExpenseIntegrationTest" --rerun
```

Expected: 4 new tests fail.
- `shouldCreateCreditPurchaseWithDueDate` → fails (field not in JSON)
- `shouldReturnFalseForCreditPurchaseOnNormalExpenses` → fails (field not in JSON)
- `shouldRejectCreditPurchaseWithoutDueDate` → fails (returns 201, no validation yet)
- `shouldReturnUpcomingCreditExpenses` → fails (404 on `/upcoming`)

Existing tests must still pass (migration already applied via Testcontainers).

- [ ] **Step 3: Commit the failing tests**

```bash
git add src/test/java/br/com/hadryan/agro/manager/ExpenseIntegrationTest.java
git commit -m "test(expense): add failing tests for credit purchase + /upcoming (TDD)"
```

---

## Task 3: Backend — Entity, DTOs, Service, Repository, Controller

**Files:**
- Modify: `src/main/java/br/com/hadryan/agro/manager/domain/expense/Expense.java`
- Modify: `src/main/java/br/com/hadryan/agro/manager/domain/expense/ExpenseRequest.java`
- Modify: `src/main/java/br/com/hadryan/agro/manager/domain/expense/ExpenseResponse.java`
- Modify: `src/main/java/br/com/hadryan/agro/manager/domain/expense/ExpenseService.java`
- Modify: `src/main/java/br/com/hadryan/agro/manager/domain/expense/ExpenseServiceImpl.java`
- Modify: `src/main/java/br/com/hadryan/agro/manager/domain/expense/ExpenseRepository.java`
- Modify: `src/main/java/br/com/hadryan/agro/manager/domain/expense/GeneralExpenseController.java`
- Modify: `src/main/resources/application-dev.yaml`

### Step 1: Expense.java — add two fields after `paymentDate`

- [ ] Add these two fields in `Expense.java` after the `paymentDate` field:

```java
@Column(name = "is_credit", nullable = false)
@Builder.Default
private boolean creditPurchase = false;

@Column(name = "due_date")
private LocalDate dueDate;
```

**Why `creditPurchase` not `isCredit`:** Lombok generates `isCreditPurchase()` for `boolean creditPurchase`. If named `isCredit`, Lombok would generate `isIsCredit()` — double-is is a bug. The DB column is still `is_credit` (set via `@Column(name = "is_credit")`).

**Why `@Builder.Default`:** Without it, the Lombok builder leaves the field uninitialized (false for boolean), but `@Builder.Default` makes the intent explicit and prevents future surprises.

### Step 2: ExpenseRequest.java — add two fields

- [ ] Add after the `notes` field (before the closing `)`):

```java
// Compra no prazo — boxed Boolean para compatibilidade com Jackson 3.x (campo ausente em payloads legados)
Boolean creditPurchase,
LocalDate dueDate
```

**Why `Boolean` (boxed) not `boolean`:** Jackson 3.x throws `MismatchedInputException` when a primitive `boolean` field is absent from the JSON. Boxed `Boolean` maps absent → `null`, which `Boolean.TRUE.equals(null)` handles safely. All existing payloads that omit this field continue to work.

### Step 3: ExpenseResponse.java — add two fields and update `from()`

- [ ] Add after the `paid` field in the record component list:

```java
boolean creditPurchase,
LocalDate dueDate,
```

- [ ] Update `from()` to pass the two new values (add after `expense.isPaid()`):

```java
expense.isCreditPurchase(),
expense.getDueDate(),
```

Full updated `from()`:

```java
public static ExpenseResponse from(Expense expense) {
    return new ExpenseResponse(
            expense.getId(),
            expense.getDescription(),
            expense.getCategory(),
            expense.getValue(),
            expense.getCompetenceDate(),
            expense.getPaymentDate(),
            expense.isPaid(),
            expense.isCreditPurchase(),
            expense.getDueDate(),
            expense.getNotes(),
            expense.getFarm() != null ? expense.getFarm().getId() : null,
            expense.getFarm() != null ? expense.getFarm().getName() : null,
            expense.getCreatedAt(),
            expense.getUpdatedAt()
    );
}
```

### Step 4: ExpenseService.java — add `findUpcoming` to interface

- [ ] Add `import java.util.List;` at the top.
- [ ] Add this method signature to the interface:

```java
List<ExpenseResponse> findUpcoming(UUID accountId, UUID userId, int days);
```

### Step 5: ExpenseRepository.java — add `findUpcomingCreditExpenses`

- [ ] Add this query method at the end of the repository interface:

```java
@Query("""
    SELECT e FROM Expense e
    WHERE e.account.id = :accountId
      AND e.creditPurchase = true
      AND e.paymentDate IS NULL
      AND e.dueDate BETWEEN :today AND :until
    ORDER BY e.dueDate ASC
""")
List<Expense> findUpcomingCreditExpenses(
    @Param("accountId") UUID accountId,
    @Param("today") LocalDate today,
    @Param("until") LocalDate until
);
```

### Step 6: ExpenseServiceImpl.java — validation + field mapping + findUpcoming

- [ ] Add `import java.util.List;` at the top.

- [ ] Add a private validation helper at the end of the private utilities section:

```java
private void validateCreditPurchase(ExpenseRequest request) {
    if (Boolean.TRUE.equals(request.creditPurchase()) && request.dueDate() == null) {
        throw new BusinessException("Data de vencimento obrigatória para compra no prazo", HttpStatus.BAD_REQUEST);
    }
}
```

- [ ] In `create()` (farm expense), add `validateCreditPurchase(request);` **before** `Expense.builder()`, and add two fields in the builder:

```java
.creditPurchase(Boolean.TRUE.equals(request.creditPurchase()))
.dueDate(Boolean.TRUE.equals(request.creditPurchase()) ? request.dueDate() : null)
```

Full updated `create()` (farm expense):

```java
@Transactional
public ExpenseResponse create(UUID accountId, UUID farmId, UUID userId, ExpenseRequest request) {
    Farm farm = findFarmAndValidate(accountId, farmId, userId);
    validateCreditPurchase(request);

    Expense expense = Expense.builder()
            .account(farm.getAccount())
            .farm(farm)
            .description(request.description())
            .category(request.category())
            .value(request.value())
            .competenceDate(request.competenceDate())
            .paymentDate(request.paymentDate())
            .notes(request.notes())
            .creditPurchase(Boolean.TRUE.equals(request.creditPurchase()))
            .dueDate(Boolean.TRUE.equals(request.creditPurchase()) ? request.dueDate() : null)
            .build();

    ExpenseResponse saved = ExpenseResponse.from(expenseRepository.save(expense));

    String categoryLabel = request.category() == ExpenseCategory.INSUMO ? "Insumo" : "Serviço";
    activityService.record(
            farmId, userId,
            FarmActivityType.EXPENSE_CREATED,
            "Despesa registrada: %s — %s — R$ %s".formatted(
                    request.description(), categoryLabel, request.value()),
            saved.id()
    );
    return saved;
}
```

- [ ] In `update()` (farm expense), add `validateCreditPurchase(request);` **before** the field setters, and add two setters:

```java
expense.setCreditPurchase(Boolean.TRUE.equals(request.creditPurchase()));
expense.setDueDate(Boolean.TRUE.equals(request.creditPurchase()) ? request.dueDate() : null);
```

- [ ] In `createGeneral()`, add `validateCreditPurchase(request);` and the two builder fields:

```java
.creditPurchase(Boolean.TRUE.equals(request.creditPurchase()))
.dueDate(Boolean.TRUE.equals(request.creditPurchase()) ? request.dueDate() : null)
```

- [ ] In `updateGeneral()`, add `validateCreditPurchase(request);` and the two setters:

```java
expense.setCreditPurchase(Boolean.TRUE.equals(request.creditPurchase()));
expense.setDueDate(Boolean.TRUE.equals(request.creditPurchase()) ? request.dueDate() : null);
```

- [ ] Add `findUpcoming` implementation in the "Despesas gerais" section (after `markGeneralAsPaid`):

```java
@Transactional(readOnly = true)
public List<ExpenseResponse> findUpcoming(UUID accountId, UUID userId, int days) {
    validateMembership(accountId, userId);
    LocalDate today = LocalDate.now();
    LocalDate until = today.plusDays(days);
    return expenseRepository.findUpcomingCreditExpenses(accountId, today, until)
            .stream()
            .map(ExpenseResponse::from)
            .toList();
}
```

### Step 7: GeneralExpenseController.java — add `/upcoming` endpoint

- [ ] Add `import java.util.List;` and `import org.springframework.beans.factory.annotation.Value;` at the top.

- [ ] Add the `@Value` field after the `expenseService` field:

```java
@Value("${app.notifications.default-days-ahead:7}")
private int defaultDaysAhead;
```

- [ ] Add the endpoint at the end of the class:

```java
@GetMapping("/upcoming")
public ResponseEntity<ApiResponse<List<ExpenseResponse>>> upcoming(
        @PathVariable UUID accountId,
        @RequestParam(required = false) Integer days,
        @AuthenticationPrincipal UserPrincipal principal) {
    int d = (days != null) ? days : defaultDaysAhead;
    List<ExpenseResponse> result = expenseService.findUpcoming(accountId, principal.getId(), d);
    return ResponseEntity.ok(ApiResponse.success(result));
}
```

### Step 8: application-dev.yaml — add default-days-ahead config

- [ ] Append to the `app:` block in `application-dev.yaml`:

```yaml
  notifications:
    default-days-ahead: 7
```

The full `app:` section becomes:

```yaml
app:
  jwt:
    secret: ${JWT_SECRET:dev-only-secret-key-do-not-use-in-production!!}
    expiration: 900000
    refresh-expiration: 604800000
  cors:
    allowed-origins: http://localhost:5173
  frontend-url: http://localhost:5173
  mail:
    from-name: ${MAIL_FROM_NAME:Agro Manager}
    from-address: ${MAIL_FROM_ADDRESS:john.doe@email.com}
  notifications:
    default-days-ahead: 7
```

### Step 9: Run tests to verify all 4 new tests pass

```bash
cd /home/hadryan/Dev/projects/agro-manager
./gradlew test --tests "br.com.hadryan.agro.manager.ExpenseIntegrationTest" --rerun
```

Expected: all tests pass (0 failures). Previous passing tests must still pass.

- [ ] **Step 10: Commit**

```bash
git add \
  src/main/java/br/com/hadryan/agro/manager/domain/expense/Expense.java \
  src/main/java/br/com/hadryan/agro/manager/domain/expense/ExpenseRequest.java \
  src/main/java/br/com/hadryan/agro/manager/domain/expense/ExpenseResponse.java \
  src/main/java/br/com/hadryan/agro/manager/domain/expense/ExpenseService.java \
  src/main/java/br/com/hadryan/agro/manager/domain/expense/ExpenseServiceImpl.java \
  src/main/java/br/com/hadryan/agro/manager/domain/expense/ExpenseRepository.java \
  src/main/java/br/com/hadryan/agro/manager/domain/expense/GeneralExpenseController.java \
  src/main/resources/application-dev.yaml
git commit -m "feat(expense): add creditPurchase + dueDate fields and /upcoming endpoint"
```

---

## Task 4: Frontend — expenseService.ts (TypeScript Types + upcoming method)

**Files:**
- Modify: `agro-manager-web/src/services/expenseService.ts`

- [ ] **Step 1: Add two fields to `ExpenseResponse`**

After `paid: boolean` add:

```typescript
creditPurchase: boolean
dueDate: string | null
```

- [ ] **Step 2: Add two optional fields to `ExpenseRequest`**

After `paymentDate?: string | null` add:

```typescript
creditPurchase?: boolean
dueDate?: string
```

- [ ] **Step 3: Add `upcoming` service method**

Add at the end of the `expenseService` object, after `markGeneralAsPaid`:

```typescript
upcoming: (accountId: string, days = 7) =>
  api
    .get<{ data: ListPayload<ExpenseResponse> }>(
      `/accounts/${accountId}/expenses/upcoming`,
      { params: { days } }
    )
    .then(normalizeListResponse<ExpenseResponse>),
```

The `ListPayload` import is already present at the top of the file. `normalizeListResponse` receives `AxiosResponse<{ data: T[] | {content?, items?, results?} }>`. Since the backend returns `{ "success": true, "data": [...] }`, `response.data.data` is the plain array, which `normalizeListPayload` returns directly. Result type: `AxiosResponse<{ data: ExpenseResponse[] }>`.

- [ ] **Step 4: Commit**

```bash
cd /home/hadryan/Dev/projects/agro-manager-web
git add src/services/expenseService.ts
git commit -m "feat(frontend): add creditPurchase/dueDate types and upcoming() to expenseService"
```

---

## Task 5: Frontend — ExpenseFormView.vue (creditPurchase checkbox + dueDate field)

**Files:**
- Modify: `agro-manager-web/src/views/expenses/ExpenseFormView.vue`

- [ ] **Step 1: Add state refs** — insert after `const notes = ref('')`:

```typescript
const creditPurchase = ref(false)
const dueDate        = ref('')
```

- [ ] **Step 2: Load in `onMounted`** — in the edit block, after `notes.value = e.notes ?? ''`:

```typescript
creditPurchase.value = e.creditPurchase ?? false
dueDate.value        = e.dueDate ?? ''
```

- [ ] **Step 3: Include in `handleSubmit` payload** — after `notes: notes.value || undefined`:

```typescript
creditPurchase: creditPurchase.value || undefined,
dueDate:        creditPurchase.value ? dueDate.value || undefined : undefined,
```

- [ ] **Step 4: Disable save when creditPurchase but no dueDate** — update the `btn-primary` `:disabled`:

```html
:disabled="loading || !description || !value || !competenceDate || (creditPurchase && !dueDate)"
```

- [ ] **Step 5: Add the form section** — insert a new `<div class="form-section">` block **after** the closing `</div>` of the "Datas" section (which ends at the row with `paymentDate`), and **before** the "Observações" section:

```html
<!-- Compra no prazo -->
<div class="form-section">
  <div class="form-section__header">
    <h2 class="form-section__title">Compra no prazo</h2>
    <label class="checkbox-label">
      <input v-model="creditPurchase" type="checkbox" class="checkbox-input" />
      <span>Marcar como compra no prazo</span>
    </label>
  </div>

  <div v-if="creditPurchase" class="form-field form-field--credit">
    <label class="form-label">Data de vencimento *</label>
    <input
      v-model="dueDate"
      type="date"
      class="form-input"
      :required="creditPurchase"
    />
    <span class="form-hint">Prazo para pagamento desta compra</span>
  </div>
</div>
```

- [ ] **Step 6: Add CSS** — append to the `<style scoped>` block:

```css
/* ── Compra no prazo ─────────────────────────────────────────── */
.form-section__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 0.5rem;
}
.form-section__header .form-section__title { margin-bottom: 0; }

.checkbox-label {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.875rem;
  font-weight: 500;
  color: var(--color-text);
  cursor: pointer;
}

.checkbox-input {
  width: 16px;
  height: 16px;
  accent-color: var(--color-primary);
  cursor: pointer;
  flex-shrink: 0;
}

.form-field--credit { margin-top: 0.875rem; }
```

- [ ] **Step 7: Commit**

```bash
git add src/views/expenses/ExpenseFormView.vue
git commit -m "feat(frontend): add creditPurchase checkbox and dueDate field to expense form"
```

---

## Task 6: Frontend — useNotifications.ts (new composable)

**Files:**
- Create: `agro-manager-web/src/composables/useNotifications.ts`

- [ ] **Step 1: Create the composables directory and file**

```bash
mkdir -p /home/hadryan/Dev/projects/agro-manager-web/src/composables
```

- [ ] **Step 2: Write the composable**

```typescript
import { ref, computed } from 'vue'
import expenseService from '@/services/expenseService'
import type { ExpenseResponse } from '@/services/expenseService'

// Module-level ref — all callers share the same instance (singleton).
// Dashboard and AppLayout both read this without duplicate API calls.
const upcomingExpenses = ref<ExpenseResponse[]>([])

export function useNotifications() {
  async function loadUpcoming(accountId: string) {
    try {
      const { data } = await expenseService.upcoming(accountId)
      upcomingExpenses.value = data.data
    } catch {
      // Notification failure must not break the UI — silently ignore
    }
  }

  const upcomingCount = computed(() => upcomingExpenses.value.length)

  return { upcomingExpenses, upcomingCount, loadUpcoming }
}
```

- [ ] **Step 3: Commit**

```bash
git add src/composables/useNotifications.ts
git commit -m "feat(frontend): add useNotifications composable for upcoming credit expenses"
```

---

## Task 7: Frontend — DashboardView.vue (upcoming card) + AppLayout.vue (sidebar badge)

**Files:**
- Modify: `agro-manager-web/src/views/DashboardView.vue`
- Modify: `agro-manager-web/src/layouts/AppLayout.vue`

### DashboardView.vue

- [ ] **Step 1: Import and instantiate useNotifications**

Add to the `<script setup>` imports:

```typescript
import { useNotifications } from '@/composables/useNotifications'
```

Instantiate after the existing store/ref declarations:

```typescript
const { upcomingExpenses, upcomingCount, loadUpcoming } = useNotifications()
```

- [ ] **Step 2: Call `loadUpcoming` in `fetchDashboard`**

In `fetchDashboard()`, after the `dashboardService.getSummary` call succeeds (inside the `try` block after `summary.value = data.data`):

```typescript
if (accountId.value) await loadUpcoming(accountId.value)
```

- [ ] **Step 3: Add the `formatDate` helper** (it already exists in the file — verify before adding to avoid duplication)

- [ ] **Step 4: Add upcoming card to the template**

Insert a new `<div class="section">` block **between** the "Resumo financeiro" section and the "Distribuição por status" section (inside the `<template v-else-if="summary">` block):

```html
<!-- ── Vencimentos próximos ──────────────────────────────────── -->
<div v-if="upcomingCount > 0" class="section">
  <h2 class="section__title">Vencimentos próximos</h2>
  <div class="upcoming-list">
    <div
      v-for="expense in upcomingExpenses"
      :key="expense.id"
      class="upcoming-item"
      tabindex="0"
      @click="navigateToExpense(expense)"
      @keydown.enter="navigateToExpense(expense)"
    >
      <div class="upcoming-item__info">
        <span class="upcoming-item__desc">{{ expense.description }}</span>
        <span v-if="expense.farmName" class="upcoming-item__farm">{{ expense.farmName }}</span>
      </div>
      <div class="upcoming-item__right">
        <span class="upcoming-item__value">{{ formatCurrency(expense.value) }}</span>
        <span class="upcoming-item__due">Vence {{ formatDate(expense.dueDate) }}</span>
      </div>
    </div>
  </div>
</div>
```

- [ ] **Step 5: Add `navigateToExpense` helper function**

Add to the script after `fetchDashboard`:

```typescript
function navigateToExpense(expense: ExpenseResponse) {
  if (expense.farmId) {
    router.push({ name: 'farm-expenses', params: { farmId: expense.farmId } })
  } else {
    router.push({ name: 'transactions' })
  }
}
```

Add the import for `ExpenseResponse` at the top:

```typescript
import type { ExpenseResponse } from '@/services/expenseService'
```

- [ ] **Step 6: Add CSS for the upcoming card**

Append to `<style scoped>`:

```css
/* ── Vencimentos próximos ──────────────────────────────────── */
.upcoming-list {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.upcoming-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 0.875rem 1rem;
  background: var(--color-card);
  border: 1px solid var(--color-warning);
  border-left: 3px solid var(--color-warning);
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: background 0.12s;
}

.upcoming-item:hover { background: var(--color-warning-light); }
.upcoming-item:focus-visible { outline: 2px solid var(--color-warning); outline-offset: 2px; }

.upcoming-item__info {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
  min-width: 0;
}

.upcoming-item__desc {
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--color-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.upcoming-item__farm {
  font-size: 0.75rem;
  color: var(--color-text-muted);
}

.upcoming-item__right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 0.2rem;
  flex-shrink: 0;
}

.upcoming-item__value {
  font-size: 0.875rem;
  font-weight: 700;
  color: var(--color-text);
}

.upcoming-item__due {
  font-size: 0.75rem;
  color: var(--color-warning);
  font-weight: 600;
}
```

### AppLayout.vue

- [ ] **Step 7: Import useNotifications and accountStore**

Add to `<script setup>` imports:

```typescript
import { useNotifications } from '@/composables/useNotifications'
import { useAccountStore } from '@/stores/accountStore'
```

Instantiate:

```typescript
const accountStore = useAccountStore()
const { upcomingCount, loadUpcoming } = useNotifications()
```

- [ ] **Step 8: Call `loadUpcoming` in `onMounted`**

Update `onMounted` to also load upcoming:

```typescript
onMounted(async () => {
  userStore.fetchProfile()
  if (accountStore.selectedAccount?.id) {
    await loadUpcoming(accountStore.selectedAccount.id)
  }
})
```

- [ ] **Step 9: Add badge to the Dashboard nav item**

In the nav button template, find the line `<span v-if="item.comingSoon" class="nav-badge">Em breve</span>` and add a sibling badge for the dashboard alert:

```html
<span v-if="item.comingSoon" class="nav-badge">Em breve</span>
<span
  v-else-if="item.name === 'dashboard' && upcomingCount > 0"
  class="nav-badge nav-badge--alert"
>{{ upcomingCount }}</span>
```

- [ ] **Step 10: Add CSS for the alert badge variant**

Append to `<style scoped>`:

```css
.nav-badge--alert {
  background: var(--color-warning-light);
  color: var(--color-warning);
  border-color: var(--color-warning);
}
```

- [ ] **Step 11: Commit both files**

```bash
git add src/views/DashboardView.vue src/layouts/AppLayout.vue
git commit -m "feat(frontend): add upcoming expenses card to dashboard and badge to sidebar"
```

---

## Spec Coverage Check

| Spec requirement | Task |
|---|---|
| `is_credit` + `due_date` DB columns with index | Task 1 |
| `creditPurchase` + `dueDate` in Expense entity | Task 3 |
| `Boolean creditPurchase` (boxed) in ExpenseRequest | Task 3 |
| `boolean creditPurchase` + `LocalDate dueDate` in ExpenseResponse | Task 3 |
| Validation: creditPurchase=true requires dueDate | Task 3 |
| `GET /accounts/{accountId}/expenses/upcoming?days=7` | Task 3 |
| Repository query — upcoming credit expenses | Task 3 |
| Config `app.notifications.default-days-ahead` | Task 3 |
| Integration tests (TDD) | Task 2 + 3 |
| Frontend types in expenseService.ts | Task 4 |
| `upcoming()` service method | Task 4 |
| Checkbox + dueDate field in ExpenseFormView | Task 5 |
| `useNotifications` composable (singleton pattern) | Task 6 |
| Dashboard card (hidden when empty) | Task 7 |
| Sidebar badge (hidden when count = 0) | Task 7 |
| Expense pago desaparece do /upcoming | Task 3 (paymentDate IS NULL in query) |
| Desmarcar creditPurchase ao editar → dueDate → null | Task 5 (payload sends undefined when unchecked) |
| Multi-tenant isolation via account_id | Task 3 (validateMembership + query param) |
