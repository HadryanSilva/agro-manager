# Customer Order → Purchase Lot Link Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Criar a entidade `CustomerOrder` e tornar obrigatória sua vinculação ao criar um `PurchaseLot`, garantindo no banco que um lote só existe quando há um pedido de cliente.

**Architecture:** Nova entidade `CustomerOrder` com CRUD próprio. `PurchaseLot` ganha FK `customer_order_id NOT NULL UNIQUE`. Status de `CustomerOrder` é derivado (sem coluna): PENDING = sem lote, FULFILLED = com lote. Relação unidirecional: `PurchaseLot` → `CustomerOrder`.

**Tech Stack:** Spring Boot 3, Spring Data JPA, Hibernate, Flyway, JUnit 5, AssertJ, MockMvc

---

## File Map

**Novos:**
- `src/main/java/.../trading/CustomerOrderStatus.java`
- `src/main/java/.../trading/CustomerOrder.java`
- `src/main/java/.../trading/CustomerOrderRepository.java`
- `src/main/java/.../trading/CustomerOrderRequest.java`
- `src/main/java/.../trading/CustomerOrderResponse.java`
- `src/main/java/.../trading/CustomerOrderService.java`
- `src/main/java/.../trading/CustomerOrderController.java`
- `src/main/java/.../trading/CreatePurchaseLotRequest.java`
- `src/main/resources/db/migration/V17__create_customer_orders_table.sql`
- `src/main/resources/db/migration/V18__add_customer_order_id_to_purchase_lots.sql`
- `src/test/java/.../CustomerOrderIntegrationTest.java`

**Modificados:**
- `src/main/java/.../trading/PurchaseLot.java`
- `src/main/java/.../trading/PurchaseLotRepository.java`
- `src/main/java/.../trading/PurchaseLotSummaryResponse.java`
- `src/main/java/.../trading/PurchaseLotDetailResponse.java`
- `src/main/java/.../trading/PurchaseLotService.java`
- `src/main/java/.../trading/PurchaseLotController.java`
- `src/test/java/.../MockMvcIntegrationTestBase.java`

> Prefixo omitido: `src/main/java/br/com/hadryan/agro/manager/domain/`

---

## Task 1: CustomerOrderStatus enum + V17 migration + CustomerOrder entity

**Files:**
- Create: `src/main/resources/db/migration/V17__create_customer_orders_table.sql`
- Create: `src/main/java/br/com/hadryan/agro/manager/domain/trading/CustomerOrderStatus.java`
- Create: `src/main/java/br/com/hadryan/agro/manager/domain/trading/CustomerOrder.java`

- [ ] **Step 1: Criar migration V17**

```sql
-- Pedidos de clientes do modo comprador.
-- Um pedido representa a demanda que origina um lote de compra.
-- Status é derivado: PENDING = sem lote, FULFILLED = lote vinculado.

CREATE TABLE customer_orders (
    id                UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id        UUID           NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    customer_name     VARCHAR(150)   NOT NULL,
    customer_phone    VARCHAR(20),
    customer_document VARCHAR(20),
    quantity_kg       DECIMAL(12, 4) NOT NULL,
    price_per_kg      DECIMAL(10, 4),
    product           VARCHAR(100)   NOT NULL,
    order_date        DATE           NOT NULL,
    delivery_deadline DATE,
    notes             TEXT,
    created_at        TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP      NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_customer_orders_account      ON customer_orders (account_id);
CREATE INDEX idx_customer_orders_account_date ON customer_orders (account_id, order_date DESC);
```

- [ ] **Step 2: Criar enum CustomerOrderStatus**

```java
package br.com.hadryan.agro.manager.domain.trading;

public enum CustomerOrderStatus {
    PENDING,   // nenhum PurchaseLot vinculado
    FULFILLED  // PurchaseLot vinculado
}
```

- [ ] **Step 3: Criar entidade CustomerOrder**

```java
package br.com.hadryan.agro.manager.domain.trading;

import br.com.hadryan.agro.manager.domain.account.Account;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
@Table(name = "customer_orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(name = "customer_name", nullable = false, length = 150)
    private String customerName;

    @Column(name = "customer_phone", length = 20)
    private String customerPhone;

    @Column(name = "customer_document", length = 20)
    private String customerDocument;

    @Column(name = "quantity_kg", nullable = false, precision = 12, scale = 4)
    private BigDecimal quantityKg;

    @Column(name = "price_per_kg", precision = 10, scale = 4)
    private BigDecimal pricePerKg;

    @Column(nullable = false, length = 100)
    private String product;

    @Column(name = "order_date", nullable = false)
    private LocalDate orderDate;

    @Column(name = "delivery_deadline")
    private LocalDate deliveryDeadline;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now(ZoneOffset.UTC);
        updatedAt = LocalDateTime.now(ZoneOffset.UTC);
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now(ZoneOffset.UTC);
    }
}
```

- [ ] **Step 4: Verificar que a migration roda**

```bash
mvn flyway:migrate -Dflyway.url=... # ou simplesmente:
mvn test -pl . -Dtest=AgroManagerApplicationTests
```

Expected: BUILD SUCCESS (migration V17 aplicada).

- [ ] **Step 5: Commit**

```bash
git add src/main/resources/db/migration/V17__create_customer_orders_table.sql \
        src/main/java/br/com/hadryan/agro/manager/domain/trading/CustomerOrderStatus.java \
        src/main/java/br/com/hadryan/agro/manager/domain/trading/CustomerOrder.java
git commit -m "feat(trading): add CustomerOrder entity and V17 migration"
```

---

