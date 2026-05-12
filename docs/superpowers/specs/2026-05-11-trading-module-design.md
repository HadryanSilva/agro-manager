# Trading Module Design

**Date:** 2026-05-11  
**Project:** agro-manager  
**Status:** Approved

## Context

The same producer who manages their own farms (lavouras) also buys watermelon production from other producers for resale. This trading module lives inside the same `Account` as the farming module — no separate account needed.

Product is always watermelon. Purchase is always a physical lot (spot), payment may be deferred. Each purchase maps to exactly one sale (1:1). No partial inventory splitting.

---

## Architecture

New package `domain/trading` inside the existing Spring Boot project. Follows the established pattern: one sub-package per aggregate, each containing entity + repository + service interface + serviceImpl + controller + DTOs.

```
domain/
  trading/
    supplier/
      Supplier.java
      SupplierRepository.java
      SupplierService.java
      SupplierServiceImpl.java
      SupplierController.java
      SupplierRequest.java
      SupplierResponse.java
    buyer/
      Buyer.java
      BuyerRepository.java
      BuyerService.java
      BuyerServiceImpl.java
      BuyerController.java
      BuyerRequest.java
      BuyerResponse.java
    purchase/
      PurchaseLot.java
      PurchaseLotRepository.java
      PurchaseLotService.java
      PurchaseLotServiceImpl.java
      PurchaseLotController.java
      PurchaseLotRequest.java
      PurchaseLotResponse.java
    sale/
      Sale.java
      SaleRepository.java
      SaleService.java
      SaleServiceImpl.java
      SaleController.java
      SaleRequest.java
      SaleResponse.java
    report/
      TradingReportController.java
      TradingReportService.java
      TradingReportResponse.java
```

All entities carry `account_id` FK for multi-tenant isolation, identical to the farming module.

---

## Data Model

### `suppliers`

| Column | Type | Notes |
|--------|------|-------|
| id | UUID PK | gen_random_uuid() |
| account_id | UUID FK | NOT NULL, CASCADE DELETE |
| name | VARCHAR(200) | NOT NULL |
| cpf_cnpj | VARCHAR(18) | nullable, UNIQUE per account |
| phone | VARCHAR(20) | nullable |
| email | VARCHAR(200) | nullable |
| notes | TEXT | nullable |
| created_at | TIMESTAMP | NOT NULL |
| updated_at | TIMESTAMP | NOT NULL |

Index: `(account_id, cpf_cnpj)` for uniqueness check.

### `buyers`

Same structure as `suppliers`.

### `purchase_lots`

| Column | Type | Notes |
|--------|------|-------|
| id | UUID PK | |
| account_id | UUID FK | NOT NULL |
| supplier_id | UUID FK | NOT NULL, RESTRICT DELETE |
| quantity_kg | DECIMAL(12,3) | NOT NULL |
| price_per_kg | DECIMAL(12,4) | NOT NULL |
| purchase_date | DATE | NOT NULL |
| payment_date | DATE | nullable — null means unpaid/pending |
| notes | TEXT | nullable |
| created_at | TIMESTAMP | NOT NULL |
| updated_at | TIMESTAMP | NOT NULL |

Computed (@Transient):
- `totalPrice()` = `quantity_kg × price_per_kg`
- `isPaid()` = `paymentDate != null`

### `sales`

| Column | Type | Notes |
|--------|------|-------|
| id | UUID PK | |
| account_id | UUID FK | NOT NULL |
| purchase_lot_id | UUID FK | NOT NULL, UNIQUE, RESTRICT DELETE |
| buyer_id | UUID FK | NOT NULL, RESTRICT DELETE |
| quantity_kg | DECIMAL(12,3) | NOT NULL |
| price_per_kg | DECIMAL(12,4) | NOT NULL |
| sale_date | DATE | NOT NULL |
| receipt_date | DATE | nullable — null means payment not yet received |
| notes | TEXT | nullable |
| created_at | TIMESTAMP | NOT NULL |
| updated_at | TIMESTAMP | NOT NULL |

Computed (@Transient):
- `totalRevenue()` = `quantity_kg × price_per_kg`
- `profit` not a @Transient on `Sale` — computed in `TradingReportService` by joining with `PurchaseLot`, exposed only via report endpoints
- `isReceived()` = `receiptDate != null`

