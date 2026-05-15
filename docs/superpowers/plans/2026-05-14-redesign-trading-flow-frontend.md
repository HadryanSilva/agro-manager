# Redesign do Fluxo de Compra/Venda — Frontend Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the PurchaseLot/LotSale UI with ClientOrder/OrderSupplierLeg/OrderTruck + TradingClient, matching the redesigned backend API.

**Architecture:** Additive-then-delete — build all new views first, wire router/nav, then delete old views. Each task compiles cleanly so you get early feedback. All work is in `C:\Workspace\Frontend\agro-manager-web`.

**Tech Stack:** Vue 3 Composition API (`<script setup lang="ts">`), TypeScript, Vue Router, Pinia, Axios via `tradingService.ts`

---

## File Map

| File | Change |
|---|---|
| `src/services/tradingService.ts` | Replace — new types + API methods |
| `src/views/trader/TraderDashboardView.vue` | Modify — new field names + routes |
| `src/views/trader/TradingClientsView.vue` | Create — client CRUD modal |
| `src/views/trader/ClientOrdersView.vue` | Create — paginated order list |
| `src/views/trader/ClientOrderFormView.vue` | Create — nested 3-level form + live summary |
| `src/views/trader/ClientOrderDetailView.vue` | Create — read-only order detail |
| `src/router/index.ts` | Modify — replace lot routes, add client/order routes |
| `src/layouts/TraderLayout.vue` | Modify — update navItems |
| `src/views/trader/PurchaseLotsView.vue` | Delete |
| `src/views/trader/PurchaseLotFormView.vue` | Delete |
| `src/views/trader/PurchaseLotDetailView.vue` | Delete |

---

## Task 1: Replace tradingService.ts

**Files:**
- Modify: `src/services/tradingService.ts`

- [ ] **Step 1: Replace the entire file**

```typescript
import api from './api'
import {
  normalizeListResponse,
  normalizeObjectResponse,
  normalizePagePayload,
  type ListPayload,
  type ObjectPayload,
} from './responseUtils'

// ── Status ────────────────────────────────────────────────────────────────────

export type ClientOrderStatus = 'OPEN' | 'CLOSED'

// ── Fornecedores ──────────────────────────────────────────────────────────────

export interface TradingSupplierRequest {
  name: string
  phone?: string
  city?: string
  notes?: string
}

export interface TradingSupplierResponse {
  id: string
  name: string
  phone: string | null
  city: string | null
  notes: string | null
  createdAt: string
}

// ── Clientes ──────────────────────────────────────────────────────────────────

export interface TradingClientRequest {
  name: string
  phone: string
  city?: string
  notes?: string
}

export interface TradingClientResponse {
  id: string
  name: string
  phone: string
  city: string | null
  notes: string | null
  createdAt: string
}

// ── Caminhões e legs de pedido ────────────────────────────────────────────────

export interface OrderTruckRequest {
  truckPlate: string
  quantityKg: number
  freightValue?: number
  notes?: string
}

export interface OrderTruckResponse {
  id: string
  truckPlate: string
  quantityKg: number
  freightValue: number | null
  notes: string | null
}

export interface OrderSupplierLegRequest {
  supplierId: string
  supplierPricePerKg: number
  notes?: string
  trucks: OrderTruckRequest[]
}

export interface OrderSupplierLegResponse {
  id: string
  supplierId: string
  supplierName: string
  supplierCity: string | null
  supplierPricePerKg: number
  trucks: OrderTruckResponse[]
  totalKg: number
  totalCost: number
  notes: string | null
}

// ── Pedidos ───────────────────────────────────────────────────────────────────

export interface ClientOrderRequest {
  clientId: string
  orderDate: string
  clientPricePerKg: number
  notes?: string
  legs: OrderSupplierLegRequest[]
}

export interface ClientOrderDetailResponse {
  id: string
  clientId: string
  clientName: string
  clientPhone: string
  orderDate: string
  clientPricePerKg: number
  status: ClientOrderStatus
  legs: OrderSupplierLegResponse[]
  totalKg: number
  totalRevenue: number
  totalCost: number
  grossMargin: number
  notes: string | null
  createdAt: string
}

export interface ClientOrderSummaryResponse {
  id: string
  clientId: string
  clientName: string
  orderDate: string
  clientPricePerKg: number
  status: ClientOrderStatus
  totalKg: number
  totalRevenue: number
  totalCost: number
  grossMargin: number
  notes: string | null
  createdAt: string
}

// ── Dashboard ─────────────────────────────────────────────────────────────────

export interface TradingDashboardResponse {
  totalOrders: number
  openOrders: number
  closedOrders: number
  totalKg: number
  totalRevenue: number
  totalCost: number
  grossMargin: number
  totalClients: number
  totalSuppliers: number
}

// ── PageResponse genérico ─────────────────────────────────────────────────────

export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

// ── Service ───────────────────────────────────────────────────────────────────

const tradingService = {

  // Dashboard
  getDashboard: (accountId: string) =>
    api
      .get<{ data: ObjectPayload<TradingDashboardResponse> }>(`/accounts/${accountId}/trading/dashboard`)
      .then(normalizeObjectResponse<TradingDashboardResponse>),

  // Fornecedores
  createSupplier: (accountId: string, data: TradingSupplierRequest) =>
    api
      .post<{ data: ObjectPayload<TradingSupplierResponse> }>(`/accounts/${accountId}/trading/suppliers`, data)
      .then(normalizeObjectResponse<TradingSupplierResponse>),

  listSuppliers: (accountId: string, search?: string) =>
    api
      .get<{ data: ListPayload<TradingSupplierResponse> }>(`/accounts/${accountId}/trading/suppliers`, {
        params: search ? { search } : undefined
      })
      .then(normalizeListResponse<TradingSupplierResponse>),

  getSupplier: (accountId: string, supplierId: string) =>
    api
      .get<{ data: ObjectPayload<TradingSupplierResponse> }>(`/accounts/${accountId}/trading/suppliers/${supplierId}`)
      .then(normalizeObjectResponse<TradingSupplierResponse>),

  updateSupplier: (accountId: string, supplierId: string, data: TradingSupplierRequest) =>
    api
      .put<{ data: ObjectPayload<TradingSupplierResponse> }>(`/accounts/${accountId}/trading/suppliers/${supplierId}`, data)
      .then(normalizeObjectResponse<TradingSupplierResponse>),

  deleteSupplier: (accountId: string, supplierId: string) =>
    api.delete(`/accounts/${accountId}/trading/suppliers/${supplierId}`),

  // Clientes
  createClient: (accountId: string, data: TradingClientRequest) =>
    api
      .post<{ data: ObjectPayload<TradingClientResponse> }>(`/accounts/${accountId}/trading/clients`, data)
      .then(normalizeObjectResponse<TradingClientResponse>),

  listClients: (accountId: string, search?: string) =>
    api
      .get<{ data: ListPayload<TradingClientResponse> }>(`/accounts/${accountId}/trading/clients`, {
        params: search ? { search } : undefined
      })
      .then(normalizeListResponse<TradingClientResponse>),

  getClient: (accountId: string, clientId: string) =>
    api
      .get<{ data: ObjectPayload<TradingClientResponse> }>(`/accounts/${accountId}/trading/clients/${clientId}`)
      .then(normalizeObjectResponse<TradingClientResponse>),

  updateClient: (accountId: string, clientId: string, data: TradingClientRequest) =>
    api
      .put<{ data: ObjectPayload<TradingClientResponse> }>(`/accounts/${accountId}/trading/clients/${clientId}`, data)
      .then(normalizeObjectResponse<TradingClientResponse>),

  deleteClient: (accountId: string, clientId: string) =>
    api.delete(`/accounts/${accountId}/trading/clients/${clientId}`),

  // Pedidos
  createOrder: (accountId: string, data: ClientOrderRequest) =>
    api
      .post<{ data: ObjectPayload<ClientOrderDetailResponse> }>(`/accounts/${accountId}/trading/orders`, data)
      .then(normalizeObjectResponse<ClientOrderDetailResponse>),

  listOrders: (accountId: string, params?: { status?: ClientOrderStatus; clientId?: string; page?: number; size?: number }) =>
    api
      .get<{ data: unknown }>(`/accounts/${accountId}/trading/orders`, { params })
      .then((response) => ({
        ...response,
        data: {
          data: normalizePagePayload<ClientOrderSummaryResponse>(response.data?.data) as PageResponse<ClientOrderSummaryResponse>,
        },
      })),

  getOrder: (accountId: string, orderId: string) =>
    api
      .get<{ data: ObjectPayload<ClientOrderDetailResponse> }>(`/accounts/${accountId}/trading/orders/${orderId}`)
      .then(normalizeObjectResponse<ClientOrderDetailResponse>),

  updateOrder: (accountId: string, orderId: string, data: ClientOrderRequest) =>
    api
      .put<{ data: ObjectPayload<ClientOrderDetailResponse> }>(`/accounts/${accountId}/trading/orders/${orderId}`, data)
      .then(normalizeObjectResponse<ClientOrderDetailResponse>),

  closeOrder: (accountId: string, orderId: string) =>
    api.patch(`/accounts/${accountId}/trading/orders/${orderId}/close`),

  deleteOrder: (accountId: string, orderId: string) =>
    api.delete(`/accounts/${accountId}/trading/orders/${orderId}`),

  addLeg: (accountId: string, orderId: string, data: OrderSupplierLegRequest) =>
    api
      .post<{ data: ObjectPayload<ClientOrderDetailResponse> }>(`/accounts/${accountId}/trading/orders/${orderId}/legs`, data)
      .then(normalizeObjectResponse<ClientOrderDetailResponse>),

  deleteLeg: (accountId: string, orderId: string, legId: string) =>
    api.delete(`/accounts/${accountId}/trading/orders/${orderId}/legs/${legId}`),
}

export default tradingService
```

- [ ] **Step 2: Run type check**

```bash
npx vue-tsc --noEmit
```

Expected: no errors related to `tradingService.ts`. There will be errors in Dashboard and old views — those are fixed in subsequent tasks. Note only errors from files NOT being changed in this task.