## Task 2: CustomerOrderRepository + Request + Response

**Files:**
- Create: `src/main/java/br/com/hadryan/agro/manager/domain/trading/CustomerOrderRepository.java`
- Create: `src/main/java/br/com/hadryan/agro/manager/domain/trading/CustomerOrderRequest.java`
- Create: `src/main/java/br/com/hadryan/agro/manager/domain/trading/CustomerOrderResponse.java`

- [ ] **Step 1: Criar CustomerOrderRepository**

```java
package br.com.hadryan.agro.manager.domain.trading;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, UUID> {

    List<CustomerOrder> findByAccountIdOrderByOrderDateDesc(UUID accountId);

    Optional<CustomerOrder> findByIdAndAccountId(UUID id, UUID accountId);

    boolean existsByIdAndAccountId(UUID id, UUID accountId);
}
```

- [ ] **Step 2: Criar CustomerOrderRequest**

```java
package br.com.hadryan.agro.manager.domain.trading;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CustomerOrderRequest(

        @NotBlank(message = "Nome do cliente é obrigatório")
        String customerName,

        String customerPhone,

        String customerDocument,

        @NotNull(message = "Quantidade em Kg é obrigatória")
        @DecimalMin(value = "0.0001", message = "Quantidade deve ser maior que zero")
        BigDecimal quantityKg,

        BigDecimal pricePerKg,

        @NotBlank(message = "Produto é obrigatório")
        String product,

        @NotNull(message = "Data do pedido é obrigatória")
        LocalDate orderDate,

        LocalDate deliveryDeadline,

        String notes
) {
}
```

- [ ] **Step 3: Criar CustomerOrderResponse**

```java
package br.com.hadryan.agro.manager.domain.trading;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record CustomerOrderResponse(
        UUID id,
        String customerName,
        String customerPhone,
        String customerDocument,
        BigDecimal quantityKg,
        BigDecimal pricePerKg,
        String product,
        LocalDate orderDate,
        LocalDate deliveryDeadline,
        CustomerOrderStatus status,
        String notes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static CustomerOrderResponse from(CustomerOrder o, CustomerOrderStatus status) {
        return new CustomerOrderResponse(
                o.getId(),
                o.getCustomerName(),
                o.getCustomerPhone(),
                o.getCustomerDocument(),
                o.getQuantityKg(),
                o.getPricePerKg(),
                o.getProduct(),
                o.getOrderDate(),
                o.getDeliveryDeadline(),
                status,
                o.getNotes(),
                o.getCreatedAt(),
                o.getUpdatedAt()
        );
    }
}
```

- [ ] **Step 4: Commit**

```bash
git add src/main/java/br/com/hadryan/agro/manager/domain/trading/CustomerOrderRepository.java \
        src/main/java/br/com/hadryan/agro/manager/domain/trading/CustomerOrderRequest.java \
        src/main/java/br/com/hadryan/agro/manager/domain/trading/CustomerOrderResponse.java
git commit -m "feat(trading): add CustomerOrder repository, request, and response records"
```

---

## Task 3: Escrever testes de integração para CustomerOrder (falharão até Task 4-5)

**Files:**
- Create: `src/test/java/br/com/hadryan/agro/manager/CustomerOrderIntegrationTest.java`

- [ ] **Step 1: Criar CustomerOrderIntegrationTest**

```java
package br.com.hadryan.agro.manager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CustomerOrderIntegrationTest extends MockMvcIntegrationTestBase {

    private String token;
    private String accountId;

    @BeforeEach
    void setUp() throws Exception {
        String email = uniqueEmail();
        token = registerAndGetToken(email, "senha123");
        accountId = createAccount(token, "Conta Teste");
    }

    @Test
    void createOrder_returnsCreatedWithPendingStatus() throws Exception {
        Map<String, Object> payload = orderPayload();

        mockMvc.perform(post("/accounts/" + accountId + "/trading/orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.customerName").value("João Silva"))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.product").value("Soja"));
    }

    @Test
    void listOrders_returnsAllWithDerivedStatus() throws Exception {
        createOrder(token, accountId);

        mockMvc.perform(get("/accounts/" + accountId + "/trading/orders")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].status").value("PENDING"));
    }

    @Test
    void listOrders_filterByStatus_returnOnlyMatching() throws Exception {
        createOrder(token, accountId);

        mockMvc.perform(get("/accounts/" + accountId + "/trading/orders?status=FULFILLED")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));

        mockMvc.perform(get("/accounts/" + accountId + "/trading/orders?status=PENDING")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    void getOrderById_returnsPendingOrder() throws Exception {
        String orderId = createOrder(token, accountId);

        mockMvc.perform(get("/accounts/" + accountId + "/trading/orders/" + orderId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(orderId))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    void updateOrder_whenPending_returnsOk() throws Exception {
        String orderId = createOrder(token, accountId);
        Map<String, Object> updated = orderPayload();
        updated.put("customerName", "Maria Santos");

        mockMvc.perform(put("/accounts/" + accountId + "/trading/orders/" + orderId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.customerName").value("Maria Santos"));
    }

    @Test
    void deleteOrder_whenPending_returnsNoContent() throws Exception {
        String orderId = createOrder(token, accountId);

        mockMvc.perform(delete("/accounts/" + accountId + "/trading/orders/" + orderId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/accounts/" + accountId + "/trading/orders/" + orderId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void createOrder_missingRequiredFields_returnsBadRequest() throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("customerName", "João");
        // Missing: quantityKg, product, orderDate

        mockMvc.perform(post("/accounts/" + accountId + "/trading/orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Map<String, Object> orderPayload() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("customerName", "João Silva");
        payload.put("customerPhone", "11999999999");
        payload.put("customerDocument", "123.456.789-00");
        payload.put("quantityKg", 5000.0);
        payload.put("pricePerKg", 2.50);
        payload.put("product", "Soja");
        payload.put("orderDate", "2026-05-10");
        payload.put("deliveryDeadline", "2026-06-10");
        return payload;
    }

    protected String createOrder(String token, String accountId) throws Exception {
        var result = mockMvc.perform(post("/accounts/" + accountId + "/trading/orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderPayload())))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("id").asText();
    }
}
```