The UNIQUE constraint on `purchase_lot_id` enforces the 1:1 rule at the database level.

---

## API Endpoints

All endpoints: `/api/trading/...`, JWT-protected, `account_id` derived from `UserPrincipal` (never from request body).

### Suppliers — `/api/trading/suppliers`

| Method | Path | Action |
|--------|------|--------|
| POST | `/` | create supplier |
| GET | `/` | list (paginated) |
| GET | `/{id}` | get by ID |
| PUT | `/{id}` | update |
| DELETE | `/{id}` | delete (blocked if has purchase lots) |

### Buyers — `/api/trading/buyers`

Same structure as Suppliers (blocked if has sales).

### Purchase Lots — `/api/trading/purchases`

| Method | Path | Action |
|--------|------|--------|
| POST | `/` | register purchase |
| GET | `/` | list (filters: supplierId, dateRange, paid status) |
| GET | `/{id}` | get by ID |
| PUT | `/{id}` | update (blocked if sale exists) |
| DELETE | `/{id}` | delete (blocked if sale exists) |
| PATCH | `/{id}/payment` | register payment (sets `paymentDate`) |

### Sales — `/api/trading/sales`

| Method | Path | Action |
|--------|------|--------|
| POST | `/` | register sale (validates purchaseLot has no existing sale) |
| GET | `/` | list (filters: buyerId, dateRange, received status) |
| GET | `/{id}` | get by ID |
| PUT | `/{id}` | update |
| DELETE | `/{id}` | delete |
| PATCH | `/{id}/receipt` | register receipt (sets `receiptDate`) |

### Trading Report — `/api/trading/report`

| Method | Path | Action |
|--------|------|--------|
| GET | `/summary` | total purchased, total sold, total profit, pending payments, pending receivables |
| GET | `/by-supplier` | breakdown by supplier |
| GET | `/by-buyer` | breakdown by buyer |

All list endpoints return `PageResponse<T>` (existing shared DTO).

---

## Business Rules

### Integrity
- `PurchaseLot` cannot be edited or deleted if a `Sale` is linked → `BusinessException` (409)
- `Supplier` cannot be deleted if it has any `PurchaseLot` → `BusinessException` (409)
- `Buyer` cannot be deleted if it has any `Sale` → `BusinessException` (409)
- `cpf_cnpj` must be unique per `Account` within each table independently (`suppliers` and `buyers` are separate scopes — same CPF/CNPJ can appear in both) → validated in service before persist
- `Sale.purchaseLotId` uniqueness validated in service before persist (in addition to DB constraint)

### Financial
- `paymentDate` and `receiptDate` are append-only: once set they cannot be cleared → service rejects null value on PATCH
- Profit may be negative (loss on resale) — no validation, only reported
- `Sale.quantity_kg` does not need to equal `PurchaseLot.quantity_kg` (losses allowed)

### Tenant isolation
- All queries filter by `account_id` derived from authenticated `UserPrincipal`
- No cross-account data access possible

---

## Error Handling

Reuses existing `GlobalExceptionHandler`, `ResourceNotFoundException`, and `BusinessException`.

| Scenario | Exception | HTTP |
|----------|-----------|------|
| Entity not found | `ResourceNotFoundException` | 404 |
| Purchase lot already has a sale | `BusinessException` | 409 |
| Supplier/Buyer in use | `BusinessException` | 409 |
| Duplicate CPF/CNPJ in account | `BusinessException` | 409 |
| Attempting to clear payment/receipt date | `BusinessException` | 422 |

---

## Testing

Integration tests following the existing `*IntegrationTest.java` pattern (Spring Boot test slice, real DB via Testcontainers or H2).

Coverage targets:
- Full CRUD for Supplier, Buyer, PurchaseLot, Sale
- Deletion blocked when dependencies exist
- PATCH payment/receipt sets date correctly
- PATCH cannot clear an already-set date
- Sale creation blocked when purchase lot already has a sale
- Report summary returns correct totals

---

## Migrations

New Flyway migrations (continuing current sequence from V11):

- `V12__create_suppliers_table.sql`
- `V13__create_buyers_table.sql`
- `V14__create_purchase_lots_table.sql`
- `V15__create_sales_table.sql`