- [ ] **Step 3: Commit**

```bash
git add src/services/tradingService.ts
git commit -m "feat: replace tradingService with new client/order types and API methods"
```

---

## Task 2: Update TraderDashboardView.vue

**Files:**
- Modify: `src/views/trader/TraderDashboardView.vue`

- [ ] **Step 1: Replace button route and KPI section**

Locate line 56 in the template. Replace:
```html
<button class="btn btn--primary" @click="router.push({ name: 'trader-lots-new' })">
        <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
        Registrar Lote
      </button>
```
With:
```html
<button class="btn btn--primary" @click="router.push({ name: 'trader-orders-new' })">
        <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
        Novo Pedido
      </button>
```

- [ ] **Step 2: Replace the 6 KPI cards**

Locate the `<div class="kpi-grid">` block (lines ~80–108). Replace the entire block with:
```html
      <div class="kpi-grid">
        <div class="kpi-card">
          <span class="kpi-card__label">Receita Total</span>
          <span class="kpi-card__value kpi-card__value--positive">{{ currency(dashboard.totalRevenue) }}</span>
        </div>
        <div class="kpi-card">
          <span class="kpi-card__label">Custo Total</span>
          <span class="kpi-card__value kpi-card__value--negative">{{ currency(dashboard.totalCost) }}</span>
        </div>
        <div class="kpi-card" :class="{ 'kpi-card--highlight': dashboard.grossMargin > 0 }">
          <span class="kpi-card__label">Margem Bruta</span>
          <span class="kpi-card__value" :class="dashboard.grossMargin >= 0 ? 'kpi-card__value--positive' : 'kpi-card__value--negative'">
            {{ currency(dashboard.grossMargin) }}
          </span>
        </div>
        <div class="kpi-card">
          <span class="kpi-card__label">Total de Kg</span>
          <span class="kpi-card__value">{{ kg(dashboard.totalKg) }}</span>
        </div>
        <div class="kpi-card">
          <span class="kpi-card__label">Pedidos Abertos</span>
          <span class="kpi-card__value">{{ dashboard.openOrders }}</span>
        </div>
        <div class="kpi-card">
          <span class="kpi-card__label">Pedidos Fechados</span>
          <span class="kpi-card__value">{{ dashboard.closedOrders }}</span>
        </div>
      </div>
```

- [ ] **Step 3: Replace the 2 stat cards**

Locate the `<div class="stats-row">` block (lines ~110–134). Replace the entire block with:
```html
      <div class="stats-row">
        <div class="stat-card" @click="router.push({ name: 'trader-orders' })">
          <div class="stat-card__icon stat-card__icon--amber">
            <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/><polyline points="10 9 9 9 8 9"/></svg>
          </div>
          <div class="stat-card__content">
            <span class="stat-card__value">{{ dashboard.totalOrders }}</span>
            <span class="stat-card__label">Pedidos no total</span>
          </div>
          <div class="stat-card__badges">
            <span class="badge badge--green">{{ dashboard.openOrders }} abertos</span>
            <span class="badge badge--gray">{{ dashboard.closedOrders }} fechados</span>
          </div>
        </div>

        <div class="stat-card" @click="router.push({ name: 'trader-clients' })">
          <div class="stat-card__icon stat-card__icon--teal">
            <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>
          </div>
          <div class="stat-card__content">
            <span class="stat-card__value">{{ dashboard.totalClients }}</span>
            <span class="stat-card__label">Clientes cadastrados</span>
          </div>
        </div>
      </div>
```

- [ ] **Step 4: Run type check**

```bash
npx vue-tsc --noEmit
```

Expected: no errors in `TraderDashboardView.vue`. Errors from old views still present — those are fixed later.

- [ ] **Step 5: Commit**

```bash
git add src/views/trader/TraderDashboardView.vue
git commit -m "feat: update TraderDashboardView to use new order/client dashboard fields"
```

---

## Task 3: Create TradingClientsView.vue

**Files:**
- Create: `src/views/trader/TradingClientsView.vue`

- [ ] **Step 1: Write the file**