- [ ] **Step 2: Rodar testes e confirmar que falham com 404**

```bash
mvn test -Dtest=CustomerOrderIntegrationTest
```

Expected: FAIL — `Status expected:<201> but was:<404>` (controller não existe ainda).

- [ ] **Step 3: Commit**

```bash
git add src/test/java/br/com/hadryan/agro/manager/CustomerOrderIntegrationTest.java
git commit -m "test(trading): add failing integration tests for CustomerOrder CRUD"
```

---

## Task 4: CustomerOrderService

**Files:**
- Create: `src/main/java/br/com/hadryan/agro/manager/domain/trading/CustomerOrderService.java`

**Nota:** O `CustomerOrderService` precisa de `PurchaseLotRepository` para derivar o status. O `PurchaseLotRepository` ainda não tem os métodos necessários — serão adicionados na Task 8. Por enquanto, declare o campo mas use um placeholder temporário `false` no existsByCustomerOrderId até a Task 8. Ou, para evitar o ciclo, adicione já na Task 8 e volte aqui.

**Ordem recomendada:** Implementar CustomerOrderService completo agora. Os métodos `existsByCustomerOrderId` e `findCustomerOrderIdsByAccountId` do `PurchaseLotRepository` serão adicionados na Task 8. O código vai compilar pois o service ainda não é invocado nos testes até a Task 5 (controller).

- [ ] **Step 1: Criar CustomerOrderService**

```java
package br.com.hadryan.agro.manager.domain.trading;

import br.com.hadryan.agro.manager.domain.account.Account;
import br.com.hadryan.agro.manager.domain.account.AccountMemberRepository;
import br.com.hadryan.agro.manager.domain.account.AccountRepository;
import br.com.hadryan.agro.manager.shared.exception.BusinessException;
import br.com.hadryan.agro.manager.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomerOrderService {

    private final CustomerOrderRepository orderRepository;
    private final PurchaseLotRepository lotRepository;
    private final AccountRepository accountRepository;
    private final AccountMemberRepository accountMemberRepository;

    @Transactional
    public CustomerOrderResponse create(UUID accountId, UUID userId, CustomerOrderRequest request) {
        Account account = validateAndGetAccount(accountId, userId);

        CustomerOrder order = CustomerOrder.builder()
                .account(account)
                .customerName(request.customerName().trim())
                .customerPhone(request.customerPhone())
                .customerDocument(request.customerDocument())
                .quantityKg(request.quantityKg())
                .pricePerKg(request.pricePerKg())
                .product(request.product().trim())
                .orderDate(request.orderDate())
                .deliveryDeadline(request.deliveryDeadline())
                .notes(request.notes())
                .build();

        return CustomerOrderResponse.from(orderRepository.save(order), CustomerOrderStatus.PENDING);
    }

    @Transactional(readOnly = true)
    public List<CustomerOrderResponse> listAll(UUID accountId, UUID userId, CustomerOrderStatus statusFilter) {
        validateMembership(accountId, userId);

        List<CustomerOrder> orders = orderRepository.findByAccountIdOrderByOrderDateDesc(accountId);
        // Uma query para todos os IDs de pedidos com lote — evita N+1
        Set<UUID> fulfilledIds = new HashSet<>(lotRepository.findCustomerOrderIdsByAccountId(accountId));

        return orders.stream()
                .map(o -> {
                    CustomerOrderStatus status = fulfilledIds.contains(o.getId())
                            ? CustomerOrderStatus.FULFILLED
                            : CustomerOrderStatus.PENDING;
                    return CustomerOrderResponse.from(o, status);
                })
                .filter(r -> statusFilter == null || r.status() == statusFilter)
                .toList();
    }

    @Transactional(readOnly = true)
    public CustomerOrderResponse findById(UUID accountId, UUID userId, UUID orderId) {
        validateMembership(accountId, userId);

        CustomerOrder order = orderRepository.findByIdAndAccountId(orderId, accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido", "id", orderId));

        CustomerOrderStatus status = lotRepository.existsByCustomerOrderId(orderId)
                ? CustomerOrderStatus.FULFILLED
                : CustomerOrderStatus.PENDING;

        return CustomerOrderResponse.from(order, status);
    }

    @Transactional
    public CustomerOrderResponse update(UUID accountId, UUID userId, UUID orderId, CustomerOrderRequest request) {
        validateMembership(accountId, userId);

        CustomerOrder order = orderRepository.findByIdAndAccountId(orderId, accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido", "id", orderId));

        if (lotRepository.existsByCustomerOrderId(orderId)) {
            throw new BusinessException(
                    "Pedido já vinculado a um lote de compra e não pode ser editado.",
                    HttpStatus.CONFLICT
            );
        }

        order.setCustomerName(request.customerName().trim());
        order.setCustomerPhone(request.customerPhone());
        order.setCustomerDocument(request.customerDocument());
        order.setQuantityKg(request.quantityKg());
        order.setPricePerKg(request.pricePerKg());
        order.setProduct(request.product().trim());
        order.setOrderDate(request.orderDate());
        order.setDeliveryDeadline(request.deliveryDeadline());
        order.setNotes(request.notes());

        return CustomerOrderResponse.from(orderRepository.save(order), CustomerOrderStatus.PENDING);
    }

    @Transactional
    public void delete(UUID accountId, UUID userId, UUID orderId) {
        validateMembership(accountId, userId);

        CustomerOrder order = orderRepository.findByIdAndAccountId(orderId, accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido", "id", orderId));

        if (lotRepository.existsByCustomerOrderId(orderId)) {
            throw new BusinessException(
                    "Pedido já vinculado a um lote de compra e não pode ser excluído.",
                    HttpStatus.CONFLICT
            );
        }

        orderRepository.delete(order);
    }

    // ── Utilitários privados ──────────────────────────────────────────────────

    private Account validateAndGetAccount(UUID accountId, UUID userId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Conta", "id", accountId));
        if (!accountMemberRepository.existsByAccountIdAndUserId(accountId, userId)) {
            throw new BusinessException("Acesso negado a esta conta", HttpStatus.FORBIDDEN);
        }
        return account;
    }

    private void validateMembership(UUID accountId, UUID userId) {
        if (!accountRepository.existsById(accountId)) {
            throw new ResourceNotFoundException("Conta", "id", accountId);
        }
        if (!accountMemberRepository.existsByAccountIdAndUserId(accountId, userId)) {
            throw new BusinessException("Acesso negado a esta conta", HttpStatus.FORBIDDEN);
        }
    }
}
```

