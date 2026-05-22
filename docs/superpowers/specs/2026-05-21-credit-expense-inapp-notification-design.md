# Design: Compra no Prazo + Notificação In-App

**Data:** 2026-05-21
**Status:** Aprovado
**Spec:** 1 de 2 (Spec 2 cobre e-mail + configuração por conta)

---

## Problema

O modelo de despesas atual não distingue despesas já pagas de despesas a prazo com vencimento futuro. Um produtor que compra insumos a prazo não tem como registrar o vencimento da dívida nem receber alertas antes de vencer.

---

## Solução

Adicionar flag `is_credit` e campo `due_date` na entidade `Expense`. Quando marcada como compra no prazo, a despesa recebe uma data de vencimento. Um endpoint dedicado retorna despesas próximas do vencimento, exibidas como card no dashboard e badge no header.

---

## Banco de Dados

Migration `V26__add_credit_fields_to_expenses.sql`:

```sql
ALTER TABLE expenses
  ADD COLUMN is_credit BOOLEAN NOT NULL DEFAULT FALSE,
  ADD COLUMN due_date  DATE;

CREATE INDEX idx_expenses_upcoming
  ON expenses (account_id, due_date)
  WHERE is_credit = TRUE AND payment_date IS NULL;
```

`DEFAULT FALSE` garante retrocompatibilidade. `due_date` nullable no banco — a obrigatoriedade quando `is_credit = true` é validada no service.

---

## Backend (Java)

### Expense.java

Dois novos campos após `paymentDate`:

```java
@Column(name = "is_credit", nullable = false)
@Builder.Default
private boolean isCredit = false;

@Column(name = "due_date")
private LocalDate dueDate;
```

### ExpenseRequest.java

```java
Boolean isCredit,   // boxed — pode ausente em payloads legados
LocalDate dueDate,
```

### ExpenseResponse.java

```java
boolean isCredit,
LocalDate dueDate,
```

Mapeado em `from()`:
```java
expense.isIsCredit(),   // Lombok gera isIsCredit() para boolean isCredit
expense.getDueDate(),
```

> **Nota sobre Lombok:** campo `boolean isCredit` gera getter `isIsCredit()`. Alternativa: usar `creditPurchase` como nome do campo para gerar `isCreditPurchase()` mais legível. **Decisão: usar `creditPurchase` no Java para evitar getter duplicado `isIs`.**

### Validação no Service

Em `ExpenseServiceImpl.create()` e `update()`:
```java
if (Boolean.TRUE.equals(request.creditPurchase()) && request.dueDate() == null) {
    throw new BusinessException("Data de vencimento obrigatória para compra no prazo", HttpStatus.BAD_REQUEST);
}
```

### Endpoint de vencimentos próximos

`GET /accounts/{accountId}/expenses/upcoming?days=7`

- Parâmetro `days` opcional, default = 7 (configurável via `app.notifications.default-days-ahead` em `application.yaml`)
- Retorna despesas com `is_credit = true`, `payment_date IS NULL`, `due_date <= today + days`, `due_date >= today` (não inclui vencidas)
- Ordenado por `due_date ASC`
- Response: `List<ExpenseResponse>` (mesmo DTO existente)

**Novo método no `ExpenseRepository`:**
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
    UUID accountId, LocalDate today, LocalDate until
);
```

**Novo controller endpoint** em `ExpenseController`:
```java
@GetMapping("/upcoming")
public ResponseEntity<?> upcoming(
    @PathVariable UUID accountId,
    @RequestParam(defaultValue = "7") int days,
    @AuthenticationPrincipal UserDetails user
) { ... }
```

---

## Frontend (Vue + TypeScript)

### expenseService.ts

Adicionar em `ExpenseRequest` e `ExpenseResponse`:
```typescript
// ExpenseRequest
creditPurchase?: boolean
dueDate?: string

// ExpenseResponse
creditPurchase: boolean
dueDate: string | null
```

Novo método no service:
```typescript
upcoming: (accountId: string, days = 7) =>
  api
    .get<{ data: ListPayload<ExpenseResponse> }>(`/accounts/${accountId}/expenses/upcoming`, { params: { days } })
    .then(normalizeListResponse<ExpenseResponse>)
```

### ExpenseFormView.vue

Na seção "Datas", após o campo `payment_date`:

```html
<!-- Compra no prazo -->
<div class="form-section">
  <label class="checkbox-label">
    <input v-model="creditPurchase" type="checkbox" class="checkbox-input" />
    <span>Compra no prazo</span>
  </label>

  <div v-if="creditPurchase" class="form-field">
    <label class="form-label">Data de vencimento *</label>
    <input v-model="dueDate" type="date" class="form-input" :required="creditPurchase" />
    <span class="form-hint">Prazo para pagamento desta compra</span>
  </div>
</div>
```

Estado:
```typescript
const creditPurchase = ref(false)
const dueDate        = ref('')
```

Payload em `handleSubmit`:
```typescript
creditPurchase: creditPurchase.value,
dueDate:        creditPurchase.value ? dueDate.value || undefined : undefined,
```

Botão "Salvar" desabilitado adicionalmente quando `creditPurchase && !dueDate`.

Carregamento em edição (`onMounted`):
```typescript
creditPurchase.value = e.creditPurchase ?? false
dueDate.value        = e.dueDate ?? ''
```

### DashboardView.vue — Card de vencimentos

Card "Vencimentos próximos" adicionado ao dashboard:
- Chamada `expenseService.upcoming(accountId)` no `onMounted`
- Se retorno vazio: card oculto (`v-if="upcomingExpenses.length > 0"`)
- Se houver resultados: lista com descrição, lavoura, valor, dias restantes
- Cada item navega para a despesa

### Navbar / Header — Badge

Badge numérico no header mostrando `upcomingExpenses.length`. Usa o mesmo estado carregado no dashboard via Pinia store ou composable `useNotifications()`. Badge oculto se contagem = 0.

**Composable `useNotifications.ts`** (novo arquivo):
```typescript
// src/composables/useNotifications.ts
export function useNotifications() {
  const upcomingExpenses = ref<ExpenseResponse[]>([])

  async function loadUpcoming(accountId: string) {
    const { data } = await expenseService.upcoming(accountId)
    upcomingExpenses.value = data.data
  }

  const upcomingCount = computed(() => upcomingExpenses.value.length)

  return { upcomingExpenses, upcomingCount, loadUpcoming }
}
```

Chamado no `AppLayout` (ou equivalente) ao montar, e reutilizado por Dashboard e Navbar.

---

## Validações e Edge Cases

- `due_date` no passado é permitido (despesa vencida) — não bloqueado, apenas não aparece no endpoint `/upcoming` (que filtra `>= today`)
- Despesa paga (`payment_date != null`) some do `/upcoming` automaticamente
- Usuário desmarca "Compra no prazo" ao editar: `dueDate` enviado como `null`, campo zerado no banco
- Endpoint `/upcoming` usa o `account_id` do path — isolamento multi-tenant garantido pelo service existente

---

## Fora de Escopo (Spec 1)

- Threshold configurável por conta → Spec 2
- E-mail de notificação → Spec 2
- Marcar notificação como lida
- Histórico de notificações
- Despesas vencidas (apenas "próximas" neste spec)