```vue
<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useAccountStore } from '@/stores/accountStore'
import tradingService from '@/services/tradingService'
import type { TradingClientResponse, TradingClientRequest } from '@/services/tradingService'

const accountStore = useAccountStore()
const accountId = computed(() => accountStore.selectedAccount?.id)

const clients  = ref<TradingClientResponse[]>([])
const loading  = ref(true)
const saving   = ref(false)
const error    = ref('')
const search   = ref('')

let searchTimer: ReturnType<typeof setTimeout> | null = null
onUnmounted(() => { if (searchTimer) clearTimeout(searchTimer) })

const showModal    = ref(false)
const editingId    = ref<string | null>(null)
const form         = ref<TradingClientRequest>({ name: '', phone: '', city: '', notes: '' })
const formError    = ref('')

const deletingId   = ref<string | null>(null)
const deletingName = ref('')

onMounted(loadClients)

async function loadClients() {
  if (!accountId.value) return
  loading.value = true
  error.value   = ''
  try {
    const { data } = await tradingService.listClients(accountId.value, search.value || undefined)
    clients.value = data.data
  } catch {
    error.value = 'Erro ao carregar clientes.'
  } finally {
    loading.value = false
  }
}

function onSearchInput() {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(loadClients, 350)
}

function openCreate() {
  editingId.value = null
  form.value = { name: '', phone: '', city: '', notes: '' }
  formError.value = ''
  showModal.value = true
}

function openEdit(client: TradingClientResponse) {
  editingId.value = client.id
  form.value = { name: client.name, phone: client.phone, city: client.city ?? '', notes: client.notes ?? '' }
  formError.value = ''
  showModal.value = true
}

async function submitForm() {
  if (!form.value.name.trim()) { formError.value = 'Nome é obrigatório.'; return }
  if (!form.value.phone.trim()) { formError.value = 'Telefone é obrigatório.'; return }
  if (!accountId.value) return

  saving.value    = true
  formError.value = ''
  try {
    if (editingId.value) {
      await tradingService.updateClient(accountId.value, editingId.value, form.value)
    } else {
      await tradingService.createClient(accountId.value, form.value)
    }
    showModal.value = false
    await loadClients()
  } catch {
    formError.value = 'Erro ao salvar cliente. Tente novamente.'
  } finally {
    saving.value = false
  }
}

function openDelete(client: TradingClientResponse) {
  deletingId.value   = client.id
  deletingName.value = client.name
}

async function confirmDelete() {
  if (!accountId.value || !deletingId.value) return
  try {
    await tradingService.deleteClient(accountId.value, deletingId.value)
    deletingId.value = null
    await loadClients()
  } catch (e: any) {
    const msg = e?.response?.data?.message ?? ''
    error.value = msg.toLowerCase().includes('pedido') ? 'Este cliente possui pedidos vinculados e não pode ser excluído.' : 'Erro ao excluir cliente.'
    deletingId.value = null
  }
}
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h1 class="page-title">Clientes</h1>
        <p class="page-subtitle">Compradores que fazem pedidos de melancia</p>
      </div>
      <button class="btn btn--primary" @click="openCreate">
        <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
        Novo Cliente
      </button>
    </div>

    <div class="search-row">
      <div class="search-input-wrap">
        <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
        <input v-model="search" type="text" placeholder="Buscar por nome..." class="search-input" @input="onSearchInput" />
      </div>
    </div>

    <div v-if="error" class="error-banner">{{ error }}</div>
    <div v-if="loading" class="loading-state"><div class="spinner" /></div>

    <div v-else-if="clients.length === 0" class="empty-state">
      <svg xmlns="http://www.w3.org/2000/svg" width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>
      <p>{{ search ? 'Nenhum cliente encontrado.' : 'Nenhum cliente cadastrado ainda.' }}</p>
      <button v-if="!search" class="btn btn--primary" @click="openCreate">Cadastrar primeiro cliente</button>
    </div>

    <div v-else class="clients-grid">
      <div v-for="c in clients" :key="c.id" class="client-card">
        <div class="client-card__header">
          <div class="client-card__avatar">{{ c.name.charAt(0).toUpperCase() }}</div>
          <div class="client-card__info">
            <span class="client-card__name">{{ c.name }}</span>
            <span v-if="c.city" class="client-card__city">{{ c.city }}</span>
          </div>
        </div>
        <div class="client-card__phone">
          <svg xmlns="http://www.w3.org/2000/svg" width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07A19.5 19.5 0 0 1 4.69 13a19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 3.56 2h3a2 2 0 0 1 2 1.72c.127.96.361 1.903.7 2.81a2 2 0 0 1-.45 2.11L7.91 9.91a16 16 0 0 0 6.1 6.1l.9-.9a2 2 0 0 1 2.11-.45c.907.339 1.85.573 2.81.7A2 2 0 0 1 22 16.92z"/></svg>
          {{ c.phone }}
        </div>
        <div v-if="c.notes" class="client-card__notes">{{ c.notes }}</div>
        <div class="client-card__actions">
          <button class="btn-icon" title="Editar" @click="openEdit(c)">
            <svg xmlns="http://www.w3.org/2000/svg" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>
          </button>
          <button class="btn-icon btn-icon--danger" title="Excluir" @click="openDelete(c)">
            <svg xmlns="http://www.w3.org/2000/svg" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6"/><path d="M10 11v6"/><path d="M14 11v6"/><path d="M9 6V4a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v2"/></svg>
          </button>
        </div>
      </div>
    </div>
  </div>

  <div v-if="showModal" class="modal-overlay" @click.self="showModal = false">
    <div class="modal">
      <div class="modal__header">
        <h2 class="modal__title">{{ editingId ? 'Editar Cliente' : 'Novo Cliente' }}</h2>
        <button class="modal__close" @click="showModal = false">
          <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
        </button>
      </div>
      <div class="modal__body">
        <div v-if="formError" class="error-banner">{{ formError }}</div>
        <div class="form-group">
          <label class="form-label">Nome *</label>
          <input v-model="form.name" class="form-input" type="text" placeholder="Nome do cliente" />
        </div>
        <div class="form-row">
          <div class="form-group">
            <label class="form-label">Telefone *</label>
            <input v-model="form.phone" class="form-input" type="text" placeholder="(00) 00000-0000" />
          </div>
          <div class="form-group">
            <label class="form-label">Cidade</label>
            <input v-model="form.city" class="form-input" type="text" placeholder="Ex: Cristalina, GO" />
          </div>
        </div>
        <div class="form-group">
          <label class="form-label">Observações</label>
          <textarea v-model="form.notes" class="form-input form-textarea" placeholder="Informações adicionais..." />
        </div>
      </div>
      <div class="modal__footer">
        <button class="btn btn--secondary" @click="showModal = false">Cancelar</button>
        <button class="btn btn--primary" :disabled="saving" @click="submitForm">
          {{ saving ? 'Salvando...' : (editingId ? 'Salvar alterações' : 'Cadastrar') }}
        </button>
      </div>
    </div>
  </div>

  <div v-if="deletingId" class="modal-overlay" @click.self="deletingId = null">
    <div class="modal modal--sm">
      <div class="modal__header">
        <h2 class="modal__title">Excluir Cliente</h2>
      </div>
      <div class="modal__body">
        <p class="confirm-text">Tem certeza que deseja excluir <strong>{{ deletingName }}</strong>? Esta ação não pode ser desfeita.</p>
      </div>
      <div class="modal__footer">
        <button class="btn btn--secondary" @click="deletingId = null">Cancelar</button>
        <button class="btn btn--danger" @click="confirmDelete">Excluir</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.page-container { padding: 2rem 1.5rem; max-width: 900px; margin: 0 auto; }
.page-header { display: flex; align-items: flex-start; justify-content: space-between; gap: 1rem; margin-bottom: 1.5rem; }
.page-title { font-size: 1.75rem; font-weight: 700; color: var(--color-text); letter-spacing: -0.02em; line-height: 1.2; margin: 0 0 0.25rem; }
.page-subtitle { font-size: 0.875rem; color: var(--color-text-muted); margin: 0; }
.search-row { margin-bottom: 1.5rem; }
.search-input-wrap { position: relative; max-width: 340px; }
.search-input-wrap svg { position: absolute; left: 0.75rem; top: 50%; transform: translateY(-50%); color: var(--color-text-muted); pointer-events: none; }
.search-input { width: 100%; padding: 0.5rem 0.75rem 0.5rem 2.25rem; border: 1px solid var(--color-border); border-radius: var(--radius-sm); background: var(--color-card); color: var(--color-text); font-family: inherit; font-size: 0.875rem; }
.search-input:focus { outline: none; border-color: var(--color-primary); }
.clients-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(260px, 1fr)); gap: 1rem; }
.client-card { background: var(--color-card); border: 1px solid var(--color-border); border-radius: var(--radius-md); padding: 1.25rem; display: flex; flex-direction: column; gap: 0.625rem; box-shadow: var(--shadow-card); transition: box-shadow 0.15s; }
.client-card:hover { box-shadow: var(--shadow-card-hover); }
.client-card__header { display: flex; align-items: center; gap: 0.75rem; }
.client-card__avatar { width: 38px; height: 38px; border-radius: 50%; background: var(--color-primary-light); color: var(--color-primary); font-size: 1rem; font-weight: 700; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.client-card__info { display: flex; flex-direction: column; gap: 0.1rem; min-width: 0; }
.client-card__name { font-weight: 600; color: var(--color-text); font-size: 0.95rem; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.client-card__city { font-size: 0.78rem; color: var(--color-text-muted); }
.client-card__phone { display: flex; align-items: center; gap: 0.375rem; font-size: 0.82rem; color: var(--color-text-muted); }
.client-card__notes { font-size: 0.82rem; color: var(--color-text-muted); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.client-card__actions { display: flex; gap: 0.375rem; margin-top: 0.25rem; justify-content: flex-end; }
.btn { display: inline-flex; align-items: center; gap: 0.5rem; padding: 0.5rem 1rem; border-radius: var(--radius-sm); font-family: inherit; font-size: 0.875rem; font-weight: 600; border: none; cursor: pointer; transition: opacity 0.15s; white-space: nowrap; }
.btn--primary { background: var(--color-primary); color: #fff; }
.btn--secondary { background: var(--color-surface); color: var(--color-text); border: 1px solid var(--color-border); }
.btn--danger { background: var(--color-error); color: #fff; }
.btn:disabled { opacity: 0.6; cursor: not-allowed; }
.btn--primary:not(:disabled):hover, .btn--danger:not(:disabled):hover { opacity: 0.88; }
.btn-icon { width: 32px; height: 32px; border: 1px solid var(--color-border); border-radius: var(--radius-sm); background: none; cursor: pointer; display: flex; align-items: center; justify-content: center; color: var(--color-text-muted); transition: background 0.15s, color 0.15s; }
.btn-icon:hover { background: var(--color-surface); color: var(--color-text); }
.btn-icon--danger:hover { background: var(--color-error-light); color: var(--color-error); border-color: var(--color-error); }
.form-group { display: flex; flex-direction: column; gap: 0.375rem; }
.form-row { display: grid; grid-template-columns: 1fr 1fr; gap: 1rem; }
.form-label { font-size: 0.825rem; font-weight: 600; color: var(--color-text); }
.form-input { padding: 0.5rem 0.75rem; border: 1px solid var(--color-border); border-radius: var(--radius-sm); background: var(--color-surface); color: var(--color-text); font-family: inherit; font-size: 0.875rem; width: 100%; box-sizing: border-box; }
.form-input:focus { outline: none; border-color: var(--color-primary); }
.form-textarea { resize: vertical; min-height: 80px; }
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.45); display: flex; align-items: center; justify-content: center; z-index: 500; padding: 1rem; }
.modal { background: var(--color-card); border: 1px solid var(--color-border); border-radius: var(--radius-md); width: 100%; max-width: 480px; max-height: 90vh; overflow-y: auto; }
.modal--sm { max-width: 380px; }
.modal__header { display: flex; align-items: center; justify-content: space-between; padding: 1.25rem 1.5rem; border-bottom: 1px solid var(--color-border); }
.modal__title { font-size: 1rem; font-weight: 700; color: var(--color-text); margin: 0; }
.modal__close { background: none; border: none; cursor: pointer; color: var(--color-text-muted); padding: 0.25rem; display: flex; align-items: center; }
.modal__body { padding: 1.5rem; display: flex; flex-direction: column; gap: 1rem; }
.modal__footer { padding: 1rem 1.5rem; border-top: 1px solid var(--color-border); display: flex; justify-content: flex-end; gap: 0.75rem; }
.confirm-text { color: var(--color-text); font-size: 0.9rem; line-height: 1.5; margin: 0; }
.error-banner { background: var(--color-error-light); color: var(--color-error); border: 1px solid var(--color-error); border-radius: var(--radius-sm); padding: 0.625rem 0.875rem; font-size: 0.85rem; }
.loading-state { display: flex; justify-content: center; padding: 4rem; }
.spinner { width: 32px; height: 32px; border: 3px solid var(--color-border); border-top-color: var(--color-primary); border-radius: 50%; animation: spin 0.7s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
.empty-state { text-align: center; padding: 4rem 2rem; color: var(--color-text-muted); display: flex; flex-direction: column; align-items: center; gap: 1rem; }
@media (max-width: 768px) {
  .page-container { padding: 1.25rem 1rem; }
  .page-header { flex-direction: column; }
  .clients-grid { grid-template-columns: 1fr; }
  .form-row { grid-template-columns: 1fr; }
}
</style>
```

- [ ] **Step 2: Run type check**

```bash
npx vue-tsc --noEmit
```

Expected: `TradingClientsView.vue` compiles without errors.

- [ ] **Step 3: Commit**

```bash
git add src/views/trader/TradingClientsView.vue
git commit -m "feat: add TradingClientsView with modal CRUD for trading clients"
```

---

## Task 4: Create ClientOrdersView.vue

**Files:**
- Create: `src/views/trader/ClientOrdersView.vue`

- [ ] **Step 1: Write the file**