**Nota de compilação:** O `PurchaseLotRepository` ainda não tem `existsByCustomerOrderId` nem `findCustomerOrderIdsByAccountId`. Adicione-os agora para permitir compilação (antes do commit desta task):

Em `PurchaseLotRepository.java`, adicione ao final da interface:

```java
// Verifica se existe lote vinculado a um pedido — para derivar status FULFILLED
boolean existsByCustomerOrderId(UUID customerOrderId);

// Retorna IDs de todos os pedidos com lote na conta — resolve status em bulk (sem N+1)
@Query("SELECT pl.customerOrder.id FROM PurchaseLot pl WHERE pl.account.id = :accountId")
List<UUID> findCustomerOrderIdsByAccountId(@Param("accountId") UUID accountId);
```

**Atenção:** Esses métodos referenciam `pl.customerOrder` — campo que ainda não existe em `PurchaseLot.java`. O código não compilará até a Task 7. Por isso, adicione esses métodos em `PurchaseLotRepository` **somente após** completar a Task 7 (que adiciona o campo `customerOrder` na entidade). A ordem correta de execução é:

```
Task 4 (service) → Task 5 (controller) → Task 6 (commit parcial) →
Task 7 (migration V18 + PurchaseLot entity) →
Task 8 (PurchaseLotRepository novos métodos — incluindo os usados aqui) →
Task 9 (responses) → Task 10 (service + controller) → testes passam
```

Se o CI exige que compile a cada commit, implemente na Task 8 e adicione o `@Autowired` do `lotRepository` no `CustomerOrderService` como temporariamente sem uso até lá.

- [ ] **Step 2: NÃO commitar ainda — aguardar Tasks 6 e 7**

O `CustomerOrderService` não compilará até que:
- Task 6 adicione o campo `customerOrder` em `PurchaseLot.java`
- Task 7 adicione `existsByCustomerOrderId` e `findCustomerOrderIdsByAccountId` em `PurchaseLotRepository.java`

Salve o arquivo agora, mas faça o commit junto com o da Task 7:

```bash
# Rodar DEPOIS de completar Tasks 6 e 7:
git add src/main/java/br/com/hadryan/agro/manager/domain/trading/CustomerOrderService.java
git commit -m "feat(trading): add CustomerOrderService with CRUD and derived status"
```

---

## Task 5: CustomerOrderController

**Files:**
- Create: `src/main/java/br/com/hadryan/agro/manager/domain/trading/CustomerOrderController.java`

- [ ] **Step 1: Criar CustomerOrderController**

```java
package br.com.hadryan.agro.manager.domain.trading;

import br.com.hadryan.agro.manager.infra.security.UserPrincipal;
import br.com.hadryan.agro.manager.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/accounts/{accountId}/trading/orders")
@RequiredArgsConstructor
public class CustomerOrderController {

    private final CustomerOrderService orderService;

    @PostMapping
    public ResponseEntity<ApiResponse<CustomerOrderResponse>> create(
            @PathVariable UUID accountId,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody @Valid CustomerOrderRequest request) {

        CustomerOrderResponse response = orderService.create(accountId, principal.getId(), request);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity.created(location).body(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CustomerOrderResponse>>> listAll(
            @PathVariable UUID accountId,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) CustomerOrderStatus status) {

        return ResponseEntity.ok(ApiResponse.success(
                orderService.listAll(accountId, principal.getId(), status)));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<CustomerOrderResponse>> findById(
            @PathVariable UUID accountId,
            @PathVariable UUID orderId,
            @AuthenticationPrincipal UserPrincipal principal) {

        return ResponseEntity.ok(ApiResponse.success(
                orderService.findById(accountId, principal.getId(), orderId)));
    }

    @PutMapping("/{orderId}")
    public ResponseEntity<ApiResponse<CustomerOrderResponse>> update(
            @PathVariable UUID accountId,
            @PathVariable UUID orderId,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody @Valid CustomerOrderRequest request) {

        return ResponseEntity.ok(ApiResponse.success(
                orderService.update(accountId, principal.getId(), orderId, request)));
    }

    @DeleteMapping("/{orderId}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID accountId,
            @PathVariable UUID orderId,
            @AuthenticationPrincipal UserPrincipal principal) {

        orderService.delete(accountId, principal.getId(), orderId);
        return ResponseEntity.noContent().build();
    }
}
```

