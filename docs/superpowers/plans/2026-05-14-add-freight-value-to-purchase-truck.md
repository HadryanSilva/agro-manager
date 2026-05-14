# Add Freight Value to Purchase Truck Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a "Valor do Frete (R$)" field to each truck row in the purchase lot registration form, propagating the value through types, payload, and the detail view.

**Architecture:** The change touches three layers in order — service types (`tradingService.ts`), the form view (`PurchaseLotFormView.vue`), and the detail view (`PurchaseLotDetailView.vue`). No new files are needed; all edits are additive.

**Tech Stack:** Vue 3 (Composition API, `<script setup>`), TypeScript, scoped CSS

---

## File Map

| File | Change |
|---|---|
| `src/services/tradingService.ts` | Add `freightValue` to `PurchaseTruckRequest` and `PurchaseTruckResponse` |
| `src/views/trader/PurchaseLotFormView.vue` | Add `freightValue` to state, `makeTruckRow`, `loadLot`, `submit` payload, template field, summary, and grid CSS |
| `src/views/trader/PurchaseLotDetailView.vue` | Add `freightValue` column to the "Caminhões de Recebimento" table |

---

## Task 1: Extend service types with `freightValue`

**Files:**
- Modify: `src/services/tradingService.ts:34-45`

- [ ] **Step 1: Add `freightValue` to `PurchaseTruckRequest`**

In `src/services/tradingService.ts`, change:

```ts
export interface PurchaseTruckRequest {
  truckPlate: string
  quantityKg: number
  notes?: string
}
```

to:

```ts
export interface PurchaseTruckRequest {
  truckPlate: string
  quantityKg: number
  freightValue?: number
  notes?: string
}
```

- [ ] **Step 2: Add `freightValue` to `PurchaseTruckResponse`**

In `src/services/tradingService.ts`, change:

```ts
export interface PurchaseTruckResponse {
  id: string
  truckPlate: string
  quantityKg: number
  notes: string | null
}
```

to:

```ts
export interface PurchaseTruckResponse {
  id: string
  truckPlate: string
  quantityKg: number
  freightValue: number | null
  notes: string | null
}
```

- [ ] **Step 3: Verify TypeScript compiles with no new errors**

Run: `npx tsc --noEmit`
Expected: no errors related to the changed interfaces (existing errors are pre-existing)

- [ ] **Step 4: Commit**

```bash
git add src/services/tradingService.ts
git commit -m "feat: add freightValue to PurchaseTruckRequest and PurchaseTruckResponse types"
```

---

## Task 2: Add freight value field to the purchase lot form

**Files:**
- Modify: `src/views/trader/PurchaseLotFormView.vue`

### Script changes

- [ ] **Step 1: Add `freightValue` to `makeTruckRow`**

In `PurchaseLotFormView.vue`, change:

```ts
const makeTruckRow = (): TruckRow => ({ truckPlate: '', quantityKg: 0, notes: '', _key: ++_truckKeyCounter })
```

to:

```ts
const makeTruckRow = (): TruckRow => ({ truckPlate: '', quantityKg: 0, freightValue: 0, notes: '', _key: ++_truckKeyCounter })
```

- [ ] **Step 2: Restore `freightValue` when loading a lot for editing**

In `loadLot()`, change:

```ts
trucks.value = lot.purchaseTrucks.map(t => ({
  truckPlate: t.truckPlate,
  quantityKg: t.quantityKg,
  notes: t.notes ?? '',
  _key: ++_truckKeyCounter,
}))
```

to:

```ts
trucks.value = lot.purchaseTrucks.map(t => ({
  truckPlate: t.truckPlate,
  quantityKg: t.quantityKg,
  freightValue: t.freightValue ?? 0,
  notes: t.notes ?? '',
  _key: ++_truckKeyCounter,
}))
```

- [ ] **Step 3: Add total freight computed property**

After the existing computed properties `totalKg` and `totalCost`, add:

```ts
const totalFreight = computed(() => trucks.value.reduce((sum, t) => sum + (Number(t.freightValue) || 0), 0))
```

- [ ] **Step 4: Include `freightValue` in the submit payload**

In `submit()`, change:

```ts
trucks: trucks.value.map(t => ({
  truckPlate: t.truckPlate.toUpperCase().trim(),
  quantityKg: Number(t.quantityKg),
  notes: t.notes || undefined,
}))
```

to:

```ts
trucks: trucks.value.map(t => ({
  truckPlate: t.truckPlate.toUpperCase().trim(),
  quantityKg: Number(t.quantityKg),
  freightValue: Number(t.freightValue) > 0 ? Number(t.freightValue) : undefined,
  notes: t.notes || undefined,
}))
```

### Template changes

- [ ] **Step 5: Add "Frete (R$)" input field inside each truck row**

In the template, the `truck-row__fields` div currently has three `form-group` children (Placa, Peso, Obs.). Add a new `form-group` for freight between "Peso" and "Obs.":

```html
<div class="truck-row__fields">
  <div class="form-group">
    <label class="form-label">Placa *</label>
    <input
      v-model="truck.truckPlate"
      type="text"
      class="form-input form-input--plate"
      placeholder="ABC1D23"
      maxlength="10"
      @input="truck.truckPlate = truck.truckPlate.toUpperCase()"
    />
  </div>
  <div class="form-group">
    <label class="form-label">Peso (Kg) *</label>
    <input v-model="truck.quantityKg" type="number" step="0.01" min="0" class="form-input" placeholder="0,00" />
  </div>
  <div class="form-group">
    <label class="form-label">Frete (R$)</label>
    <input v-model="truck.freightValue" type="number" step="0.01" min="0" class="form-input" placeholder="0,00" />
  </div>
  <div class="form-group form-group--notes">
    <label class="form-label">Obs.</label>
    <input v-model="truck.notes" type="text" class="form-input" placeholder="Opcional" />
  </div>
</div>
```