```vue
<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAccountStore } from '@/stores/accountStore'
import { useTraderFormatters } from '@/composables/useTraderFormatters'
import tradingService from '@/services/tradingService'
import type { ClientOrderSummaryResponse, TradingClientResponse, ClientOrderStatus } from '@/services/tradingService'

const router       = useRouter()
const accountStore = useAccountStore()
const { currency, kg } = useTraderFormatters()
const accountId = computed(() => accountStore.selectedAccount?.id)

const orders      = ref<ClientOrderSummaryResponse[]>([])
const clients     = ref<TradingClientResponse[]>([])
const loading     = ref(true)
const error       = ref('')
const totalPages  = ref(0)
const currentPage = ref(0)

const statusFilter = ref<ClientOrderStatus | ''>('')
const clientFilter = ref('')

const deletingId   = ref<string | null>(null)
const deletingName = ref('')
const deleteError  = ref('')

onMounted(async () => {
  await Promise.all([loadClients(), loadOrders()])
})

async function loadClients() {
  if (!accountId.value) return
  try {
    const { data } = await tradingService.listClients(accountId.value)
    clients.value = data.data
  } catch {
    // non-fatal
  }
}

async function loadOrders() {
  if (!accountId.value) return
  loading.value = true
  error.value   = ''
  try {
    const result = await tradingService.listOrders(accountId.value, {
      status:   statusFilter.value || undefined,
      clientId: clientFilter.value || undefined,
      page:     currentPage.value,
      size:     12,
    })
    const page = result.data.data
    orders.value     = page.content
    totalPages.value = page.totalPages
  } catch {
    error.value = 'Erro ao carregar pedidos.'
  } finally {
    loading.value = false
  }
}

function applyFilter() {
  currentPage.value = 0
  loadOrders()
}

function goToPage(page: number) {
  currentPage.value = page
  loadOrders()
}

function openDelete(order: ClientOrderSummaryResponse) {
  deletingId.value   = order.id
  deletingName.value = `Pedido de ${order.clientName} em ${formatDate(order.orderDate)}`
  deleteError.value  = ''
}

async function confirmDelete() {
  if (!accountId.value || !deletingId.value) return
  try {
    await tradingService.deleteOrder(accountId.value, deletingId.value)
    deletingId.value = null
    await loadOrders()
  } catch (e: any) {
    const msg = e?.response?.data?.message ?? ''
    deleteError.value = msg || 'Erro ao excluir pedido.'
  }
}

function formatDate(iso: string) {
  const [y, m, d] = iso.split('-')
  return `${d}/${m}/${y}`
}

function marginClass(margin: number) {
  return margin >= 0 ? 'value--positive' : 'value--negative'
}
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h1 class="page-title">Pedidos</h1>
        <p class="page-subtitle">Pedidos dos clientes e suas pernas de fornecedor</p>
      </div>
      <button class="btn btn--primary" @click="router.push({ name: 'trader-orders-new' })">
        <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
        Novo Pedido
      </button>
    </div>

    <!-- Filtros -->
    <div class="filter-row">
      <select v-model="statusFilter" class="form-select" @change="applyFilter">
        <option value="">Todos os status</option>
        <option value="OPEN">Em aberto</option>
        <option value="CLOSED">Fechados</option>
      </select>
      <select v-model="clientFilter" class="form-select" @change="applyFilter">
        <option value="">Todos os clientes</option>
        <option v-for="c in clients" :key="c.id" :value="c.id">{{ c.name }}</option>
      </select>
    </div>

    <div v-if="error" class="error-banner">{{ error }}</div>
    <div v-if="loading" class="loading-state"><div class="spinner" /></div>

    <div v-else-if="orders.length === 0" class="empty-state">
      <svg xmlns="http://www.w3.org/2000/svg" width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>
      <p>{{ statusFilter || clientFilter ? 'Nenhum pedido encontrado com esses filtros.' : 'Nenhum pedido cadastrado ainda.' }}</p>
      <button v-if="!statusFilter && !clientFilter" class="btn btn--primary" @click="router.push({ name: 'trader-orders-new' })">Criar primeiro pedido</button>
    </div>

    <div v-else class="orders-table">
      <div class="orders-table__header">
        <span>Cliente</span>
        <span>Data</span>
        <span class="align-right">Total Kg</span>
        <span class="align-right">Receita</span>
        <span class="align-right">Margem</span>
        <span class="align-center">Status</span>
        <span></span>
      </div>
      <div
        v-for="order in orders"
        :key="order.id"
        class="orders-table__row"
        @click="router.push({ name: 'trader-order-detail', params: { orderId: order.id } })"
      >
        <span class="client-name">{{ order.clientName }}</span>
        <span class="date">{{ formatDate(order.orderDate) }}</span>
        <span class="align-right">{{ kg(order.totalKg) }}</span>
        <span class="align-right">{{ currency(order.totalRevenue) }}</span>
        <span class="align-right" :class="marginClass(order.grossMargin)">{{ currency(order.grossMargin) }}</span>
        <span class="align-center">
          <span class="badge" :class="order.status === 'OPEN' ? 'badge--open' : 'badge--closed'">
            {{ order.status === 'OPEN' ? 'Aberto' : 'Fechado' }}
          </span>
        </span>
        <span class="row-actions" @click.stop>
          <button
            class="btn-icon"
            title="Editar"
            :disabled="order.status === 'CLOSED'"
            @click="router.push({ name: 'trader-order-edit', params: { orderId: order.id } })"
          >
            <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>
          </button>
          <button
            class="btn-icon btn-icon--danger"
            title="Excluir"
            :disabled="order.status === 'CLOSED'"
            @click="openDelete(order)"
          >
            <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6"/></svg>
          </button>
        </span>
      </div>
    </div>

    <!-- Paginação -->
    <div v-if="totalPages > 1" class="pagination">
      <button class="btn btn--secondary btn--sm" :disabled="currentPage === 0" @click="goToPage(currentPage - 1)">Anterior</button>
      <span class="pagination__info">{{ currentPage + 1 }} / {{ totalPages }}</span>
      <button class="btn btn--secondary btn--sm" :disabled="currentPage >= totalPages - 1" @click="goToPage(currentPage + 1)">Próxima</button>
    </div>
  </div>

  <!-- Modal confirmação de exclusão -->
  <div v-if="deletingId" class="modal-overlay" @click.self="deletingId = null">
    <div class="modal modal--sm">
      <div class="modal__header">
        <h2 class="modal__title">Excluir Pedido</h2>
      </div>
      <div class="modal__body">
        <div v-if="deleteError" class="error-banner">{{ deleteError }}</div>
        <p class="confirm-text">Tem certeza que deseja excluir <strong>{{ deletingName }}</strong>? Todos os dados do pedido serão removidos.</p>
      </div>
      <div class="modal__footer">
        <button class="btn btn--secondary" @click="deletingId = null">Cancelar</button>
        <button class="btn btn--danger" @click="confirmDelete">Excluir</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.page-container { padding: 2rem 1.5rem; max-width: 1100px; margin: 0 auto; }
.page-header { display: flex; align-items: flex-start; justify-content: space-between; gap: 1rem; margin-bottom: 1.5rem; }
.page-title { font-size: 1.75rem; font-weight: 700; color: var(--color-text); letter-spacing: -0.02em; line-height: 1.2; margin: 0 0 0.25rem; }
.page-subtitle { font-size: 0.875rem; color: var(--color-text-muted); margin: 0; }
.filter-row { display: flex; gap: 0.75rem; margin-bottom: 1.5rem; flex-wrap: wrap; }
.form-select { padding: 0.5rem 0.75rem; border: 1px solid var(--color-border); border-radius: var(--radius-sm); background: var(--color-card); color: var(--color-text); font-family: inherit; font-size: 0.875rem; cursor: pointer; }
.form-select:focus { outline: none; border-color: var(--color-primary); }
.orders-table { background: var(--color-card); border: 1px solid var(--color-border); border-radius: var(--radius-md); overflow: hidden; box-shadow: var(--shadow-card); }
.orders-table__header { display: grid; grid-template-columns: 1fr 90px 90px 110px 110px 90px 80px; gap: 0.5rem; padding: 0.75rem 1rem; background: var(--color-surface); font-size: 0.75rem; font-weight: 600; color: var(--color-text-muted); text-transform: uppercase; letter-spacing: 0.04em; border-bottom: 1px solid var(--color-border); }
.orders-table__row { display: grid; grid-template-columns: 1fr 90px 90px 110px 110px 90px 80px; gap: 0.5rem; padding: 0.875rem 1rem; border-bottom: 1px solid var(--color-border); cursor: pointer; font-size: 0.875rem; color: var(--color-text); align-items: center; transition: background 0.1s; }
.orders-table__row:last-child { border-bottom: none; }
.orders-table__row:hover { background: var(--color-surface); }
.client-name { font-weight: 600; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.date { color: var(--color-text-muted); font-size: 0.82rem; }
.align-right { text-align: right; }
.align-center { text-align: center; }
.value--positive { color: var(--color-primary); font-weight: 600; }
.value--negative { color: var(--color-error); font-weight: 600; }
.badge { font-size: 0.7rem; font-weight: 700; padding: 0.2rem 0.6rem; border-radius: 20px; white-space: nowrap; }
.badge--open { background: var(--color-primary-light); color: var(--color-primary); }
.badge--closed { background: var(--color-surface); color: var(--color-text-muted); }
.row-actions { display: flex; gap: 0.25rem; justify-content: flex-end; }
.btn { display: inline-flex; align-items: center; gap: 0.5rem; padding: 0.5rem 1rem; border-radius: var(--radius-sm); font-family: inherit; font-size: 0.875rem; font-weight: 600; border: none; cursor: pointer; transition: opacity 0.15s; white-space: nowrap; }
.btn--primary { background: var(--color-primary); color: #fff; }
.btn--secondary { background: var(--color-surface); color: var(--color-text); border: 1px solid var(--color-border); }
.btn--danger { background: var(--color-error); color: #fff; }
.btn--sm { padding: 0.375rem 0.75rem; font-size: 0.8rem; }
.btn:disabled { opacity: 0.6; cursor: not-allowed; }
.btn--primary:not(:disabled):hover { opacity: 0.88; }
.btn-icon { width: 30px; height: 30px; border: 1px solid var(--color-border); border-radius: var(--radius-sm); background: none; cursor: pointer; display: flex; align-items: center; justify-content: center; color: var(--color-text-muted); transition: background 0.15s; }
.btn-icon:disabled { opacity: 0.35; cursor: not-allowed; }
.btn-icon:not(:disabled):hover { background: var(--color-surface); color: var(--color-text); }
.btn-icon--danger:not(:disabled):hover { background: var(--color-error-light); color: var(--color-error); border-color: var(--color-error); }
.pagination { display: flex; align-items: center; justify-content: center; gap: 1rem; margin-top: 1.5rem; }
.pagination__info { font-size: 0.875rem; color: var(--color-text-muted); }
.error-banner { background: var(--color-error-light); color: var(--color-error); border: 1px solid var(--color-error); border-radius: var(--radius-sm); padding: 0.625rem 0.875rem; font-size: 0.85rem; margin-bottom: 0.5rem; }
.loading-state { display: flex; justify-content: center; padding: 4rem; }
.spinner { width: 32px; height: 32px; border: 3px solid var(--color-border); border-top-color: var(--color-primary); border-radius: 50%; animation: spin 0.7s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
.empty-state { text-align: center; padding: 4rem 2rem; color: var(--color-text-muted); display: flex; flex-direction: column; align-items: center; gap: 1rem; }
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.45); display: flex; align-items: center; justify-content: center; z-index: 500; padding: 1rem; }
.modal { background: var(--color-card); border: 1px solid var(--color-border); border-radius: var(--radius-md); width: 100%; max-width: 480px; }
.modal--sm { max-width: 380px; }
.modal__header { display: flex; align-items: center; justify-content: space-between; padding: 1.25rem 1.5rem; border-bottom: 1px solid var(--color-border); }
.modal__title { font-size: 1rem; font-weight: 700; color: var(--color-text); margin: 0; }
.modal__body { padding: 1.5rem; display: flex; flex-direction: column; gap: 0.75rem; }
.modal__footer { padding: 1rem 1.5rem; border-top: 1px solid var(--color-border); display: flex; justify-content: flex-end; gap: 0.75rem; }
.confirm-text { color: var(--color-text); font-size: 0.9rem; line-height: 1.5; margin: 0; }
@media (max-width: 900px) {
  .orders-table__header, .orders-table__row { grid-template-columns: 1fr 80px 90px 80px; }
  .orders-table__header span:nth-child(3),
  .orders-table__row span:nth-child(3),
  .orders-table__header span:nth-child(5),
  .orders-table__row span:nth-child(5) { display: none; }
}
@media (max-width: 600px) {
  .page-container { padding: 1.25rem 1rem; }
  .page-header { flex-direction: column; }
  .orders-table__header, .orders-table__row { grid-template-columns: 1fr 80px 80px; }
  .orders-table__header span:nth-child(3),
  .orders-table__row span:nth-child(3),
  .orders-table__header span:nth-child(4),
  .orders-table__row span:nth-child(4),
  .orders-table__header span:nth-child(5),
  .orders-table__row span:nth-child(5),
  .orders-table__header span:nth-child(7),
  .orders-table__row span:nth-child(7) { display: none; }
}
</style>
```