- [ ] **Step 2: Rodar testes CustomerOrder (devem passar, exceto os que dependem de lot)**

```bash
mvn test -Dtest=CustomerOrderIntegrationTest
```

Expected: todos os 6 testes passam (CRUD básico, sem integração com lote ainda).

- [ ] **Step 3: Commit**

```bash
git add src/main/java/br/com/hadryan/agro/manager/domain/trading/CustomerOrderController.java
git commit -m "feat(trading): add CustomerOrderController with full CRUD endpoints"
```

---

## Task 6: V18 migration + modificar PurchaseLot entity

**Files:**
- Create: `src/main/resources/db/migration/V18__add_customer_order_id_to_purchase_lots.sql`
- Modify: `src/main/java/br/com/hadryan/agro/manager/domain/trading/PurchaseLot.java`

**Atenção:** A migration V18 adiciona `customer_order_id NOT NULL` na tabela `purchase_lots`. Se houver dados existentes no banco de dev, a migration falhará porque linhas sem `customer_order_id` violam o NOT NULL. Limpe a tabela antes de rodar:
```sql
TRUNCATE purchase_lots CASCADE;
```
Em produção, seria necessário um script de migração de dados ou permitir NULL temporariamente.

- [ ] **Step 1: Criar migration V18**

```sql
-- Vincula cada lote de compra a um pedido de cliente.
-- NOT NULL + UNIQUE garante 1:1 no banco: um lote para exatamente um pedido.

ALTER TABLE purchase_lots
    ADD COLUMN customer_order_id UUID NOT NULL
        REFERENCES customer_orders(id);

ALTER TABLE purchase_lots
    ADD CONSTRAINT uq_purchase_lots_customer_order UNIQUE (customer_order_id);

CREATE INDEX idx_purchase_lots_customer_order ON purchase_lots (customer_order_id);
```

- [ ] **Step 2: Adicionar campo customerOrder em PurchaseLot.java**

No arquivo `src/main/java/br/com/hadryan/agro/manager/domain/trading/PurchaseLot.java`, adicionar após o campo `supplier`:

```java
// Pedido do cliente que originou esta compra — sempre obrigatório (1:1, unidirecional)
@OneToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "customer_order_id", nullable = false, unique = true)
private CustomerOrder customerOrder;
```

- [ ] **Step 3: Verificar que a migration roda**

```bash
mvn test -Dtest=AgroManagerApplicationTests
```

Expected: BUILD SUCCESS.

- [ ] **Step 4: Commit**

```bash
git add src/main/resources/db/migration/V18__add_customer_order_id_to_purchase_lots.sql \
        src/main/java/br/com/hadryan/agro/manager/domain/trading/PurchaseLot.java
git commit -m "feat(trading): add customer_order_id FK to purchase_lots (V18 migration)"
```

---

## Task 7: Atualizar PurchaseLotRepository + criar CreatePurchaseLotRequest

**Files:**
- Modify: `src/main/java/br/com/hadryan/agro/manager/domain/trading/PurchaseLotRepository.java`
- Create: `src/main/java/br/com/hadryan/agro/manager/domain/trading/CreatePurchaseLotRequest.java`

- [ ] **Step 1: Adicionar métodos ao PurchaseLotRepository**

Adicionar ao final da interface `PurchaseLotRepository`:

```java
// Verifica se existe lote vinculado a um pedido — para derivar status FULFILLED
boolean existsByCustomerOrderId(UUID customerOrderId);

// Retorna IDs de todos os pedidos com lote na conta — resolve status em bulk (sem N+1)
@Query("SELECT pl.customerOrder.id FROM PurchaseLot pl WHERE pl.account.id = :accountId")
List<UUID> findCustomerOrderIdsByAccountId(@Param("accountId") UUID accountId);
```

Atualizar as queries das listagens paginadas para carregar `customerOrder` via JOIN FETCH (evita LazyInitializationException em `PurchaseLotSummaryResponse.from()`).

Substituir os 3 métodos de listagem paginada existentes por:

```java
@Query(value = "SELECT pl FROM PurchaseLot pl JOIN FETCH pl.supplier s JOIN FETCH pl.customerOrder co WHERE pl.account.id = :accountId",
        countQuery = "SELECT COUNT(pl) FROM PurchaseLot pl WHERE pl.account.id = :accountId")
Page<PurchaseLot> findByAccountIdWithSupplier(@Param("accountId") UUID accountId, Pageable pageable);

@Query(value = "SELECT pl FROM PurchaseLot pl JOIN FETCH pl.supplier s JOIN FETCH pl.customerOrder co WHERE pl.account.id = :accountId AND pl.status = :status",
        countQuery = "SELECT COUNT(pl) FROM PurchaseLot pl WHERE pl.account.id = :accountId AND pl.status = :status")
Page<PurchaseLot> findByAccountIdAndStatusWithSupplier(@Param("accountId") UUID accountId,
                                                       @Param("status") PurchaseLotStatus status,
                                                       Pageable pageable);

@Query(value = "SELECT pl FROM PurchaseLot pl JOIN FETCH pl.supplier s JOIN FETCH pl.customerOrder co WHERE pl.account.id = :accountId AND pl.supplier.id = :supplierId",
        countQuery = "SELECT COUNT(pl) FROM PurchaseLot pl WHERE pl.account.id = :accountId AND pl.supplier.id = :supplierId")
Page<PurchaseLot> findByAccountIdAndSupplierIdWithSupplier(@Param("accountId") UUID accountId,
                                                           @Param("supplierId") UUID supplierId,
                                                           Pageable pageable);
```