- [ ] **Step 6: Add total freight to the summary card**

In the `summary-card`, add a new `summary-item` after "Total em Kg" and before "Custo total estimado":

```html
<div class="summary-item">
  <span class="summary-label">Total em Kg</span>
  <span class="summary-value">{{ kg(totalKg) }}</span>
</div>
<div class="summary-item">
  <span class="summary-label">Total de Frete</span>
  <span class="summary-value">{{ currency(totalFreight) }}</span>
</div>
<div class="summary-item summary-item--highlight">
  <span class="summary-label">Custo total estimado</span>
  <span class="summary-value summary-value--cost">{{ currency(totalCost) }}</span>
</div>
```

### CSS changes

- [ ] **Step 7: Update truck row grid to accommodate 4 columns**

In the scoped `<style>` block, change:

```css
.truck-row__fields {
  flex: 1;
  display: grid;
  grid-template-columns: 120px 1fr 1fr;
  gap: 0.75rem;
}
```

to:

```css
.truck-row__fields {
  flex: 1;
  display: grid;
  grid-template-columns: 120px 1fr 1fr 1fr;
  gap: 0.75rem;
}
```

- [ ] **Step 8: Fix responsive CSS for the new column count**

The `@media (max-width: 768px)` block currently overrides the grid. Update it so the 4-column layout collapses properly:

```css
@media (max-width: 768px) {
  .page-container { padding: 1.25rem 1rem; }
  .form-row { grid-template-columns: 1fr; }
  .truck-row__fields { grid-template-columns: 1fr 1fr; }
  .form-group--notes { grid-column: span 2; }
  .summary-card { flex-direction: column; gap: 0.875rem; }
  .summary-item--highlight { margin-left: 0; }
  .form-actions { flex-direction: column-reverse; }
  .form-actions .btn { width: 100%; justify-content: center; }
}
```

(No change needed here — `grid-template-columns: 1fr 1fr` already gives a 2-column collapse, and the freight field will naturally wrap. The `@media (max-width: 480px)` block already sets `1fr`, so it also remains correct.)

- [ ] **Step 9: Verify TypeScript compiles with no new errors**

Run: `npx tsc --noEmit`
Expected: no errors

- [ ] **Step 10: Commit**

```bash
git add src/views/trader/PurchaseLotFormView.vue
git commit -m "feat: add freight value field to purchase truck rows in PurchaseLotFormView"
```

---

## Task 3: Show freight value in the purchase lot detail view

**Files:**
- Modify: `src/views/trader/PurchaseLotDetailView.vue:222-234`

- [ ] **Step 1: Add "Frete" column header to the trucks table**

In `PurchaseLotDetailView.vue`, the `trucks-table__header` currently has three `<span>` children. Change it to:

```html
<div class="trucks-table__header">
  <span>Placa</span>
  <span>Quantidade</span>
  <span>Frete</span>
  <span>Obs.</span>
</div>
```

- [ ] **Step 2: Add `freightValue` cell to each truck row**

Change the `trucks-table__row` div to:

```html
<div v-for="truck in lot.purchaseTrucks" :key="truck.id" class="trucks-table__row">
  <span class="truck-plate">{{ truck.truckPlate }}</span>
  <span>{{ kg(truck.quantityKg) }}</span>
  <span>{{ truck.freightValue != null ? currency(truck.freightValue) : '—' }}</span>
  <span class="text-muted">{{ truck.notes ?? '—' }}</span>
</div>
```

- [ ] **Step 3: Update the trucks table CSS grid to 4 columns**

In the scoped `<style>` block of `PurchaseLotDetailView.vue`, change:

```css
.trucks-table__header {
  display: grid;
  grid-template-columns: 120px 1fr 1fr;
  ...
}

.trucks-table__row {
  display: grid;
  grid-template-columns: 120px 1fr 1fr;
  ...
}
```

to (update both selectors):

```css
.trucks-table__header {
  display: grid;
  grid-template-columns: 120px 1fr 1fr 1fr;
  padding: 0.5rem 0.875rem;
  background: var(--color-surface);
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--color-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.trucks-table__row {
  display: grid;
  grid-template-columns: 120px 1fr 1fr 1fr;
  padding: 0.625rem 0.875rem;
  border-top: 1px solid var(--color-border);
  font-size: 0.875rem;
  color: var(--color-text);
}
```

- [ ] **Step 4: Fix responsive hide-last-column rule**

The `@media (max-width: 768px)` block currently hides the last column (Obs.) when the grid collapses to 2 columns. With 4 columns now, update the responsive grid and hide the last two columns to keep the layout clean:

```css
@media (max-width: 768px) {
  ...
  .trucks-table__header, .trucks-table__row { grid-template-columns: 100px 1fr; }
  .trucks-table__header span:nth-child(n+3),
  .trucks-table__row span:nth-child(n+3) { display: none; }
  ...
}
```

- [ ] **Step 5: Verify TypeScript compiles with no new errors**

Run: `npx tsc --noEmit`
Expected: no errors

- [ ] **Step 6: Commit**

```bash
git add src/views/trader/PurchaseLotDetailView.vue
git commit -m "feat: show freight value column in purchase trucks table on PurchaseLotDetailView"
```

---

## Notes

- The `freightValue` field is **optional** in `PurchaseTruckRequest` — trucks without freight will omit the field from the API payload (sent as `undefined`).
- In `PurchaseTruckResponse` it is `number | null` — the detail view renders `'—'` when `null`.
- This plan assumes the backend API already accepts and returns `freightValue` on purchase trucks. If not, coordinate with the backend team before testing the full flow end-to-end.
