# Design: Vinculação CustomerOrder → PurchaseLot

**Data:** 2026-05-13  
**Domínio:** trading (modo comprador)  
**Status:** aprovado

---

## Contexto

No modo comprador, o usuário cadastra fornecedores (`TradingSupplier`) e lotes de compra (`PurchaseLot`). A regra de negócio é: um lote de compra só existe porque um cliente fez um pedido. Atualmente não há entidade que represente esse pedido, e `PurchaseLot` pode ser criado sem qualquer vínculo com demanda de cliente.

---

## Objetivo

Criar a entidade `CustomerOrder` e tornar obrigatória sua vinculação ao criar um `PurchaseLot`, garantindo a regra de negócio no nível do banco de dados.

---

## Decisões

| Questão | Decisão |
|---|---|
| Modelo de pedido | Nova entidade `CustomerOrder` (não reaproveitamento de `LotSale`) |
| Cardinalidade | 1:1 — um pedido gera exatamente um lote de compra |
| Status do pedido | Derivado (sem coluna própria): sem lote = `PENDING`; com lote = `FULFILLED` |
| Direção do relacionamento | Unidirecional: `PurchaseLot` → `CustomerOrder` |
| Enforcement | FK `NOT NULL UNIQUE` em `purchase_lots.customer_order_id` |

---

## Modelo de Dados

### Nova tabela `customer_orders`

```sql
customer_orders
├── id                UUID         PK
├── account_id        UUID         FK (accounts) NOT NULL
├── customer_name     VARCHAR(150) NOT NULL
├── customer_phone    VARCHAR(20)  nullable
├── customer_document VARCHAR(20)  nullable       -- CPF ou CNPJ
├── quantity_kg       DECIMAL(12,4) NOT NULL
├── price_per_kg      DECIMAL(10,4) nullable      -- preço acordado com cliente
├── product           VARCHAR(100) NOT NULL       -- ex: "Soja", "Milho"
├── order_date        DATE         NOT NULL
├── delivery_deadline DATE         nullable
├── notes             TEXT         nullable
├── created_at        TIMESTAMP    NOT NULL
└── updated_at        TIMESTAMP    NOT NULL
```

### Alteração em `purchase_lots`

```sql
ALTER TABLE purchase_lots
  ADD COLUMN customer_order_id UUID NOT NULL
    REFERENCES customer_orders(id),
  ADD CONSTRAINT uq_purchase_lots_customer_order UNIQUE (customer_order_id);
```

A constraint `UNIQUE` garante 1:1 no banco.

---

## Modelo Java

### `CustomerOrder` (nova entidade)

Pacote: `br.com.hadryan.agro.manager.domain.trading`

Campos JPA:
- `@Id UUID id`
- `@ManyToOne(fetch=LAZY) Account account`
- `String customerName`
- `String customerPhone` (nullable)
- `String customerDocument` (nullable)
- `BigDecimal quantityKg`
- `BigDecimal pricePerKg` (nullable)
- `String product`
- `LocalDate orderDate`
- `LocalDate deliveryDeadline` (nullable)
- `String notes` (nullable)
- `LocalDateTime createdAt / updatedAt` com `@PrePersist / @PreUpdate`

Sem campo `status` — calculado no response.

### Alteração em `PurchaseLot`

```java
@OneToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "customer_order_id", nullable = false, unique = true)
private CustomerOrder customerOrder;
```

Unidirecional: `CustomerOrder` não conhece `PurchaseLot`.

---

## Status Derivado

`CustomerOrderStatus` (enum usado apenas no response, sem coluna):

```java
public enum CustomerOrderStatus {
    PENDING,    // nenhum PurchaseLot referencia este pedido
    FULFILLED   // existe um PurchaseLot vinculado
}
```

O repository de `CustomerOrder` expõe uma query que verifica existência de lote vinculado:

```java
@Query("SELECT COUNT(pl) > 0 FROM PurchaseLot pl WHERE pl.customerOrder.id = :orderId")
boolean existsPurchaseLotForOrder(UUID orderId);
```