Atualizar também o `findWithTrucksByIdAndAccountId` para incluir `customerOrder`:

```java
@Query("SELECT pl FROM PurchaseLot pl " +
        "JOIN FETCH pl.supplier s " +
        "JOIN FETCH pl.customerOrder co " +
        "LEFT JOIN FETCH pl.trucks t " +
        "WHERE pl.id = :id AND pl.account.id = :accountId")
Optional<PurchaseLot> findWithTrucksByIdAndAccountId(@Param("id") UUID id,
                                                     @Param("accountId") UUID accountId);
```

- [ ] **Step 2: Criar CreatePurchaseLotRequest**

```java
package br.com.hadryan.agro.manager.domain.trading;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Dados para criação de um lote de compra.
 * Diferente de PurchaseLotRequest (usado em updates), exige customerOrderId.
 */
public record CreatePurchaseLotRequest(

        @NotNull(message = "Pedido do cliente é obrigatório")
        UUID customerOrderId,

        @NotNull(message = "Fornecedor é obrigatório")
        UUID supplierId,

        @NotNull(message = "Data da compra é obrigatória")
        LocalDate purchaseDate,

        @NotNull(message = "Preço por Kg é obrigatório")
        @DecimalMin(value = "0.0001", message = "Preço por Kg deve ser maior que zero")
        BigDecimal pricePerKg,

        @NotEmpty(message = "Informe ao menos um caminhão")
        @Valid
        List<PurchaseTruckRequest> trucks,

        String notes
) {
}
```

- [ ] **Step 3: Compile check**

```bash
mvn compile
```

Expected: BUILD SUCCESS.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/br/com/hadryan/agro/manager/domain/trading/PurchaseLotRepository.java \
        src/main/java/br/com/hadryan/agro/manager/domain/trading/CreatePurchaseLotRequest.java
git commit -m "feat(trading): update PurchaseLotRepository queries and add CreatePurchaseLotRequest"
```

---

## Task 8: Atualizar PurchaseLotSummaryResponse + PurchaseLotDetailResponse

**Files:**
- Modify: `src/main/java/br/com/hadryan/agro/manager/domain/trading/PurchaseLotSummaryResponse.java`
- Modify: `src/main/java/br/com/hadryan/agro/manager/domain/trading/PurchaseLotDetailResponse.java`

- [ ] **Step 1: Atualizar PurchaseLotSummaryResponse**

Substituir o arquivo inteiro por:

```java
package br.com.hadryan.agro.manager.domain.trading;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record PurchaseLotSummaryResponse(
        UUID id,
        UUID supplierId,
        String supplierName,
        UUID customerOrderId,
        String customerName,
        LocalDate purchaseDate,
        BigDecimal pricePerKg,
        PurchaseLotStatus status,
        BigDecimal totalPurchasedKg,
        BigDecimal totalSoldKg,
        BigDecimal remainingKg,
        BigDecimal totalCost,
        String notes,
        LocalDateTime createdAt
) {
    public static PurchaseLotSummaryResponse from(PurchaseLot lot,
                                                  BigDecimal totalPurchasedKg,
                                                  BigDecimal totalSoldKg) {
        BigDecimal remaining = totalPurchasedKg.subtract(totalSoldKg);
        BigDecimal totalCost = totalPurchasedKg.multiply(lot.getPricePerKg());

        return new PurchaseLotSummaryResponse(
                lot.getId(),
                lot.getSupplier().getId(),
                lot.getSupplier().getName(),
                lot.getCustomerOrder().getId(),
                lot.getCustomerOrder().getCustomerName(),
                lot.getPurchaseDate(),
                lot.getPricePerKg(),
                lot.getStatus(),
                totalPurchasedKg,
                totalSoldKg,
                remaining.max(BigDecimal.ZERO),
                totalCost,
                lot.getNotes(),
                lot.getCreatedAt()
        );
    }
}
```

- [ ] **Step 2: Atualizar PurchaseLotDetailResponse**

Adicionar os campos `customerOrderId` e `customerName` ao record e ao método `from()`.

Substituir a declaração do record (antes de `UUID supplierId`):

```java
public record PurchaseLotDetailResponse(
        UUID id,
        UUID supplierId,
        String supplierName,
        String supplierCity,
        UUID customerOrderId,        // NOVO
        String customerName,         // NOVO
        LocalDate purchaseDate,
        BigDecimal pricePerKg,
        PurchaseLotStatus status,
        List<PurchaseTruckResponse> purchaseTrucks,
        List<LotSaleResponse> sales,
        BigDecimal totalPurchasedKg,
        BigDecimal totalSoldKg,
        BigDecimal remainingKg,
        BigDecimal totalCost,
        BigDecimal totalRevenue,
        BigDecimal grossMargin,
        String notes,
        LocalDateTime createdAt
)
```

No método `from()`, após `lot.getSupplier().getCity()`, adicionar:

```java
lot.getCustomerOrder().getId(),          // customerOrderId
lot.getCustomerOrder().getCustomerName(), // customerName
```

O construtor `return new PurchaseLotDetailResponse(...)` completo:

```java
return new PurchaseLotDetailResponse(
        lot.getId(),
        lot.getSupplier().getId(),
        lot.getSupplier().getName(),
        lot.getSupplier().getCity(),
        lot.getCustomerOrder().getId(),
        lot.getCustomerOrder().getCustomerName(),
        lot.getPurchaseDate(),
        lot.getPricePerKg(),
        lot.getStatus(),
        truckResponses,
        saleResponses,
        totalPurchasedKg,
        totalSoldKg,
        totalSoldKg.compareTo(totalPurchasedKg) >= 0 ? BigDecimal.ZERO : totalPurchasedKg.subtract(totalSoldKg),
        totalCost,
        totalRevenue,
        grossMargin,
        lot.getNotes(),
        lot.getCreatedAt()
);
```

- [ ] **Step 3: Compile check**

```bash
mvn compile
```

Expected: BUILD SUCCESS.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/br/com/hadryan/agro/manager/domain/trading/PurchaseLotSummaryResponse.java \
        src/main/java/br/com/hadryan/agro/manager/domain/trading/PurchaseLotDetailResponse.java
git commit -m "feat(trading): add customerOrderId and customerName to PurchaseLot responses"
```

