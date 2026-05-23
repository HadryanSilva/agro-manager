# Notification Bell + UserPreferences Design

**Date:** 2026-05-22
**Status:** Approved

---

## Goal

Add a bell icon in the topbar that opens a dropdown with upcoming credit expenses. Allow each user to configure how many days ahead they want to be notified, stored in a dedicated `UserPreferences` entity.

---

## Decisions

| Topic | Decision |
|---|---|
| Badge position | Bell icon in topbar (right side) |
| Click behavior | Dropdown inline (not a page) |
| Settings location | New "Notificações" tab in Settings |
| Preference scope | Per-user (not per-account) |
| Preference storage | Separate `UserPreferences` entity |

---

## Architecture

### Data flow

```
AppLayout.onMounted
  → userStore.fetchPreferences()         (GET /users/me/preferences)
  → watch(accountStore.selectedAccount)
  → useNotifications.loadUpcoming(accountId, daysAhead)
  → expenseService.upcoming(accountId, days)
  → GET /accounts/{id}/expenses/upcoming?days=N
```

The `NotificationDropdown` and `NotificationsSettingsView` both read/write through `userStore.preferences`.

---

## Backend

### Migration V27

```sql
CREATE TABLE user_preferences (
  id                       UUID      PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id                  UUID      NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
  notification_days_ahead  INT       NOT NULL DEFAULT 7,
  created_at               TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at               TIMESTAMP NOT NULL DEFAULT NOW()
);
```

One row per user. `UNIQUE` on `user_id` enforces the 1:1 relationship. `ON DELETE CASCADE` removes preferences when the user is deleted.

### New files

| File | Responsibility |
|---|---|
| `V27__create_user_preferences.sql` | Migration |
| `UserPreferences.java` | Entity: `userId`, `notificationDaysAhead` |
| `UserPreferencesRepository.java` | `findByUserId(UUID)` |
| `UserPreferencesRequest.java` | `record (Integer notificationDaysAhead)` |
| `UserPreferencesResponse.java` | `record (int notificationDaysAhead)` |
| `UserPreferencesService.java` | Interface: `getOrCreate`, `update` |
| `UserPreferencesServiceImpl.java` | Lazy-init: if no row exists, create with `notificationDaysAhead = 7` |
| `UserPreferencesController.java` | `GET /users/me/preferences`, `PATCH /users/me/preferences` |
| `UserPreferencesIntegrationTest.java` | Integration tests (TDD) |

### Modified files

| File | Change |
|---|---|
| `GeneralExpenseController.java` | `/upcoming` injects `UserPreferencesService`, uses `prefs.notificationDaysAhead` when `days` param is null |

### Endpoints

**`GET /users/me/preferences`**
- Auth required
- Returns `ApiResponse<UserPreferencesResponse>`
- Lazy-creates row with default 7 if not found

**`PATCH /users/me/preferences`**
- Auth required
- Body: `{ "notificationDaysAhead": 14 }`
- Validates: `1 ≤ notificationDaysAhead ≤ 90`
- Returns updated `ApiResponse<UserPreferencesResponse>`

### `/upcoming` change

```java
int d = (days != null) ? days
      : userPreferencesService.getOrCreate(principal.getId()).getNotificationDaysAhead();
```

### Integration tests (TDD — failing first)

1. `GET /users/me/preferences` on new user → returns `{ notificationDaysAhead: 7 }`
2. `PATCH /users/me/preferences` with `{ notificationDaysAhead: 14 }` → returns 14
3. `GET /accounts/{id}/expenses/upcoming` without `days` param → uses user's stored value
4. `PATCH /users/me/preferences` with value `0` → returns 400
5. `PATCH /users/me/preferences` with value `91` → returns 400

---

## Frontend

### New files

| File | Responsibility |
|---|---|
| `src/components/NotificationDropdown.vue` | Dropdown rendered by AppLayout when bell is clicked |
| `src/views/settings/NotificationsSettingsView.vue` | Form to save `notificationDaysAhead` |

### Modified files

| File | Change |
|---|---|
| `src/services/userService.ts` | Add `UserPreferencesResponse` type, `getPreferences()`, `updatePreferences()` |
| `src/stores/userStore.ts` | Add `preferences: UserPreferencesResponse \| null`, `fetchPreferences()` |
| `src/composables/useNotifications.ts` | `loadUpcoming` reads `userStore.preferences?.notificationDaysAhead ?? 7` |
| `src/layouts/AppLayout.vue` | Bell icon in topbar + badge + mounts `NotificationDropdown` |
| `src/components/SettingsTabs.vue` | Add `{ name: 'settings-notifications', label: 'Notificações' }` |
| `src/router/index.ts` | Add `settings-notifications` route → `NotificationsSettingsView` |

### NotificationDropdown.vue

- Triggered by bell click in topbar: `showDropdown = !showDropdown`
- Closes on outside click (`@click.outside` or `document` listener on mount)
- Renders `upcomingExpenses` from `useNotifications` singleton
- Each item clickable → `navigateToExpense(expense)` (same logic as dashboard card)
- Footer: "Ver no Dashboard" → `router.push({ name: 'dashboard' })`
- Empty state: "Nenhum vencimento próximo"
- Bell badge hidden when `upcomingCount === 0`

### AppLayout.vue topbar changes

```html
<!-- right side of topbar -->
<div class="topbar__actions">
  <div class="topbar__bell" @click="toggleDropdown">
    <BellIcon />
    <span v-if="upcomingCount > 0" class="topbar__bell-badge"
          :aria-label="`${upcomingCount} vencimentos próximos`">
      {{ upcomingCount }}
    </span>
    <NotificationDropdown v-if="showDropdown" @close="showDropdown = false" />
  </div>
  <ThemeToggle />
</div>
```

- Remove existing `nav-badge--alert` from sidebar dashboard item (replaced by bell)
- `userStore.fetchPreferences()` awaited in `onMounted` **before** `loadUpcoming` is called — ensures the user's real preference (not the fallback 7) is used on first load
- Replace `watch({ immediate: true })` with explicit `loadUpcoming` call after `await fetchPreferences()` in `onMounted`; keep the watch (without `immediate`) to handle account switches

### NotificationsSettingsView.vue

- Form with single field: `notificationDaysAhead` (number input, min=1, max=90)
- Loads current value from `userStore.preferences`
- On save: calls `userService.updatePreferences()` → updates `userStore.preferences`
- Success/error inline feedback (same pattern as ProfileSettingsView)

### userStore additions

```typescript
preferences: null as UserPreferencesResponse | null,

async fetchPreferences() {
  const { data } = await userService.getPreferences()
  this.preferences = data.data
}
```

### useNotifications change

```typescript
async function loadUpcoming(accountId: string) {
  upcomingExpenses.value = []
  const days = userStore.preferences?.notificationDaysAhead ?? 7
  try {
    const { data } = await expenseService.upcoming(accountId, days)
    upcomingExpenses.value = data.data ?? []
  } catch {
    // silent
  }
}
```

---

## Out of scope

- Push / email notifications
- "Mark as read" / dismiss individual notifications
- Multiple notification types (only expense due date for now)
- Notification history / inbox page

---

## Testing strategy

**Backend:** Integration tests with Testcontainers — TDD cycle (failing tests committed first).

**Frontend:** Manual verification — bell icon visible, badge count correct, dropdown opens/closes, settings tab saves and reloads value, `/upcoming` uses updated preference.