- [ ] **Step 2: Run type check**

```bash
npx vue-tsc --noEmit
```

Expected: `ClientOrdersView.vue` compiles without errors.

- [ ] **Step 3: Commit**

```bash
git add src/views/trader/ClientOrdersView.vue
git commit -m "feat: add ClientOrdersView with paginated list and status/client filters"
```

---

## Task 5: Create ClientOrderFormView.vue

**Files:**
- Create: `src/views/trader/ClientOrderFormView.vue`

- [ ] **Step 1: Write the file**

```vue
<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAccountStore } from '@/stores/accountStore'
import { useTraderFormatters } from '@/composables/useTraderFormatters'
import tradingService from '@/services/tradingService'
import type {
  TradingClientResponse,
  TradingSupplierResponse,
  ClientOrderRequest,
} from '@/services/tradingService'

const route        = useRoute()
const router       = useRouter()
const accountStore = useAccountStore()
const { currency, kg } = useTraderFormatters()

const accountId = computed(() => accountStore.selectedAccount?.id)
const orderId   = route.params.orderId as string | undefined

// ── Dados de suporte ──────────────────────────────────────────────────────────

const clients   = ref<TradingClientResponse[]>([])
const suppliers = ref<TradingSupplierResponse[]>([])

// ── Estado da página ──────────────────────────────────────────────────────────

const loading   = ref(true)
const saving    = ref(false)
const formError = ref('')

// ── Tipos internos de formulário ──────────────────────────────────────────────

interface TruckForm {
  _key: number
  truckPlate: string
  quantityKg: number | ''
  freightValue: number | ''
  notes: string
}

interface LegForm {
  _key: number
  supplierId: string
  supplierPricePerKg: number | ''
  notes: string
  trucks: TruckForm[]
}

interface OrderForm {
  clientId: string
  orderDate: string
  clientPricePerKg: number | ''
  notes: string
  legs: LegForm[]
}

// _key counter — not reactive, only used for v-for :key stability
let _key = 0

function newTruck(): TruckForm {
  return { _key: _key++, truckPlate: '', quantityKg: '', freightValue: '', notes: '' }
}

function newLeg(): LegForm {
  return { _key: _key++, supplierId: '', supplierPricePerKg: '', notes: '', trucks: [newTruck()] }
}

const form = ref<OrderForm>({
  clientId:         '',
  orderDate:        new Date().toISOString().slice(0, 10),
  clientPricePerKg: '',
  notes:            '',
  legs:             [newLeg()],
})

// ── Resumo ao vivo ────────────────────────────────────────────────────────────

const summary = computed(() => {
  const clientPrice = Number(form.value.clientPricePerKg) || 0
  let totalKg = 0
  let totalProductCost = 0
  let totalFreight = 0

  for (const leg of form.value.legs) {
    const supplierPrice = Number(leg.supplierPricePerKg) || 0
    for (const truck of leg.trucks) {
      const kgVal = Number(truck.quantityKg) || 0
      totalKg          += kgVal
      totalProductCost += kgVal * supplierPrice
      totalFreight     += Number(truck.freightValue) || 0
    }
  }

  const totalCost    = totalProductCost + totalFreight
  const totalRevenue = totalKg * clientPrice
  const grossMargin  = totalRevenue - totalCost

  return { totalKg, totalRevenue, totalProductCost, totalFreight, totalCost, grossMargin }
})

// ── Inicialização ─────────────────────────────────────────────────────────────

onMounted(async () => {
  if (!accountId.value) return
  loading.value = true
  try {
    const [clientsRes, suppliersRes] = await Promise.all([
      tradingService.listClients(accountId.value),
      tradingService.listSuppliers(accountId.value),
    ])
    clients.value   = clientsRes.data.data
    suppliers.value = suppliersRes.data.data

    if (orderId) {
      const { data } = await tradingService.getOrder(accountId.value, orderId)
      const o = data.data
      form.value = {
        clientId:         o.clientId,
        orderDate:        o.orderDate,
        clientPricePerKg: o.clientPricePerKg,
        notes:            o.notes ?? '',
        legs: o.legs.map(leg => ({
          _key:               _key++,
          supplierId:         leg.supplierId,
          supplierPricePerKg: leg.supplierPricePerKg,
          notes:              leg.notes ?? '',
          trucks: leg.trucks.map(t => ({
            _key:         _key++,
            truckPlate:   t.truckPlate,
            quantityKg:   t.quantityKg,
            freightValue: t.freightValue ?? '',
            notes:        t.notes ?? '',
          })),
        })),
      }
    }
  } catch {
    formError.value = 'Erro ao carregar dados.'
  } finally {
    loading.value = false
  }
})

// ── Manipulação dinâmica de legs e trucks ─────────────────────────────────────

function addLeg() {
  form.value.legs.push(newLeg())
}

function removeLeg(index: number) {
  if (form.value.legs.length === 1) return
  form.value.legs.splice(index, 1)
}

function addTruck(leg: LegForm) {
  leg.trucks.push(newTruck())
}

function removeTruck(leg: LegForm, index: number) {
  if (leg.trucks.length === 1) return
  leg.trucks.splice(index, 1)
}

// ── Submit ────────────────────────────────────────────────────────────────────

async function submit() {
  formError.value = ''

  if (!form.value.clientId)                   { formError.value = 'Selecione o cliente.'; return }
  if (!form.value.orderDate)                  { formError.value = 'Informe a data do pedido.'; return }
  if (Number(form.value.clientPricePerKg) <= 0) { formError.value = 'Preço/kg do cliente deve ser maior que zero.'; return }
  if (form.value.legs.length === 0)           { formError.value = 'Adicione ao menos um fornecedor.'; return }

  for (let li = 0; li < form.value.legs.length; li++) {
    const leg = form.value.legs[li]
    if (!leg.supplierId)                         { formError.value = `Selecione o fornecedor na perna ${li + 1}.`; return }
    if (Number(leg.supplierPricePerKg) <= 0)     { formError.value = `Preço/kg do fornecedor na perna ${li + 1} deve ser maior que zero.`; return }
    if (leg.trucks.length === 0)                 { formError.value = `Adicione ao menos um caminhão na perna ${li + 1}.`; return }
    for (let ti = 0; ti < leg.trucks.length; ti++) {
      const t = leg.trucks[ti]
      if (!t.truckPlate.trim())            { formError.value = `Informe a placa do caminhão ${ti + 1} na perna ${li + 1}.`; return }
      if (t.truckPlate.trim().length > 10) { formError.value = `Placa do caminhão ${ti + 1} na perna ${li + 1} deve ter no máximo 10 caracteres.`; return }
      if (Number(t.quantityKg) <= 0)       { formError.value = `Quantidade (kg) do caminhão ${ti + 1} na perna ${li + 1} deve ser maior que zero.`; return }
      if (t.freightValue !== '' && Number(t.freightValue) < 0) { formError.value = `Frete do caminhão ${ti + 1} não pode ser negativo.`; return }
    }
  }

  const payload: ClientOrderRequest = {
    clientId:         form.value.clientId,
    orderDate:        form.value.orderDate,
    clientPricePerKg: Number(form.value.clientPricePerKg),
    notes:            form.value.notes || undefined,
    legs: form.value.legs.map(leg => ({
      supplierId:         leg.supplierId,
      supplierPricePerKg: Number(leg.supplierPricePerKg),
      notes:              leg.notes || undefined,
      trucks: leg.trucks.map(t => ({
        truckPlate:   t.truckPlate.trim().toUpperCase(),
        quantityKg:   Number(t.quantityKg),
        freightValue: t.freightValue !== '' ? Number(t.freightValue) : undefined,
        notes:        t.notes || undefined,
      })),
    })),
  }

  saving.value = true
  try {
    if (orderId) {
      await tradingService.updateOrder(accountId.value!, orderId, payload)
    } else {
      await tradingService.createOrder(accountId.value!, payload)
    }
    router.push({ name: 'trader-orders' })
  } catch (e: any) {
    formError.value = e?.response?.data?.message ?? 'Erro ao salvar pedido. Tente novamente.'
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h1 class="page-title">{{ orderId ? 'Editar Pedido' : 'Novo Pedido' }}</h1>
        <p class="page-subtitle">Preencha o pedido, os fornecedores e os caminhões</p>
      </div>
      <button class="btn btn--secondary" @click="router.push({ name: 'trader-orders' })">Cancelar</button>
    </div>

    <div v-if="loading" class="loading-state"><div class="spinner" /></div>

    <form v-else @submit.prevent="submit">
      <div v-if="formError" class="error-banner">{{ formError }}</div>

      <!-- Cabeçalho do pedido -->
      <div class="section-card">
        <h2 class="section-title">Dados do Pedido</h2>
        <div class="form-grid">
          <div class="form-group form-group--wide">
            <label class="form-label">Cliente *</label>
            <select v-model="form.clientId" class="form-input">
              <option value="">Selecione o cliente</option>
              <option v-for="c in clients" :key="c.id" :value="c.id">{{ c.name }}</option>
            </select>
          </div>
          <div class="form-group">
            <label class="form-label">Data do Pedido *</label>
            <input v-model="form.orderDate" type="date" class="form-input" />
          </div>
          <div class="form-group">
            <label class="form-label">Preço/kg do Cliente (R$) *</label>
            <input v-model="form.clientPricePerKg" type="number" step="0.01" min="0" class="form-input" placeholder="0,00" />
          </div>
          <div class="form-group form-group--full">
            <label class="form-label">Observações</label>
            <textarea v-model="form.notes" class="form-input form-textarea" placeholder="Observações sobre o pedido..." />
          </div>
        </div>
      </div>

      <!-- Legs de fornecedores -->
      <div class="legs-section">
        <div v-for="(leg, li) in form.legs" :key="leg._key" class="leg-card">
          <div class="leg-card__header">
            <h3 class="leg-card__title">Fornecedor {{ li + 1 }}</h3>
            <button
              v-if="form.legs.length > 1"
              type="button"
              class="btn-icon btn-icon--danger"
              title="Remover fornecedor"
              @click="removeLeg(li)"
            >
              <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
            </button>
          </div>

          <div class="form-grid">
            <div class="form-group form-group--wide">
              <label class="form-label">Fornecedor *</label>
              <select v-model="leg.supplierId" class="form-input">
                <option value="">Selecione o fornecedor</option>
                <option v-for="s in suppliers" :key="s.id" :value="s.id">{{ s.name }}</option>
              </select>
            </div>
            <div class="form-group">
              <label class="form-label">Preço/kg do Fornecedor (R$) *</label>
              <input v-model="leg.supplierPricePerKg" type="number" step="0.01" min="0" class="form-input" placeholder="0,00" />
            </div>
            <div class="form-group form-group--full">
              <label class="form-label">Observações da perna</label>
              <input v-model="leg.notes" type="text" class="form-input" placeholder="Observações opcionais..." />
            </div>
          </div>

          <!-- Trucks da leg -->
          <div class="trucks-section">
            <div v-for="(truck, ti) in leg.trucks" :key="truck._key" class="truck-row">
              <div class="truck-row__fields">
                <div class="form-group">
                  <label class="form-label">Placa *</label>
                  <input v-model="truck.truckPlate" type="text" maxlength="10" class="form-input" placeholder="ABC1D23" style="text-transform:uppercase" />
                </div>
                <div class="form-group">
                  <label class="form-label">Quantidade (kg) *</label>
                  <input v-model="truck.quantityKg" type="number" step="0.01" min="0" class="form-input" placeholder="0,00" />
                </div>
                <div class="form-group">
                  <label class="form-label">Frete (R$)</label>
                  <input v-model="truck.freightValue" type="number" step="0.01" min="0" class="form-input" placeholder="Opcional" />
                </div>
                <div class="form-group form-group--notes">
                  <label class="form-label">Obs.</label>
                  <input v-model="truck.notes" type="text" class="form-input" placeholder="Opcional" />
                </div>
              </div>
              <button
                v-if="leg.trucks.length > 1"
                type="button"
                class="btn-icon btn-icon--danger truck-remove"
                title="Remover caminhão"
                @click="removeTruck(leg, ti)"
              >
                <svg xmlns="http://www.w3.org/2000/svg" width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
              </button>
            </div>

            <button type="button" class="btn btn--ghost btn--sm" @click="addTruck(leg)">
              <svg xmlns="http://www.w3.org/2000/svg" width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
              Adicionar Caminhão
            </button>
          </div>
        </div>

        <button type="button" class="btn btn--secondary" @click="addLeg">
          <svg xmlns="http://www.w3.org/2000/svg" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
          Adicionar Fornecedor
        </button>
      </div>

      <!-- Resumo ao vivo -->
      <div class="summary-card">
        <h2 class="section-title">Resumo</h2>
        <div class="summary-grid">
          <div class="summary-item">
            <span class="summary-item__label">Total de Kg</span>
            <span class="summary-item__value">{{ kg(summary.totalKg) }}</span>
          </div>
          <div class="summary-item">
            <span class="summary-item__label">Receita Estimada</span>
            <span class="summary-item__value summary-item__value--positive">{{ currency(summary.totalRevenue) }}</span>
          </div>
          <div class="summary-item">
            <span class="summary-item__label">Custo Produto</span>
            <span class="summary-item__value">{{ currency(summary.totalProductCost) }}</span>
          </div>
          <div class="summary-item">
            <span class="summary-item__label">Custo Frete</span>
            <span class="summary-item__value">{{ currency(summary.totalFreight) }}</span>
          </div>
          <div class="summary-item">
            <span class="summary-item__label">Custo Total</span>
            <span class="summary-item__value summary-item__value--negative">{{ currency(summary.totalCost) }}</span>
          </div>
          <div class="summary-item summary-item--highlight">
            <span class="summary-item__label">Margem Bruta</span>
            <span class="summary-item__value" :class="summary.grossMargin >= 0 ? 'summary-item__value--positive' : 'summary-item__value--negative'">
              {{ currency(summary.grossMargin) }}
            </span>
          </div>
        </div>
      </div>

      <!-- Ações -->
      <div class="form-actions">
        <button type="button" class="btn btn--secondary" @click="router.push({ name: 'trader-orders' })">Cancelar</button>
        <button type="submit" class="btn btn--primary" :disabled="saving">
          {{ saving ? 'Salvando...' : (orderId ? 'Salvar Alterações' : 'Criar Pedido') }}
        </button>
      </div>
    </form>
  </div>
</template>

<style scoped>
.page-container { padding: 2rem 1.5rem; max-width: 860px; margin: 0 auto; }
.page-header { display: flex; align-items: flex-start; justify-content: space-between; gap: 1rem; margin-bottom: 1.5rem; }
.page-title { font-size: 1.75rem; font-weight: 700; color: var(--color-text); letter-spacing: -0.02em; line-height: 1.2; margin: 0 0 0.25rem; }
.page-subtitle { font-size: 0.875rem; color: var(--color-text-muted); margin: 0; }

.section-card, .leg-card, .summary-card {
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 1.5rem;
  margin-bottom: 1rem;
  box-shadow: var(--shadow-card);
}

.section-title { font-size: 0.95rem; font-weight: 700; color: var(--color-text); margin: 0 0 1rem; letter-spacing: -0.01em; }

.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 1rem; }
.form-group { display: flex; flex-direction: column; gap: 0.375rem; }
.form-group--wide { grid-column: span 2; }
.form-group--full { grid-column: 1 / -1; }
.form-group--notes { flex: 1; }
.form-label { font-size: 0.825rem; font-weight: 600; color: var(--color-text); }
.form-input { padding: 0.5rem 0.75rem; border: 1px solid var(--color-border); border-radius: var(--radius-sm); background: var(--color-surface); color: var(--color-text); font-family: inherit; font-size: 0.875rem; width: 100%; box-sizing: border-box; }
.form-input:focus { outline: none; border-color: var(--color-primary); }
.form-textarea { resize: vertical; min-height: 64px; }

.legs-section { display: flex; flex-direction: column; gap: 0.75rem; margin-bottom: 1rem; }

.leg-card__header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 1rem; }
.leg-card__title { font-size: 0.875rem; font-weight: 700; color: var(--color-primary); margin: 0; text-transform: uppercase; letter-spacing: 0.04em; }

.trucks-section { margin-top: 1rem; display: flex; flex-direction: column; gap: 0.625rem; border-top: 1px solid var(--color-border); padding-top: 1rem; }

.truck-row { display: flex; align-items: flex-end; gap: 0.5rem; }
.truck-row__fields { display: grid; grid-template-columns: 100px 110px 100px 1fr; gap: 0.625rem; flex: 1; }

.truck-remove { flex-shrink: 0; margin-bottom: 0.1rem; }

.summary-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 0.75rem; }
.summary-item { display: flex; flex-direction: column; gap: 0.25rem; padding: 0.875rem; background: var(--color-surface); border-radius: var(--radius-sm); }
.summary-item--highlight { border: 1px solid var(--color-primary); background: var(--color-primary-light); }
.summary-item__label { font-size: 0.72rem; font-weight: 600; color: var(--color-text-muted); text-transform: uppercase; letter-spacing: 0.04em; }
.summary-item__value { font-size: 1.1rem; font-weight: 700; color: var(--color-text); }
.summary-item__value--positive { color: var(--color-primary); }
.summary-item__value--negative { color: var(--color-error); }

.form-actions { display: flex; justify-content: flex-end; gap: 0.75rem; margin-top: 1.5rem; }

.btn { display: inline-flex; align-items: center; gap: 0.5rem; padding: 0.5rem 1rem; border-radius: var(--radius-sm); font-family: inherit; font-size: 0.875rem; font-weight: 600; border: none; cursor: pointer; transition: opacity 0.15s; white-space: nowrap; }
.btn--primary { background: var(--color-primary); color: #fff; }
.btn--secondary { background: var(--color-surface); color: var(--color-text); border: 1px solid var(--color-border); }
.btn--ghost { background: none; color: var(--color-primary); border: 1px dashed var(--color-border); }
.btn--sm { padding: 0.35rem 0.75rem; font-size: 0.8rem; }
.btn:disabled { opacity: 0.6; cursor: not-allowed; }
.btn--primary:not(:disabled):hover, .btn--ghost:not(:disabled):hover { opacity: 0.85; }

.btn-icon { width: 30px; height: 30px; border: 1px solid var(--color-border); border-radius: var(--radius-sm); background: none; cursor: pointer; display: flex; align-items: center; justify-content: center; color: var(--color-text-muted); transition: background 0.15s; flex-shrink: 0; }
.btn-icon--danger:hover { background: var(--color-error-light); color: var(--color-error); border-color: var(--color-error); }

.error-banner { background: var(--color-error-light); color: var(--color-error); border: 1px solid var(--color-error); border-radius: var(--radius-sm); padding: 0.625rem 0.875rem; font-size: 0.85rem; margin-bottom: 1rem; }
.loading-state { display: flex; justify-content: center; padding: 4rem; }
.spinner { width: 32px; height: 32px; border: 3px solid var(--color-border); border-top-color: var(--color-primary); border-radius: 50%; animation: spin 0.7s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

@media (max-width: 768px) {
  .page-container { padding: 1.25rem 1rem; }
  .page-header { flex-direction: column; }
  .form-grid { grid-template-columns: 1fr; }
  .form-group--wide, .form-group--full { grid-column: span 1; }
  .truck-row__fields { grid-template-columns: 1fr 1fr; }
  .summary-grid { grid-template-columns: 1fr 1fr; }
}

@media (max-width: 480px) {
  .truck-row__fields { grid-template-columns: 1fr; }
  .summary-grid { grid-template-columns: 1fr; }
}
</style>
```