---

## Task 9: Atualizar PurchaseLotService + PurchaseLotController + MockMvcIntegrationTestBase

**Files:**
- Modify: `src/main/java/br/com/hadryan/agro/manager/domain/trading/PurchaseLotService.java`
- Modify: `src/main/java/br/com/hadryan/agro/manager/domain/trading/PurchaseLotController.java`
- Modify: `src/test/java/br/com/hadryan/agro/manager/MockMvcIntegrationTestBase.java`

- [ ] **Step 1: Adicionar CustomerOrderRepository ao PurchaseLotService**

No `PurchaseLotService.java`, adicionar campo:

```java
private final CustomerOrderRepository customerOrderRepository;
```

- [ ] **Step 2: Substituir método createLot() em PurchaseLotService**

Substituir o método `createLot` existente por:

```java
@Transactional
public PurchaseLotDetailResponse createLot(UUID accountId, UUID userId, CreatePurchaseLotRequest request) {
    Account account = validateAndGetAccount(accountId, userId);

    CustomerOrder customerOrder = customerOrderRepository.findByIdAndAccountId(request.customerOrderId(), accountId)
            .orElseThrow(() -> new ResourceNotFoundException("Pedido", "id", request.customerOrderId()));

    if (lotRepository.existsByCustomerOrderId(request.customerOrderId())) {
        throw new BusinessException(
                "Este pedido já está vinculado a um lote de compra.",
                HttpStatus.CONFLICT
        );
    }

    TradingSupplier supplier = supplierRepository.findByIdAndAccountId(request.supplierId(), accountId)
            .orElseThrow(() -> new ResourceNotFoundException("Fornecedor", "id", request.supplierId()));

    PurchaseLot lot = PurchaseLot.builder()
            .account(account)
            .customerOrder(customerOrder)
            .supplier(supplier)
            .purchaseDate(request.purchaseDate())
            .pricePerKg(request.pricePerKg())
            .notes(request.notes())
            .build();

    request.trucks().forEach(t -> {
        PurchaseTruck truck = PurchaseTruck.builder()
                .lot(lot)
                .truckPlate(t.truckPlate().toUpperCase().trim())
                .quantityKg(t.quantityKg())
                .notes(t.notes())
                .build();
        lot.getTrucks().add(truck);
    });

    PurchaseLot saved = lotRepository.save(lot);
    return PurchaseLotDetailResponse.from(saved, List.of());
}
```

- [ ] **Step 3: Atualizar PurchaseLotController — endpoint createLot**

No `PurchaseLotController.java`, alterar o `@PostMapping("/purchases")`:

```java
@PostMapping("/purchases")
public ResponseEntity<ApiResponse<PurchaseLotDetailResponse>> createLot(
        @PathVariable UUID accountId,
        @AuthenticationPrincipal UserPrincipal principal,
        @RequestBody @Valid CreatePurchaseLotRequest request) {

    PurchaseLotDetailResponse response = lotService.createLot(accountId, principal.getId(), request);

    URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(response.id())
            .toUri();

    return ResponseEntity.created(location).body(ApiResponse.success(response));
}
```

Adicionar import: `import br.com.hadryan.agro.manager.domain.trading.CreatePurchaseLotRequest;` (se necessário — mesmo pacote, pode ser desnecessário).

- [ ] **Step 4: Adicionar helper createOrder em MockMvcIntegrationTestBase**

Em `MockMvcIntegrationTestBase.java`, adicionar método auxiliar para criação de pedido (usado por testes de lote):

```java
protected String createTradingOrder(String token, String accountId) throws Exception {
    Map<String, Object> payload = new HashMap<>();
    payload.put("customerName", "Cliente Teste");
    payload.put("quantityKg", 5000.0);
    payload.put("product", "Soja");
    payload.put("orderDate", "2026-05-10");

    MvcResult result = mockMvc.perform(
                    post("/accounts/" + accountId + "/trading/orders")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(payload)))
            .andExpect(status().isCreated())
            .andReturn();

    return objectMapper.readTree(result.getResponse().getContentAsString())
            .path("data").path("id").asText();
}
```

- [ ] **Step 5: Compile check**

```bash
mvn compile
```

Expected: BUILD SUCCESS.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/br/com/hadryan/agro/manager/domain/trading/PurchaseLotService.java \
        src/main/java/br/com/hadryan/agro/manager/domain/trading/PurchaseLotController.java \
        src/test/java/br/com/hadryan/agro/manager/MockMvcIntegrationTestBase.java
