# Redesign do Fluxo de Compra/Venda (Modo Comprador)

**Data:** 2026-05-14
**Status:** Aprovado

---

## Contexto e Problema

O modelo atual modela "compra de fornecedor" e "venda para comprador" como duas operações independentes, cada uma com seu próprio conjunto de caminhões. Isso não reflete o negócio real:

- O fluxo real começa com um **pedido de um cliente**
- O comprador vai atrás de **fornecedores** para atender o pedido
- O **mesmo caminhão** que carrega na lavoura do fornecedor entrega diretamente ao cliente
- Não há estoque intermediário nem dois conjuntos de caminhões

O modelo atual (`PurchaseLot → LotSale`, cada um com seus trucks) é descartado por completo. O banco está zerado — sem migração de dados.

---

## Objetivo

Redesenhar o módulo de trading com um modelo de dados que reflete o fluxo real:

**cliente faz pedido → comprador busca fornecedores → caminhões saem da lavoura e entregam ao cliente**

---

## Modelo de Dados

### Entidades preservadas sem alteração

- `TradingSupplier` — fornecedores já cadastrados

### Entidades removidas

- `purchase_lots`, `purchase_trucks`, `lot_sales`, `lot_sale_trucks`

### Entidades novas

#### `trading_clients` — cadastro de clientes

| Coluna | Tipo | Restrições |
|---|---|---|
| id | UUID | PK, gen_random_uuid() |
| account_id | UUID | FK → accounts, NOT NULL |
| name | VARCHAR(150) | NOT NULL |
| phone | VARCHAR(20) | NOT NULL |
| city | VARCHAR(100) | nullable |
| notes | TEXT | nullable |
| created_at | TIMESTAMP | NOT NULL DEFAULT NOW() |
| updated_at | TIMESTAMP | NOT NULL |

Índice: `idx_trading_clients_account ON trading_clients(account_id)`

---

#### `client_orders` — pedido central (substitui PurchaseLot + LotSale)

| Coluna | Tipo | Restrições |
|---|---|---|
| id | UUID | PK, gen_random_uuid() |
| account_id | UUID | FK → accounts, NOT NULL |
| client_id | UUID | FK → trading_clients, NOT NULL |
| order_date | DATE | NOT NULL |
| client_price_per_kg | DECIMAL(12,2) | NOT NULL |
| status | VARCHAR(10) | NOT NULL DEFAULT 'OPEN' |
| notes | TEXT | nullable |
| created_at | TIMESTAMP | NOT NULL DEFAULT NOW() |
| updated_at | TIMESTAMP | NOT NULL |

Índice: `idx_client_orders_account ON client_orders(account_id)`
Índice: `idx_client_orders_client ON client_orders(client_id)`

---

#### `order_supplier_legs` — perna de fornecedor de um pedido

| Coluna | Tipo | Restrições |
|---|---|---|
| id | UUID | PK, gen_random_uuid() |
| order_id | UUID | FK → client_orders ON DELETE CASCADE, NOT NULL |
| supplier_id | UUID | FK → trading_suppliers, NOT NULL |
| supplier_price_per_kg | DECIMAL(12,2) | NOT NULL |
| notes | TEXT | nullable |
| created_at | TIMESTAMP | NOT NULL DEFAULT NOW() |

Índice: `idx_order_supplier_legs_order ON order_supplier_legs(order_id)`

---

#### `order_trucks` — caminhão de uma perna (único registro por veículo)

| Coluna | Tipo | Restrições |
|---|---|---|
| id | UUID | PK, gen_random_uuid() |
| leg_id | UUID | FK → order_supplier_legs ON DELETE CASCADE, NOT NULL |
| truck_plate | VARCHAR(10) | NOT NULL |
| quantity_kg | DECIMAL(12,2) | NOT NULL |
| freight_value | DECIMAL(12,2) | nullable |
| notes | TEXT | nullable |
| created_at | TIMESTAMP | NOT NULL DEFAULT NOW() |

Índice: `idx_order_trucks_leg ON order_trucks(leg_id)`

---

### Hierarquia

```
ClientOrder
  └── OrderSupplierLeg (1..N por pedido)
        └── OrderTruck (1..N por leg)
```

### Cálculo de margem por pedido

```
total_kg      = Σ truck.quantity_kg  (todos os trucks do pedido)
receita       = total_kg × order.client_price_per_kg
custo_produto = Σ (truck.quantity_kg × leg.supplier_price_per_kg)
custo_frete   = Σ truck.freight_value (nullable → 0 quando ausente)
custo_total   = custo_produto + custo_frete
margem_bruta  = receita − custo_total
```

---

## API

Base: `/accounts/{accountId}/trading`

### Clientes

| Método | Rota | Descrição |
|---|---|---|
| POST | `/clients` | Cadastrar cliente |
| GET | `/clients` | Listar (filtro opcional: `?search=nome`) |
| GET | `/clients/{clientId}` | Detalhes |
| PUT | `/clients/{clientId}` | Editar |
| DELETE | `/clients/{clientId}` | Excluir (bloqueado se tiver pedidos) |

### Pedidos

| Método | Rota | Descrição |
|---|---|---|
| POST | `/orders` | Criar pedido com legs+trucks inline |
| GET | `/orders` | Listar (filtros: `status`, `clientId`; paginado) |
| GET | `/orders/{orderId}` | Detalhes completos |
| PUT | `/orders/{orderId}` | Editar pedido — substitui todas as legs+trucks |
| PATCH | `/orders/{orderId}/close` | Encerrar pedido manualmente |
| DELETE | `/orders/{orderId}` | Excluir pedido (bloqueado se CLOSED) |

### Legs de um pedido

