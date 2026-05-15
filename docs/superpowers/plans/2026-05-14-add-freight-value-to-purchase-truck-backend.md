# Add Freight Value to Purchase Truck — Backend Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Persist and expose `freightValue` on `PurchaseTruck` so the frontend plan (`2026-05-14-add-freight-value-to-purchase-truck.md`) has a working API to read from and write to.

**Architecture:** Additive change only — new nullable column in the DB, new optional field in the request record, new field in the response record, and two-line update in the service builder calls. No existing behaviour changes.

**Tech Stack:** Spring Boot 3, JPA/Hibernate, Flyway, PostgreSQL, Lombok, Java records

---

## File Map

| File | Change |
|---|---|
| `src/main/resources/db/migration/V17__add_freight_value_to_purchase_trucks.sql` | Create — add nullable `freight_value` column |
| `src/main/java/.../trading/PurchaseTruck.java` | Add `freightValue` `@Column` field |
| `src/main/java/.../trading/PurchaseTruckRequest.java` | Add optional `freightValue` field to record |
| `src/main/java/.../trading/PurchaseTruckResponse.java` | Add `freightValue` to record + `from()` factory |
| `src/main/java/.../trading/PurchaseLotService.java` | Pass `freightValue` in truck builder (createLot + updateLot) |

> All paths under `src/main/java/` expand to `br/com/hadryan/agro/manager/domain/trading/`.

---

## Task 1: Database migration

**Files:**
- Create: `src/main/resources/db/migration/V17__add_freight_value_to_purchase_trucks.sql`

- [ ] **Step 1: Write the migration script**

```sql
-- Adds an optional freight cost per truck on purchase lots.
-- NULL means no freight was recorded (backwards-compatible with existing rows).
ALTER TABLE purchase_trucks
    ADD COLUMN freight_value DECIMAL(12, 2);
```

- [ ] **Step 2: Verify Flyway picks it up**

Start the application (or run `./mvnw flyway:migrate`) and confirm the migration runs without error.

Expected log line: `Successfully applied 1 migration to schema "public" (execution time ...)`

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/db/migration/V17__add_freight_value_to_purchase_trucks.sql
git commit -m "feat: add freight_value column to purchase_trucks via V17 migration"
```

---

## Task 2: Extend the `PurchaseTruck` entity

**Files:**
- Modify: `src/main/java/br/com/hadryan/agro/manager/domain/trading/PurchaseTruck.java`

- [ ] **Step 1: Add the `freightValue` field**

Current file (relevant section):

```java
@Column(name = "quantity_kg", nullable = false, precision = 12, scale = 2)
private BigDecimal quantityKg;

@Column(columnDefinition = "TEXT")
private String notes;
```

Change to:

```java
@Column(name = "quantity_kg", nullable = false, precision = 12, scale = 2)
private BigDecimal quantityKg;

@Column(name = "freight_value", precision = 12, scale = 2)
private BigDecimal freightValue;