- [ ] **Step 2: Run type check**

```bash
npx vue-tsc --noEmit
```

Expected: `ClientOrderFormView.vue` compiles without errors.

- [ ] **Step 3: Commit**

```bash
git add src/views/trader/ClientOrderFormView.vue
git commit -m "feat: add ClientOrderFormView with nested leg/truck form and live margin summary"
```

---

## Task 6: Create ClientOrderDetailView.vue

**Files:**
- Create: `src/views/trader/ClientOrderDetailView.vue`

- [ ] **Step 1: Write the file**

```vue
<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAccountStore } from '@/stores/accountStore'
import { useTraderFormatters } from '@/composables/useTraderFormatters'
import tradingService from '@/services/tradingService'
import type { ClientOrderDetailResponse } from '@/services/tradingService'

const route        = useRoute()
const router       = useRouter()
const accountStore = useAccountStore()
const { currency, kg } = useTraderFormatters()

const accountId = computed(() => accountStore.selectedAccount?.id)
const orderId   = route.params.orderId as string

const order      = ref<ClientOrderDetailResponse | null>(null)
const loading    = ref(true)
const error      = ref('')
const closing    = ref(false)
const showDelete = ref(false)
const deleteError = ref('')

onMounted(loadOrder)

async function loadOrder() {
  if (!accountId.value) return
  loading.value = true
  error.value   = ''
  try {
    const { data } = await tradingService.getOrder(accountId.value, orderId)
    order.value = data.data
  } catch {
    error.value = 'Erro ao carregar pedido.'
  } finally {
    loading.value = false
  }
}

async function closeOrder() {
  if (!accountId.value || !order.value) return
  closing.value = true
  try {
    await tradingService.closeOrder(accountId.value, orderId)
    await loadOrder()
  } catch {
    error.value = 'Erro ao fechar pedido.'
  } finally {
    closing.value = false
  }
}

async function confirmDelete() {
  if (!accountId.value) return
  try {
    await tradingService.deleteOrder(accountId.value, orderId)
    router.push({ name: 'trader-orders' })
  } catch (e: any) {
    deleteError.value = e?.response?.data?.message ?? 'Erro ao excluir pedido.'
  }
}

function formatDate(iso: string) {
  const [y, m, d] = iso.split('-')
  return `${d}/${m}/${y}`
}
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <button class="btn-back" @click="router.push({ name: 'trader-orders' })">
        <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="15 18 9 12 15 6"/></svg>
        Pedidos
      </button>
    </div>

    <div v-if="loading" class="loading-state"><div class="spinner" /></div>
    <div v-else-if="error" class="empty-state"><p>{{ error }}</p><button class="btn btn--secondary" @click="loadOrder">Tentar novamente</button></div>

    <template v-else-if="order">
      <!-- Cabeçalho do pedido -->
      <div class="detail-header">
        <div class="detail-header__main">
          <div>
            <h1 class="detail-title">{{ order.clientName }}</h1>
            <p class="detail-meta">{{ order.clientPhone }} &bull; {{ formatDate(order.orderDate) }}</p>
          </div>
          <span class="badge" :class="order.status === 'OPEN' ? 'badge--open' : 'badge--closed'">
            {{ order.status === 'OPEN' ? 'Em aberto' : 'Fechado' }}
          </span>
        </div>
        <div v-if="order.notes" class="detail-notes">{{ order.notes }}</div>
        <div class="detail-actions">
          <template v-if="order.status === 'OPEN'">
            <button class="btn btn--secondary" @click="router.push({ name: 'trader-order-edit', params: { orderId: order.id } })">
              Editar
            </button>
            <button class="btn btn--primary" :disabled="closing" @click="closeOrder">
              {{ closing ? 'Fechando...' : 'Fechar Pedido' }}
            </button>
          </template>
          <button v-if="order.status === 'OPEN'" class="btn btn--danger-outline" @click="showDelete = true">Excluir</button>
        </div>
      </div>

      <!-- Legs e trucks -->
      <div v-for="leg in order.legs" :key="leg.id" class="leg-card">
        <div class="leg-card__header">
          <div>
            <span class="leg-supplier">{{ leg.supplierName }}</span>
            <span v-if="leg.supplierCity" class="leg-city"> — {{ leg.supplierCity }}</span>
          </div>
          <span class="leg-price">{{ currency(leg.supplierPricePerKg) }}/kg</span>
        </div>

        <div v-if="leg.notes" class="leg-notes">{{ leg.notes }}</div>

        <table class="trucks-table">
          <thead>
            <tr>
              <th>Placa</th>
              <th class="align-right">Quantidade</th>
              <th class="align-right">Frete</th>
              <th class="align-right">Custo</th>
              <th>Obs.</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="truck in leg.trucks" :key="truck.id">
              <td class="truck-plate">{{ truck.truckPlate }}</td>
              <td class="align-right">{{ kg(truck.quantityKg) }}</td>
              <td class="align-right">{{ truck.freightValue != null ? currency(truck.freightValue) : '—' }}</td>
              <td class="align-right">{{ currency(truck.quantityKg * leg.supplierPricePerKg) }}</td>
              <td class="text-muted">{{ truck.notes ?? '—' }}</td>
            </tr>
          </tbody>
          <tfoot>
            <tr class="leg-totals">
              <td><strong>Subtotal perna</strong></td>
              <td class="align-right"><strong>{{ kg(leg.totalKg) }}</strong></td>
              <td></td>
              <td class="align-right"><strong>{{ currency(leg.totalCost) }}</strong></td>
              <td></td>
            </tr>
          </tfoot>
        </table>
      </div>

      <!-- Resumo financeiro -->
      <div class="summary-card">
        <h2 class="summary-title">Resumo Financeiro</h2>
        <div class="summary-grid">
          <div class="summary-row">
            <span>Total de Kg</span>
            <span class="summary-value">{{ kg(order.totalKg) }}</span>
          </div>
          <div class="summary-row">
            <span>Preço/kg do Cliente</span>
            <span class="summary-value">{{ currency(order.clientPricePerKg) }}</span>
          </div>
          <div class="summary-row">
            <span>Receita Total</span>
            <span class="summary-value summary-value--positive">{{ currency(order.totalRevenue) }}</span>
          </div>
          <div class="summary-row">
            <span>Custo Total</span>
            <span class="summary-value summary-value--negative">{{ currency(order.totalCost) }}</span>
          </div>
          <div class="summary-row summary-row--highlight">
            <span><strong>Margem Bruta</strong></span>
            <span class="summary-value" :class="order.grossMargin >= 0 ? 'summary-value--positive' : 'summary-value--negative'">
              <strong>{{ currency(order.grossMargin) }}</strong>
            </span>
          </div>
        </div>
      </div>
    </template>
  </div>

  <!-- Modal exclusão -->
  <div v-if="showDelete" class="modal-overlay" @click.self="showDelete = false">
    <div class="modal modal--sm">
      <div class="modal__header">
        <h2 class="modal__title">Excluir Pedido</h2>
      </div>
      <div class="modal__body">
        <div v-if="deleteError" class="error-banner">{{ deleteError }}</div>
        <p class="confirm-text">Tem certeza que deseja excluir este pedido? Todos os dados serão removidos e esta ação não pode ser desfeita.</p>
      </div>
      <div class="modal__footer">
        <button class="btn btn--secondary" @click="showDelete = false">Cancelar</button>
        <button class="btn btn--danger" @click="confirmDelete">Excluir</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.page-container { padding: 2rem 1.5rem; max-width: 900px; margin: 0 auto; }
.page-header { margin-bottom: 1.25rem; }
.btn-back { display: inline-flex; align-items: center; gap: 0.375rem; background: none; border: none; cursor: pointer; color: var(--color-text-muted); font-family: inherit; font-size: 0.875rem; padding: 0; transition: color 0.15s; }
.btn-back:hover { color: var(--color-text); }

.detail-header { background: var(--color-card); border: 1px solid var(--color-border); border-radius: var(--radius-md); padding: 1.5rem; margin-bottom: 1rem; box-shadow: var(--shadow-card); display: flex; flex-direction: column; gap: 0.875rem; }
.detail-header__main { display: flex; align-items: flex-start; justify-content: space-between; gap: 1rem; }
.detail-title { font-size: 1.5rem; font-weight: 700; color: var(--color-text); margin: 0 0 0.25rem; }
.detail-meta { font-size: 0.875rem; color: var(--color-text-muted); margin: 0; }
.detail-notes { font-size: 0.875rem; color: var(--color-text-muted); font-style: italic; }
.detail-actions { display: flex; gap: 0.625rem; flex-wrap: wrap; }

.badge { font-size: 0.75rem; font-weight: 700; padding: 0.25rem 0.75rem; border-radius: 20px; white-space: nowrap; flex-shrink: 0; }
.badge--open { background: var(--color-primary-light); color: var(--color-primary); }
.badge--closed { background: var(--color-surface); color: var(--color-text-muted); }

.leg-card { background: var(--color-card); border: 1px solid var(--color-border); border-radius: var(--radius-md); padding: 1.25rem; margin-bottom: 0.75rem; box-shadow: var(--shadow-card); }
.leg-card__header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 0.75rem; }
.leg-supplier { font-weight: 700; color: var(--color-text); }
.leg-city { color: var(--color-text-muted); }
.leg-price { font-weight: 600; color: var(--color-primary); font-size: 0.9rem; }
.leg-notes { font-size: 0.825rem; color: var(--color-text-muted); margin-bottom: 0.75rem; font-style: italic; }

.trucks-table { width: 100%; border-collapse: collapse; font-size: 0.855rem; }
.trucks-table th { font-size: 0.72rem; font-weight: 600; color: var(--color-text-muted); text-transform: uppercase; letter-spacing: 0.04em; padding: 0.4rem 0.5rem; border-bottom: 1px solid var(--color-border); text-align: left; }
.trucks-table td { padding: 0.5rem 0.5rem; border-bottom: 1px solid var(--color-border); color: var(--color-text); }
.trucks-table tbody tr:last-child td { border-bottom: none; }
.truck-plate { font-weight: 600; font-family: monospace; letter-spacing: 0.04em; }
.align-right { text-align: right; }
.text-muted { color: var(--color-text-muted); }
.leg-totals td { padding: 0.6rem 0.5rem; border-top: 1px solid var(--color-border); background: var(--color-surface); }

.summary-card { background: var(--color-card); border: 1px solid var(--color-border); border-radius: var(--radius-md); padding: 1.5rem; margin-top: 1rem; box-shadow: var(--shadow-card); }
.summary-title { font-size: 0.95rem; font-weight: 700; color: var(--color-text); margin: 0 0 1rem; }
.summary-grid { display: flex; flex-direction: column; gap: 0.5rem; }
.summary-row { display: flex; justify-content: space-between; align-items: center; padding: 0.5rem 0.75rem; border-radius: var(--radius-sm); font-size: 0.875rem; color: var(--color-text); }
.summary-row:nth-child(odd) { background: var(--color-surface); }
.summary-row--highlight { background: var(--color-primary-light) !important; border: 1px solid var(--color-primary); }
.summary-value { font-weight: 600; }
.summary-value--positive { color: var(--color-primary); }
.summary-value--negative { color: var(--color-error); }

.btn { display: inline-flex; align-items: center; gap: 0.5rem; padding: 0.5rem 1rem; border-radius: var(--radius-sm); font-family: inherit; font-size: 0.875rem; font-weight: 600; border: none; cursor: pointer; transition: opacity 0.15s; white-space: nowrap; }
.btn--primary { background: var(--color-primary); color: #fff; }
.btn--secondary { background: var(--color-surface); color: var(--color-text); border: 1px solid var(--color-border); }
.btn--danger { background: var(--color-error); color: #fff; }
.btn--danger-outline { background: none; color: var(--color-error); border: 1px solid var(--color-error); }
.btn:disabled { opacity: 0.6; cursor: not-allowed; }
.btn:not(:disabled):hover { opacity: 0.88; }

.loading-state { display: flex; justify-content: center; padding: 4rem; }
.spinner { width: 32px; height: 32px; border: 3px solid var(--color-border); border-top-color: var(--color-primary); border-radius: 50%; animation: spin 0.7s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
.empty-state { text-align: center; padding: 4rem 2rem; color: var(--color-text-muted); display: flex; flex-direction: column; align-items: center; gap: 1rem; }

.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.45); display: flex; align-items: center; justify-content: center; z-index: 500; padding: 1rem; }
.modal { background: var(--color-card); border: 1px solid var(--color-border); border-radius: var(--radius-md); width: 100%; max-width: 480px; }
.modal--sm { max-width: 380px; }
.modal__header { display: flex; align-items: center; padding: 1.25rem 1.5rem; border-bottom: 1px solid var(--color-border); }
.modal__title { font-size: 1rem; font-weight: 700; color: var(--color-text); margin: 0; }
.modal__body { padding: 1.5rem; display: flex; flex-direction: column; gap: 0.75rem; }
.modal__footer { padding: 1rem 1.5rem; border-top: 1px solid var(--color-border); display: flex; justify-content: flex-end; gap: 0.75rem; }
.confirm-text { color: var(--color-text); font-size: 0.9rem; line-height: 1.5; margin: 0; }
.error-banner { background: var(--color-error-light); color: var(--color-error); border: 1px solid var(--color-error); border-radius: var(--radius-sm); padding: 0.625rem 0.875rem; font-size: 0.85rem; }

@media (max-width: 600px) {
  .page-container { padding: 1.25rem 1rem; }
  .detail-header__main { flex-direction: column; }
  .detail-actions { flex-direction: column; }
  .trucks-table th:nth-child(4), .trucks-table td:nth-child(4),
  .trucks-table th:nth-child(5), .trucks-table td:nth-child(5) { display: none; }
}
</style>
```