Ou, na listagem, um `LEFT JOIN` para trazer o status em bulk.

---

## APIs

### Novo recurso: `CustomerOrderController`

Base path: `/accounts/{accountId}/trading/orders`

| Método | Path | Descrição |
|---|---|---|
| `POST` | `/orders` | Cria pedido (status inicial: `PENDING`) |
| `GET` | `/orders` | Lista pedidos com status derivado; filtro `?status=PENDING\|FULFILLED` |
| `GET` | `/orders/{orderId}` | Detalhe do pedido |
| `PUT` | `/orders/{orderId}` | Atualiza pedido (apenas se `PENDING`) |
| `DELETE` | `/orders/{orderId}` | Remove pedido (apenas se `PENDING`) |

### Alteração em `PurchaseLotRequest`

Adicionar campo obrigatório:

```java
@NotNull(message = "Pedido do cliente é obrigatório")
UUID customerOrderId
```

### Validações ao criar `PurchaseLot`

1. `CustomerOrder` existe e pertence à conta (`accountId`)
2. `CustomerOrder` está `PENDING` (nenhum lote vinculado ainda)
3. Caso falhe: `BusinessException` com mensagem descritiva

---

## Responses

### `CustomerOrderResponse`

```java
record CustomerOrderResponse(
    UUID id,
    String customerName,
    String customerPhone,
    String customerDocument,
    BigDecimal quantityKg,
    BigDecimal pricePerKg,
    String product,
    LocalDate orderDate,
    LocalDate deliveryDeadline,
    CustomerOrderStatus status,  // derivado
    String notes,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
)
```

### `PurchaseLotDetailResponse` / `PurchaseLotSummaryResponse`

Adicionar campo `customerOrderId` (UUID) e `customerName` (String) para referência rápida.

---

## Fluxo Operacional

```
1. Usuário cadastra CustomerOrder          → status PENDING
2. Usuário cria PurchaseLot               → informa customerOrderId
   Service valida: pedido existe, PENDING → associa, salva
                                          → CustomerOrder status vira FULFILLED
3. Listagem de CustomerOrder              → exibe PENDING/FULFILLED via query
4. Deleção de CustomerOrder               → só permitida se PENDING
5. Atualização de CustomerOrder           → só permitida se PENDING
```

---

## Arquivos Afetados

**Novos:**
- `CustomerOrder.java`
- `CustomerOrderRepository.java`
- `CustomerOrderRequest.java`
- `CustomerOrderResponse.java`
- `CustomerOrderStatus.java`
- `CustomerOrderService.java` (interface)
- `CustomerOrderServiceImpl.java`
- `CustomerOrderController.java`

**Modificados:**
- `PurchaseLot.java` — adicionar campo `customerOrder`
- `PurchaseLotRequest.java` — adicionar `customerOrderId`
- `PurchaseLotService.java` / `PurchaseLotServiceImpl.java` — validações ao criar lote
- `PurchaseLotDetailResponse.java` — adicionar `customerOrderId`, `customerName`
- `PurchaseLotSummaryResponse.java` — adicionar `customerOrderId`, `customerName`
- Migration SQL (Flyway/Liquibase)

---

## Restrições e Invariantes

- `PurchaseLot` sem `CustomerOrder` não existe (NOT NULL no banco)
- Um `CustomerOrder` vincula no máximo um `PurchaseLot` (UNIQUE no banco)
- `CustomerOrder` só pode ser editado/deletado enquanto `PENDING`
- `CustomerOrder` e `PurchaseLot` devem pertencer à mesma `account`
- Se o `PurchaseLot` for deletado, `CustomerOrder` volta automaticamente a `PENDING` (status é derivado, sem coluna — nenhuma ação extra necessária)
- Deleção de `PurchaseLot` com status `CLOSED` deve ser bloqueada pelo service existente; regra se aplica apenas a lotes `OPEN`