@Column(columnDefinition = "TEXT")
private String notes;
```

The field is nullable (`nullable` defaults to `true`) — existing rows will have `NULL`, which maps to Java `null`.

- [ ] **Step 2: Compile**

```bash
./mvnw compile -q
```

Expected: `BUILD SUCCESS`

- [ ] **Step 3: Commit**

```bash
git add src/main/java/br/com/hadryan/agro/manager/domain/trading/PurchaseTruck.java
git commit -m "feat: add freightValue field to PurchaseTruck entity"
```

---

## Task 3: Extend the request record

**Files:**
- Modify: `src/main/java/br/com/hadryan/agro/manager/domain/trading/PurchaseTruckRequest.java`

- [ ] **Step 1: Add `freightValue` to the record**

Current file:

```java
public record PurchaseTruckRequest(
        @NotBlank(message = "Placa do caminhão é obrigatória")
        @Size(max = 10, message = "Placa deve ter no máximo 10 caracteres")
        String truckPlate,

        @NotNull(message = "Quantidade em Kg é obrigatória")
        @DecimalMin(value = "0.01", message = "Quantidade deve ser maior que zero")
        BigDecimal quantityKg,

        String notes
) {
}
```

Change to:

```java
public record PurchaseTruckRequest(
        @NotBlank(message = "Placa do caminhão é obrigatória")
        @Size(max = 10, message = "Placa deve ter no máximo 10 caracteres")
        String truckPlate,

        @NotNull(message = "Quantidade em Kg é obrigatória")
        @DecimalMin(value = "0.01", message = "Quantidade deve ser maior que zero")
        BigDecimal quantityKg,

        @DecimalMin(value = "0.00", inclusive = true, message = "Valor do frete não pode ser negativo")
        BigDecimal freightValue,

        String notes
) {
}
```

`freightValue` has no `@NotNull` — it is optional. The frontend sends `undefined` (omitted) when there is no freight, which Jackson deserialises to `null`.

- [ ] **Step 2: Compile**

```bash
./mvnw compile -q
```

Expected: `BUILD SUCCESS`

- [ ] **Step 3: Commit**

```bash
git add src/main/java/br/com/hadryan/agro/manager/domain/trading/PurchaseTruckRequest.java
git commit -m "feat: add optional freightValue to PurchaseTruckRequest"
```

---

## Task 4: Extend the response record

**Files:**
- Modify: `src/main/java/br/com/hadryan/agro/manager/domain/trading/PurchaseTruckResponse.java`

- [ ] **Step 1: Add `freightValue` to the record and factory**

Current file:

```java
public record PurchaseTruckResponse(
        UUID id,
        String truckPlate,
        BigDecimal quantityKg,
        String notes
) {
    public static PurchaseTruckResponse from(PurchaseTruck t) {
        return new PurchaseTruckResponse(t.getId(), t.getTruckPlate(), t.getQuantityKg(), t.getNotes());
    }
}
```

Change to:

```java
public record PurchaseTruckResponse(
        UUID id,
        String truckPlate,
        BigDecimal quantityKg,
        BigDecimal freightValue,
        String notes
) {
    public static PurchaseTruckResponse from(PurchaseTruck t) {
        return new PurchaseTruckResponse(
                t.getId(),
                t.getTruckPlate(),
                t.getQuantityKg(),
                t.getFreightValue(),
                t.getNotes()
        );
    }
}
```

`freightValue` will be `null` in JSON for trucks that have no freight — the frontend renders `'—'` in that case.

- [ ] **Step 2: Compile**

```bash
./mvnw compile -q
```

Expected: `BUILD SUCCESS`

- [ ] **Step 3: Commit**

```bash
git add src/main/java/br/com/hadryan/agro/manager/domain/trading/PurchaseTruckResponse.java
git commit -m "feat: expose freightValue in PurchaseTruckResponse"
```

---

## Task 5: Wire `freightValue` through the service

**Files:**
- Modify: `src/main/java/br/com/hadryan/agro/manager/domain/trading/PurchaseLotService.java:57-65` (createLot)
- Modify: `src/main/java/br/com/hadryan/agro/manager/domain/trading/PurchaseLotService.java:145-153` (updateLot)

Both `createLot` and `updateLot` build `PurchaseTruck` entities from request data. Neither currently passes `freightValue`.

- [ ] **Step 1: Fix `createLot` truck builder**

Current block (lines ~57–65):

```java
request.trucks().forEach(t -> {
    PurchaseTruck truck = PurchaseTruck.builder()
            .lot(lot)
            .truckPlate(t.truckPlate().toUpperCase().trim())
            .quantityKg(t.quantityKg())
            .notes(t.notes())
            .build();
    lot.getTrucks().add(truck);
});
```

Change to:

```java
request.trucks().forEach(t -> {
    PurchaseTruck truck = PurchaseTruck.builder()
            .lot(lot)
            .truckPlate(t.truckPlate().toUpperCase().trim())
            .quantityKg(t.quantityKg())
            .freightValue(t.freightValue())
            .notes(t.notes())
            .build();
    lot.getTrucks().add(truck);
});
```

- [ ] **Step 2: Fix `updateLot` truck builder**

Current block (lines ~145–153):

```java
lot.getTrucks().clear();
request.trucks().forEach(t -> {
    PurchaseTruck truck = PurchaseTruck.builder()
            .lot(lot)
            .truckPlate(t.truckPlate().toUpperCase().trim())
            .quantityKg(t.quantityKg())
            .notes(t.notes())
            .build();
    lot.getTrucks().add(truck);
});
```

Change to:

```java
lot.getTrucks().clear();
request.trucks().forEach(t -> {
    PurchaseTruck truck = PurchaseTruck.builder()
            .lot(lot)
            .truckPlate(t.truckPlate().toUpperCase().trim())
            .quantityKg(t.quantityKg())
            .freightValue(t.freightValue())
            .notes(t.notes())
            .build();
    lot.getTrucks().add(truck);
});
```

- [ ] **Step 3: Compile**

```bash
./mvnw compile -q
```

Expected: `BUILD SUCCESS`

- [ ] **Step 4: Run existing tests to verify no regression**

```bash
./mvnw test -q
```

Expected: all tests pass (or same failures as baseline — do not introduce new failures).

- [ ] **Step 5: Commit**

```bash
git add src/main/java/br/com/hadryan/agro/manager/domain/trading/PurchaseLotService.java
git commit -m "feat: wire freightValue through createLot and updateLot in PurchaseLotService"
```

---

## Verification checklist

After all tasks are complete, do a quick end-to-end smoke test:

- [ ] POST `/api/accounts/{id}/purchase-lots` with a truck payload that includes `"freightValue": 150.00` — response should echo `freightValue: 150.00`.
- [ ] GET `/api/accounts/{id}/purchase-lots/{lotId}` — `purchaseTrucks[n].freightValue` appears in the response.
- [ ] PUT same lot with `freightValue` omitted from a truck — that truck's `freightValue` should be `null` in the response.
- [ ] Existing rows (created before the migration) return `freightValue: null` — no 500 errors.
