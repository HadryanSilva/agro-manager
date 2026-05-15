# Employee Daily Work Design

## Context

Watermelon producers commonly pay field workers by daily rate. Workers register days throughout the week, but the producer may not receive cash or pay on a fixed weekday. The system needs to track worked days as pending labor obligations and automatically turn paid labor into financial expenses when payment is actually made.

This feature belongs to the producer/farm management context and integrates with the existing expense model only at payment time. Pending worked days remain in the labor module and do not affect realized expense totals until paid.

## Goals

- Register employees with a simple default daily rate.
- Register one worked day per employee per date.
- Allow each worked day to be linked to a farm or left as a general account-level entry.
- Track pending worked days by employee and period.
- Pay one employee for a selected period.
- Automatically create paid `SERVICO` expenses when payment is confirmed.
- Split generated expenses by farm/general destination so farm reports remain accurate.

## Non-Goals

- Payroll batches for multiple employees at once.
- Payment method tracking.
- Employee documents, address, phone, or labor contract data.
- Fractional daily rates or multiple daily entries on the same date.
- Reversing or cancelling payments in the first version.
- Editing or deleting paid work entries.

## Existing System Fit

The current expense model already supports:

- Account-scoped expenses through `account_id`.
- Optional `farm_id`, where null means a general account expense.
- `ExpenseCategory.SERVICO` for contracted services and labor.
- Payment status derived from `paymentDate`.
- Farm reports and dashboard totals based on saved expenses.

The labor module should use those existing financial rules instead of duplicating report calculations.

## Domain Model

### Employee

Represents a worker owned by an account.

Fields:

- `id`
- `account_id`
- `name`
- `daily_rate`
- `active`
- `notes`
- `created_at`
- `updated_at`

Rules:

- `name` is required.
- `daily_rate` is required and must be greater than zero.
- New employees are active by default.
- Inactive employees remain visible historically but cannot receive new work entries.

### EmployeeWorkEntry

Represents one worked daily shift.

Fields:

- `id`
- `account_id`
- `employee_id`
- `farm_id`, nullable
- `work_date`
- `daily_rate`
- `payment_id`, nullable
- `notes`
- `created_at`
- `updated_at`

Rules:

- One employee can have only one work entry per `work_date`.
- `farm_id` is optional. Null means the work was general to the account.
- `daily_rate` is copied from the employee default when not provided.
- `daily_rate` can be overridden for a specific day.
- Paid entries have `payment_id` filled.
- Paid entries cannot be updated or deleted in the first version.

Recommended uniqueness:

- Unique key on `(employee_id, work_date)`.

### EmployeePayment

Represents a payment made to one employee for a selected period.

Fields:

- `id`
- `account_id`
- `employee_id`
- `period_start`
- `period_end`
- `payment_date`
- `total_amount`
- `notes`
- `created_at`
- `updated_at`

Rules:

- Payment is always for one employee.
- `period_end` cannot be before `period_start`.
- Payment must include at least one pending work entry in the period.
- Payment is transactional with generated expenses and work-entry updates.

### EmployeePaymentExpense

Join table linking payments to the expenses generated from them.

Fields:

- `payment_id`
- `expense_id`

Rules:

- One payment may create multiple expenses.
- Each generated expense belongs to exactly one employee payment.

## Work Entry Flow

1. User creates or selects an employee.
2. User registers a worked day with:
   - employee;
   - date;
   - optional farm;
   - optional daily rate override;
   - optional notes.
3. Backend validates account membership and ownership.
4. Backend copies the employee default daily rate if no override is provided.
5. Entry remains pending because `payment_id` is null.

Duplicate entries for the same employee and date are rejected.

## Payment Flow

1. User selects employee, period start, period end, payment date, and optional notes.
2. Backend finds all pending entries for that employee and period.
3. If none are found, backend rejects the request with a business error.
4. Backend creates an `EmployeePayment` with the total pending amount.
5. Backend groups entries by destination:
   - one group per farm;
   - one general group for entries with null `farm_id`.
6. Backend creates one paid `Expense` per destination group.
7. Backend links generated expenses through `EmployeePaymentExpense`.
8. Backend sets `payment_id` on each paid work entry.
9. Transaction commits only if all steps succeed.

## Generated Expense Rules

Each generated expense uses:

- `category`: `SERVICO`
- `description`: `Diarias de {employeeName} - {periodStart} a {periodEnd}`
- `value`: sum of daily rates in the destination group
- `competenceDate`: `periodEnd`
- `paymentDate`: payment date provided by the user
- `farmId`: destination farm for farm groups; null for general group
- `notes`: payment notes plus a compact summary of included work dates

Generated expenses enter existing dashboards, transactions, and farm reports naturally because they are regular paid expenses.

Pending work entries do not create expenses and therefore do not affect realized financial totals.

## API Design

### Employees

`POST /accounts/{accountId}/employees`

Creates an employee.

Request:

- `name`
- `dailyRate`
- `notes`

`GET /accounts/{accountId}/employees`

Lists employees. Should support filtering by `active` when useful.

`GET /accounts/{accountId}/employees/{employeeId}`

Returns one employee.

`PUT /accounts/{accountId}/employees/{employeeId}`

Updates name, default daily rate, notes, and active flag if the implementation keeps one endpoint for full updates.

`PATCH /accounts/{accountId}/employees/{employeeId}/deactivate`

Marks employee inactive.

`PATCH /accounts/{accountId}/employees/{employeeId}/activate`

Marks employee active again.

### Work Entries

`POST /accounts/{accountId}/employee-work-entries`

Creates a pending work entry.

Request:

- `employeeId`
- `farmId`, optional
- `workDate`
- `dailyRate`, optional
- `notes`, optional

`GET /accounts/{accountId}/employee-work-entries`

Lists entries with pagination.

Filters:

- `employeeId`
- `farmId`
- `startDate`
- `endDate`
- `paid`

`GET /accounts/{accountId}/employee-work-entries/{entryId}`

Returns one entry.

`PUT /accounts/{accountId}/employee-work-entries/{entryId}`

Updates an unpaid entry.

`DELETE /accounts/{accountId}/employee-work-entries/{entryId}`

Deletes an unpaid entry.

### Payments

`POST /accounts/{accountId}/employee-payments`

Pays one employee for one period.

Request:

- `employeeId`
- `periodStart`
- `periodEnd`
- `paymentDate`
- `notes`, optional

Response should include:

- payment id;
- employee id and name;
- period;
- payment date;
- total amount;
- number of paid entries;
- generated expense ids grouped by farm/general destination.

`GET /accounts/{accountId}/employee-payments`

Lists payments with pagination.

Filters:

- `employeeId`
- `startDate`
- `endDate`

`GET /accounts/{accountId}/employee-payments/{paymentId}`

Returns one payment, including generated expense references.

## Validation And Error Handling

- Account membership is required for every operation.
- Employee must belong to the account.
- Farm, when informed, must belong to the same account.
- Inactive employee cannot receive new work entries.
- Daily rate must be greater than zero.
- Work date is required.
- Period start, period end, and payment date are required.
- Period end cannot be before period start.
- Duplicate employee/date work entries are rejected.
- Paid work entries cannot be edited or deleted.
- Payment without pending entries is rejected.
- Payment must run in one database transaction.

Expected error style should follow the existing `BusinessException` and `ResourceNotFoundException` conventions.

## Reporting And Financial Behavior

Before payment:

- Work entries appear as pending labor obligations in the labor module.
- No expense exists yet.
- Dashboard and farm reports are unchanged.

After payment:

- Work entries are linked to the payment.
- Generated expenses appear as paid service expenses.
- Farm-linked work affects the correct farm report.
- General work affects account-level transactions and dashboard totals as a general expense.

## Testing Plan

Integration tests should cover:

- Creating, listing, updating, activating, and deactivating employees.
- Rejecting new work entry for inactive employee.
- Creating a general work entry.
- Creating a farm-linked work entry.
- Rejecting duplicate work entry for the same employee and date.
- Listing pending entries by employee and period.
- Paying a period with one farm destination and creating one paid expense.
- Paying a mixed period and creating separate expenses per farm/general destination.
- Linking paid entries to the payment.
- Linking generated expenses to the payment.
- Rejecting payment with no pending entries.
- Blocking update/delete of paid entries.
- Isolating data across accounts.

Repository or service tests can cover lower-level grouping and total calculation if the payment service becomes complex.

## Implementation Notes

- The payment service should depend on the existing expense creation behavior or shared expense-building logic to avoid diverging financial rules.
- Generated expenses should be distinguishable through `EmployeePaymentExpense`, not by parsing descriptions.
- The first version should avoid payment cancellation to keep accounting behavior explicit. A future cancellation feature should define whether generated expenses are deleted, reversed, or marked with a reversal entry.