- [ ] **Step 2: Run type check**

```bash
npx vue-tsc --noEmit
```

Expected: `ClientOrderDetailView.vue` compiles without errors.

- [ ] **Step 3: Commit**

```bash
git add src/views/trader/ClientOrderDetailView.vue
git commit -m "feat: add ClientOrderDetailView with leg/truck breakdown and close/delete actions"
```

---

## Task 7: Update router/index.ts + TraderLayout.vue

**Files:**
- Modify: `src/router/index.ts`
- Modify: `src/layouts/TraderLayout.vue`

- [ ] **Step 1: Replace trader routes in router/index.ts**

Locate the trader routes block (the `path: '/trader'` object, lines ~211–248). Replace the `children` array inside it with:

```typescript
      children: [
        { path: '', redirect: { name: 'trader-dashboard' } },
        {
          path: 'dashboard',
          name: 'trader-dashboard',
          component: () => import('@/views/trader/TraderDashboardView.vue')
        },
        {
          path: 'suppliers',
          name: 'trader-suppliers',
          component: () => import('@/views/trader/TradingSuppliersView.vue')
        },
        {
          path: 'clients',
          name: 'trader-clients',
          component: () => import('@/views/trader/TradingClientsView.vue')
        },
        {
          path: 'orders',
          name: 'trader-orders',
          component: () => import('@/views/trader/ClientOrdersView.vue')
        },
        {
          path: 'orders/new',
          name: 'trader-orders-new',
          component: () => import('@/views/trader/ClientOrderFormView.vue')
        },
        {
          path: 'orders/:orderId',
          name: 'trader-order-detail',
          component: () => import('@/views/trader/ClientOrderDetailView.vue')
        },
        {
          path: 'orders/:orderId/edit',
          name: 'trader-order-edit',
          component: () => import('@/views/trader/ClientOrderFormView.vue')
        },
      ]
```