git commit -m "feat(trading): require CustomerOrder when creating PurchaseLot"
```

---

## Task 10: Testes de integração para PurchaseLot + CustomerOrder e verificação final

**Files:**
- Modify: `src/test/java/br/com/hadryan/agro/manager/CustomerOrderIntegrationTest.java`

- [ ] **Step 1: Adicionar testes de ciclo de vida completo ao CustomerOrderIntegrationTest**

Adicionar os métodos abaixo à classe `CustomerOrderIntegrationTest`:

```java
@Test
void createLot_withValidOrder_orderBecomeFulfilled() throws Exception {
    String orderId = createOrder(token, accountId);
    String supplierId = createTradingSupplier(token, accountId);

    Map<String, Object> lotPayload = lotPayload(orderId, supplierId);

    mockMvc.perform(post("/accounts/" + accountId + "/trading/purchases")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(lotPayload)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.customerOrderId").value(orderId))
            .andExpect(jsonPath("$.data.customerName").value("João Silva"));

    // Pedido agora deve ser FULFILLED
    mockMvc.perform(get("/accounts/" + accountId + "/trading/orders/" + orderId)
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("FULFILLED"));
}

@Test
void createLot_withSameOrder_returnsConflict() throws Exception {
    String orderId = createOrder(token, accountId);
    String supplierId = createTradingSupplier(token, accountId);

    // Primeiro lote — OK
    mockMvc.perform(post("/accounts/" + accountId + "/trading/purchases")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(lotPayload(orderId, supplierId))))
            .andExpect(status().isCreated());

    // Segundo lote com mesmo pedido — CONFLICT
    mockMvc.perform(post("/accounts/" + accountId + "/trading/purchases")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(lotPayload(orderId, supplierId))))
            .andExpect(status().isConflict());
}

@Test
void updateOrder_whenFulfilled_returnsConflict() throws Exception {
    String orderId = createOrder(token, accountId);
    String supplierId = createTradingSupplier(token, accountId);

    mockMvc.perform(post("/accounts/" + accountId + "/trading/purchases")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(lotPayload(orderId, supplierId))))
            .andExpect(status().isCreated());

    // Tentar editar pedido FULFILLED
    mockMvc.perform(put("/accounts/" + accountId + "/trading/orders/" + orderId)
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(orderPayload())))
            .andExpect(status().isConflict());
}

@Test
void deleteOrder_whenFulfilled_returnsConflict() throws Exception {
    String orderId = createOrder(token, accountId);
    String supplierId = createTradingSupplier(token, accountId);

    mockMvc.perform(post("/accounts/" + accountId + "/trading/purchases")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(lotPayload(orderId, supplierId))))
            .andExpect(status().isCreated());

    mockMvc.perform(delete("/accounts/" + accountId + "/trading/orders/" + orderId)
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isConflict());
}

@Test
void createLot_withoutCustomerOrderId_returnsBadRequest() throws Exception {
    String supplierId = createTradingSupplier(token, accountId);

    Map<String, Object> payload = new HashMap<>();
    // customerOrderId ausente
    payload.put("supplierId", supplierId);
    payload.put("purchaseDate", "2026-05-10");
    payload.put("pricePerKg", 1.50);
    payload.put("trucks", List.of(Map.of("truckPlate", "ABC1234", "quantityKg", 5000.0)));

    mockMvc.perform(post("/accounts/" + accountId + "/trading/purchases")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload)))
            .andExpect(status().isBadRequest());
}

// ── Helpers adicionais ────────────────────────────────────────────────────

private String createTradingSupplier(String token, String accountId) throws Exception {
    Map<String, Object> payload = Map.of("name", "Fornecedor Teste");
    MvcResult result = mockMvc.perform(
                    post("/accounts/" + accountId + "/trading/suppliers")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(payload)))
            .andExpect(status().isCreated())
            .andReturn();
    return objectMapper.readTree(result.getResponse().getContentAsString())
            .path("data").path("id").asText();
}

private Map<String, Object> lotPayload(String orderId, String supplierId) {
    Map<String, Object> payload = new HashMap<>();
    payload.put("customerOrderId", orderId);
    payload.put("supplierId", supplierId);
    payload.put("purchaseDate", "2026-05-10");
    payload.put("pricePerKg", 1.50);
    payload.put("trucks", List.of(Map.of("truckPlate", "ABC1234", "quantityKg", 5000.0)));
    return payload;
}
```

- [ ] **Step 2: Rodar todos os testes**

```bash
mvn test
```

Expected: BUILD SUCCESS — todos os testes passam, incluindo os existentes (regressão) e os novos.

- [ ] **Step 3: Commit final**

```bash
git add src/test/java/br/com/hadryan/agro/manager/CustomerOrderIntegrationTest.java
git commit -m "test(trading): add integration tests for CustomerOrder + PurchaseLot lifecycle"
```

---

## Checklist de Verificação Final

Antes de marcar a feature como completa:

- [ ] `GET /trading/orders` retorna status derivado correto (PENDING/FULFILLED)
- [ ] `POST /trading/purchases` sem `customerOrderId` → 400
- [ ] `POST /trading/purchases` com `customerOrderId` inválido → 404
- [ ] `POST /trading/purchases` com mesmo `customerOrderId` duas vezes → 409
- [ ] `PUT /trading/orders/{id}` em pedido FULFILLED → 409
- [ ] `DELETE /trading/orders/{id}` em pedido FULFILLED → 409
- [ ] `GET /trading/purchases` inclui `customerOrderId` e `customerName` no response
- [ ] `GET /trading/purchases/{id}` inclui `customerOrderId` e `customerName` no response
- [ ] Testes existentes (FarmIntegrationTest, ExpenseIntegrationTest, etc.) continuam passando