| Método | Rota | Descrição |
|---|---|---|
| POST | `/orders/{orderId}/legs` | Adicionar leg com trucks (pedido OPEN) |
| DELETE | `/orders/{orderId}/legs/{legId}` | Remover leg e seus trucks |

### Dashboard

| Método | Rota | Descrição |
|---|---|---|
| GET | `/dashboard` | Métricas agregadas do novo modelo |

---

### Payloads principais

**Criar pedido (`POST /orders`)**
```json
{
  "clientId": "uuid",
  "orderDate": "2026-05-14",
  "clientPricePerKg": 1.20,
  "notes": "opcional",
  "legs": [
    {
      "supplierId": "uuid",
      "supplierPricePerKg": 0.85,
      "notes": "opcional",
      "trucks": [
        {
          "truckPlate": "ABC1D23",
          "quantityKg": 3000.00,
          "freightValue": 150.00,
          "notes": null
        }
      ]
    }
  ]
}
```

**Adicionar leg (`POST /orders/{orderId}/legs`)**
```json
{
  "supplierId": "uuid",
  "supplierPricePerKg": 0.90,
  "notes": "opcional",
  "trucks": [
    { "truckPlate": "XYZ9H87", "quantityKg": 2000.00, "freightValue": null }
  ]
}
```

---

## Regras de Negócio

### Status

- Pedido nasce como `OPEN`
- Encerramento é **manual** via `PATCH /close` — não há auto-close por volume
- Pedido `CLOSED` rejeita: edição, adição de legs, remoção de legs, exclusão

### Proteções de exclusão

| Tentativa | Resultado |
|---|---|
| Excluir cliente com pedidos | 409 CONFLICT |
| Excluir fornecedor com legs vinculadas | 409 CONFLICT |
| Excluir pedido CLOSED | 409 CONFLICT |
| Remover leg | Trucks em cascata (orphanRemoval) |
| Excluir pedido OPEN sem legs ou com legs | Deleta tudo em cascata |

### Validações de request

| Campo | Regra |
|---|---|
| `clientId` | Deve existir e pertencer à conta |
| `supplierId` | Deve existir e pertencer à conta |
| `clientPricePerKg` | > 0 |
| `supplierPricePerKg` | > 0 |
| `quantityKg` | > 0 |
| `freightValue` | >= 0 (opcional, null permitido) |
| `truckPlate` | max 10 chars, obrigatório |
| `legs` | mínimo 1 na criação |
| `trucks` | mínimo 1 por leg |

### Dashboard — métricas

- Total de pedidos (abertos / fechados)
- Total de kg entregues
- Receita total
- Custo total (produto + frete)
- Margem bruta
- Total de clientes cadastrados
- Total de fornecedores cadastrados

---

## Frontend

### Removido

- `PurchaseLotFormView.vue`
- `PurchaseLotDetailView.vue`
- Modal/formulário de `LotSale` embutido no detalhe
- Lista de lotes de compra
- Tipos `PurchaseLot*`, `LotSale*` em `tradingService.ts`

### Novo

#### Tela de Clientes

- Lista com busca por nome, botão de novo cliente
- Formulário de cadastro/edição: nome (obrigatório), telefone (obrigatório), cidade (opcional)
- Padrão visual e de navegação idêntico ao de Fornecedores já existente

#### Tela de Pedidos — Lista

- Cards ou tabela paginada com filtro por status e cliente
- Exibe: cliente, data, kg total, receita, margem bruta, status

#### Tela de Pedidos — Formulário (tela única)

Formulário aninhado em três níveis:

1. **Cabeçalho do pedido:** cliente (select), data, preço/kg do cliente, notas
2. **Legs dinâmicas:** botão "Adicionar Fornecedor"; cada leg tem fornecedor (select), preço/kg do fornecedor, notas
3. **Trucks dinâmicos por leg:** botão "Adicionar Caminhão" dentro de cada leg; cada truck tem placa, kg, frete (opcional), notas

Resumo ao vivo (similar ao atual): total de kg, receita estimada, custo estimado, margem estimada.

#### Tela de Pedidos — Detalhe

- Dados do pedido e do cliente
- Tabela de legs com fornecedor, preço/kg, subtotal
- Dentro de cada leg: tabela de trucks com placa, kg, frete
- Totais e margem bruta calculados

#### `tradingService.ts`

Substituir todos os tipos antigos por:
- `TradingClientRequest`, `TradingClientResponse`
- `ClientOrderRequest`, `ClientOrderResponse`, `ClientOrderSummaryResponse`
- `OrderSupplierLegRequest`, `OrderSupplierLegResponse`
- `OrderTruckRequest`, `OrderTruckResponse`

---

## Migrações Flyway

Com banco zerado, o Flyway executa todas as migrations em ordem do zero. A sequência completa relevante:

| Versão | Descrição |
|---|---|
| V14 | Cria `purchase_trucks` (existente) |
| V15 | Cria `lot_sales` (existente) |
| V16 | Cria `lot_sale_trucks` (existente) |
| V17 | Adiciona `freight_value` em `purchase_trucks` (existente — roda normalmente) |
| V18 | Drop das 4 tabelas antigas na ordem correta: `lot_sale_trucks`, `lot_sales`, `purchase_trucks`, `purchase_lots` |
| V19 | Criar `trading_clients` |
| V20 | Criar `client_orders` |
| V21 | Criar `order_supplier_legs` |
| V22 | Criar `order_trucks` |

A sequência é safe: V14 cria a tabela, V17 a altera, V18 a dropa. Nenhuma migration precisa ser reescrita.

---

## O que NÃO muda

- Autenticação, autorização e multi-tenancy
- `TradingSupplier` (entidade, CRUD, endpoints)
- Padrão de respostas (`ApiResponse`, `PageResponse`)
- Infraestrutura (JWT, OAuth2, email, etc.)