- [ ] **Step 2: Update navItems in TraderLayout.vue**

Locate the `const navItems = [...]` array in `<script setup>`. Replace it with:

```typescript
const navItems = [
  {
    name: 'trader-dashboard',
    label: 'Dashboard',
    icon: `<svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="14" y="14" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/></svg>`
  },
  {
    name: 'trader-suppliers',
    label: 'Fornecedores',
    icon: `<svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>`
  },
  {
    name: 'trader-clients',
    label: 'Clientes',
    icon: `<svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>`
  },
  {
    name: 'trader-orders',
    label: 'Pedidos',
    icon: `<svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/><polyline points="10 9 9 9 8 9"/></svg>`
  },
]
```

Also update `isActive` to handle child routes. The current `isActive` function checks only `route.name === name`, which won't highlight "Pedidos" when viewing an order detail. Replace it with:

```typescript
function isActive(name: string) {
  if (route.name === name) return true
  // highlight "Pedidos" for all order-related routes
  if (name === 'trader-orders') {
    return ['trader-orders', 'trader-orders-new', 'trader-order-detail', 'trader-order-edit'].includes(route.name as string)
  }
  return false
}
```

- [ ] **Step 3: Run type check**

```bash
npx vue-tsc --noEmit
```

Expected: no errors in router or layout. At this point the only errors should be in the old views (PurchaseLot*).

- [ ] **Step 4: Commit**

```bash
git add src/router/index.ts src/layouts/TraderLayout.vue
git commit -m "feat: update trader routes and nav for clients/orders, remove lot routes"
```

---

## Task 8: Delete old views + final type check

**Files:**
- Delete: `src/views/trader/PurchaseLotsView.vue`
- Delete: `src/views/trader/PurchaseLotFormView.vue`
- Delete: `src/views/trader/PurchaseLotDetailView.vue`

- [ ] **Step 1: Delete old view files**

```bash
rm src/views/trader/PurchaseLotsView.vue
rm src/views/trader/PurchaseLotFormView.vue
rm src/views/trader/PurchaseLotDetailView.vue
```

- [ ] **Step 2: Run full type check — must be clean**

```bash
npx vue-tsc --noEmit
```

Expected: **no errors**. If errors remain, they are genuine type mismatches to fix before committing.

Common errors and fixes:
- `Property 'totalLots' does not exist on type 'TradingDashboardResponse'` → check `TraderDashboardView.vue` for any missed field rename from Task 2.
- `Module not found: PurchaseLotsView.vue` → verify router has no remaining references to deleted views.

- [ ] **Step 3: Final commit**

```bash
git add -A
git commit -m "feat: remove old PurchaseLot views, complete trading flow redesign frontend"
```

---

## Verification Checklist

After all tasks, manually verify the golden paths in the browser:

- [ ] Dashboard loads with new KPI fields (no console errors)
- [ ] Dashboard "Novo Pedido" button navigates to order form
- [ ] Dashboard stat card navigates to Pedidos list / Clientes list
- [ ] Fornecedores CRUD still works (no regression)
- [ ] Clientes list loads, search works, create/edit/delete modal works
- [ ] Delete blocked (409) shows the "possui pedidos" message
- [ ] Pedidos list loads, status filter works, client filter works, pagination appears when > 12 results
- [ ] Novo Pedido: add multiple legs, each with multiple trucks; live summary updates
- [ ] Submit Novo Pedido → redirected to list, order appears
- [ ] Pedido detail: legs with trucks display correctly, totals match summary
- [ ] Fechar Pedido: status changes to Fechado, edit/delete buttons disappear
- [ ] Edit order (OPEN): form pre-populated correctly, save updates order
- [ ] `npx vue-tsc --noEmit` exits 0
