# Redesign do Fluxo de Trading — Backend Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Substituir o modelo PurchaseLot/LotSale por ClientOrder/OrderSupplierLeg/OrderTruck + novo cadastro de TradingClient, refletindo o fluxo real do negócio.

**Architecture:** Deleção completa das 4 entidades antigas e substituição por 4 novas. TradingClient é CRUD simples. ClientOrder é a entidade central com legs (por fornecedor) e trucks (por leg). O banco está zerado — sem migração de dados.

**Tech Stack:** Spring Boot 3, JPA/Hibernate, Flyway, PostgreSQL 17, Lombok, Java records, Testcontainers (testes)

> **Nota:** Este plano cobre apenas o backend Java. Um plano separado deve ser escrito para o frontend Vue.

---

## File Map

| Ação | Arquivo |
|---|---|
| DELETE | `domain/trading/PurchaseLot.java` |
| DELETE | `domain/trading/PurchaseLotStatus.java` |
| DELETE | `domain/trading/PurchaseLotRequest.java` |
| DELETE | `domain/trading/PurchaseLotDetailResponse.java` |
| DELETE | `domain/trading/PurchaseLotSummaryResponse.java` |
| DELETE | `domain/trading/PurchaseLotRepository.java` |
| DELETE | `domain/trading/PurchaseTruck.java` |
| DELETE | `domain/trading/PurchaseTruckRequest.java` |
| DELETE | `domain/trading/PurchaseTruckResponse.java` |
| DELETE | `domain/trading/LotSale.java` |
| DELETE | `domain/trading/LotSaleRequest.java` |
| DELETE | `domain/trading/LotSaleResponse.java` |
| DELETE | `domain/trading/LotSaleRepository.java` |
| DELETE | `domain/trading/LotSaleTruck.java` |
| DELETE | `domain/trading/LotSaleTruckRequest.java` |
| DELETE | `domain/trading/LotSaleTruckResponse.java` |
| DELETE | `domain/trading/PurchaseLotService.java` |
| DELETE | `domain/trading/PurchaseLotController.java` |
| DELETE | `domain/trading/TradingDashboardResponse.java` |
| MODIFY | `domain/trading/TradingSupplierRepository.java` |
| MODIFY | `domain/trading/TradingSupplierService.java` |
| CREATE | `db/migration/V18__drop_old_trading_tables.sql` |
| CREATE | `db/migration/V19__create_trading_clients_table.sql` |
| CREATE | `domain/trading/TradingClient.java` |
| CREATE | `domain/trading/TradingClientRepository.java` |
| CREATE | `domain/trading/TradingClientRequest.java` |
| CREATE | `domain/trading/TradingClientResponse.java` |
| CREATE | `domain/trading/TradingClientService.java` |
| CREATE | `domain/trading/TradingClientController.java` |
| CREATE | `test/.../TradingClientIntegrationTest.java` |
| CREATE | `db/migration/V20__create_client_orders_table.sql` |
| CREATE | `db/migration/V21__create_order_supplier_legs_table.sql` |
| CREATE | `db/migration/V22__create_order_trucks_table.sql` |
| CREATE | `domain/trading/ClientOrderStatus.java` |
| CREATE | `domain/trading/ClientOrder.java` |
| CREATE | `domain/trading/OrderSupplierLeg.java` |
| CREATE | `domain/trading/OrderTruck.java` |
| CREATE | `domain/trading/ClientOrderRepository.java` |
| CREATE | `domain/trading/OrderSupplierLegRepository.java` |
| CREATE | `domain/trading/OrderTruckRepository.java` |
| CREATE | `domain/trading/ClientOrderRequest.java` |
| CREATE | `domain/trading/OrderSupplierLegRequest.java` |
| CREATE | `domain/trading/OrderTruckRequest.java` |
| CREATE | `domain/trading/OrderTruckResponse.java` |
| CREATE | `domain/trading/OrderSupplierLegResponse.java` |
| CREATE | `domain/trading/ClientOrderDetailResponse.java` |
| CREATE | `domain/trading/ClientOrderSummaryResponse.java` |
| CREATE | `domain/trading/ClientOrderService.java` |
| CREATE | `domain/trading/ClientOrderController.java` |
| CREATE | `domain/trading/TradingDashboardResponse.java` |
| CREATE | `test/.../ClientOrderIntegrationTest.java` |

Todos os paths Java expandem para `src/main/java/br/com/hadryan/agro/manager/` e os de teste para `src/test/java/br/com/hadryan/agro/manager/`.

---

## Task 1: Remover código antigo

**Files:**
- Delete: 19 arquivos listados acima
- Modify: `domain/trading/TradingSupplierRepository.java`
- Modify: `domain/trading/TradingSupplierService.java`

- [ ] **Step 1: Deletar os arquivos antigos**

```powershell
cd "C:\Workspace\Backend\Java\agro-manager"
$base = "src\main\java\br\com\hadryan\agro\manager\domain\trading"
Remove-Item "$base\PurchaseLot.java",
            "$base\PurchaseLotStatus.java",
            "$base\PurchaseLotRequest.java",
            "$base\PurchaseLotDetailResponse.java",
            "$base\PurchaseLotSummaryResponse.java",
            "$base\PurchaseLotRepository.java",
            "$base\PurchaseTruck.java",
            "$base\PurchaseTruckRequest.java",
            "$base\PurchaseTruckResponse.java",
            "$base\LotSale.java",
            "$base\LotSaleRequest.java",
            "$base\LotSaleResponse.java",
            "$base\LotSaleRepository.java",
            "$base\LotSaleTruck.java",
            "$base\LotSaleTruckRequest.java",
            "$base\LotSaleTruckResponse.java",
            "$base\PurchaseLotService.java",
            "$base\PurchaseLotController.java",
            "$base\TradingDashboardResponse.java"
```

- [ ] **Step 2: Atualizar TradingSupplierRepository**

Remover o método `hasAssociatedLots` (referencia `PurchaseLot` que não existe mais). A proteção de exclusão será restaurada na Task 9.

Conteúdo final do arquivo:

```java
package br.com.hadryan.agro.manager.domain.trading;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TradingSupplierRepository extends JpaRepository<TradingSupplier, UUID> {

    List<TradingSupplier> findByAccountIdOrderByNameAsc(UUID accountId);

    @Query("SELECT s FROM TradingSupplier s WHERE s.account.id = :accountId AND LOWER(s.name) LIKE LOWER(CONCAT('%', :name, '%')) ORDER BY s.name")
    List<TradingSupplier> searchByName(@Param("accountId") UUID accountId, @Param("name") String name);

    Optional<TradingSupplier> findByIdAndAccountId(UUID id, UUID accountId);

    long countByAccountId(UUID accountId);
}
```

- [ ] **Step 3: Atualizar TradingSupplierService.delete()**

Remover a chamada a `hasAssociatedLots`. Conteúdo do método `delete()` após a mudança:

```java
@Transactional
public void delete(UUID accountId, UUID userId, UUID supplierId) {
    validateMembership(accountId, userId);
    TradingSupplier supplier = findSupplier(supplierId, accountId);
    supplierRepository.delete(supplier);
}
```

- [ ] **Step 4: Compilar**

```powershell
.\mvnw.cmd compile -q
```

Expected: `BUILD SUCCESS`

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "chore: remove old trading entities, service and controller (PurchaseLot/LotSale)"
```

---

## Task 2: V18 — Drop das tabelas antigas

**Files:**
- Create: `src/main/resources/db/migration/V18__drop_old_trading_tables.sql`

- [ ] **Step 1: Criar o script**

```sql
-- Remove tabelas do modelo antigo (PurchaseLot + LotSale).
-- Ordem: filhos antes dos pais para respeitar FK constraints.
DROP TABLE IF EXISTS lot_sale_trucks;
DROP TABLE IF EXISTS lot_sales;
DROP TABLE IF EXISTS purchase_trucks;
DROP TABLE IF EXISTS purchase_lots;
```

- [ ] **Step 2: Commit**

```bash
git add src/main/resources/db/migration/V18__drop_old_trading_tables.sql
git commit -m "feat: V18 drop old trading tables"
```

---

## Task 3: V19 + TradingClient entity + repository

**Files:**
- Create: `src/main/resources/db/migration/V19__create_trading_clients_table.sql`
- Create: `src/main/java/br/com/hadryan/agro/manager/domain/trading/TradingClient.java`
- Create: `src/main/java/br/com/hadryan/agro/manager/domain/trading/TradingClientRepository.java`

- [ ] **Step 1: Criar V19**

```sql
CREATE TABLE trading_clients (
    id         UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id UUID           NOT NULL REFERENCES accounts (id) ON DELETE CASCADE,
    name       VARCHAR(150)   NOT NULL,
    phone      VARCHAR(20)    NOT NULL,
    city       VARCHAR(100),
    notes      TEXT,
    created_at TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP      NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_trading_clients_account ON trading_clients (account_id);
```

- [ ] **Step 2: Criar TradingClient.java**

```java
package br.com.hadryan.agro.manager.domain.trading;

import br.com.hadryan.agro.manager.domain.account.Account;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "trading_clients")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TradingClient {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(length = 100)
    private String city;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }

    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }
}
```

- [ ] **Step 3: Criar TradingClientRepository.java**

```java
package br.com.hadryan.agro.manager.domain.trading;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TradingClientRepository extends JpaRepository<TradingClient, UUID> {

    List<TradingClient> findByAccountIdOrderByNameAsc(UUID accountId);

    @Query("SELECT c FROM TradingClient c WHERE c.account.id = :accountId AND LOWER(c.name) LIKE LOWER(CONCAT('%', :name, '%')) ORDER BY c.name")
    List<TradingClient> searchByName(@Param("accountId") UUID accountId, @Param("name") String name);

    Optional<TradingClient> findByIdAndAccountId(UUID id, UUID accountId);
}
```

- [ ] **Step 4: Compilar**

```powershell
.\mvnw.cmd compile -q
```

Expected: `BUILD SUCCESS`

- [ ] **Step 5: Commit**

```bash
git add src/main/resources/db/migration/V19__create_trading_clients_table.sql
git add src/main/java/br/com/hadryan/agro/manager/domain/trading/TradingClient.java
git add src/main/java/br/com/hadryan/agro/manager/domain/trading/TradingClientRepository.java
git commit -m "feat: add TradingClient entity and repository"
```

---

## Task 4: TradingClient CRUD (request / response / service / controller / testes)

**Files:**
- Create: `domain/trading/TradingClientRequest.java`
- Create: `domain/trading/TradingClientResponse.java`
- Create: `domain/trading/ClientOrderRepository.java` (mínimo — para proteção de delete)
- Create: `domain/trading/TradingClientService.java`
- Create: `domain/trading/TradingClientController.java`
- Create: `test/.../TradingClientIntegrationTest.java`

- [ ] **Step 1: Criar TradingClientRequest.java**

```java
package br.com.hadryan.agro.manager.domain.trading;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TradingClientRequest(
        @NotBlank(message = "Nome é obrigatório")
        @Size(max = 150, message = "Nome deve ter no máximo 150 caracteres")
        String name,

        @NotBlank(message = "Telefone é obrigatório")
        @Size(max = 20, message = "Telefone deve ter no máximo 20 caracteres")
        String phone,

        @Size(max = 100, message = "Cidade deve ter no máximo 100 caracteres")
        String city,

        String notes
) {
}
```

- [ ] **Step 2: Criar TradingClientResponse.java**

```java
package br.com.hadryan.agro.manager.domain.trading;

import java.time.LocalDateTime;
import java.util.UUID;

public record TradingClientResponse(
        UUID id,
        String name,
        String phone,
        String city,
        String notes,
        LocalDateTime createdAt
) {
    public static TradingClientResponse from(TradingClient c) {
        return new TradingClientResponse(
                c.getId(), c.getName(), c.getPhone(), c.getCity(), c.getNotes(), c.getCreatedAt()
        );
    }
}
```

- [ ] **Step 3: Criar ClientOrderRepository.java (mínimo)**

Este arquivo será expandido na Task 5. Por agora só precisa do método usado em TradingClientService.

```java
package br.com.hadryan.agro.manager.domain.trading;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ClientOrderRepository extends JpaRepository<ClientOrder, UUID> {
    boolean existsByClientId(UUID clientId);
}
```

> **Nota:** `ClientOrder` ainda não existe — o compilador vai falhar. Crie também um stub mínimo de `ClientOrder.java` para satisfazer o compilador agora. A versão completa vem na Task 5.

Stub `ClientOrder.java` temporário:

```java
package br.com.hadryan.agro.manager.domain.trading;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "client_orders")
@Getter @Setter @NoArgsConstructor
public class ClientOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private TradingClient client;
}
```

- [ ] **Step 4: Criar TradingClientService.java**

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

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TradingClientService {

    private final TradingClientRepository clientRepository;
    private final ClientOrderRepository orderRepository;
    private final AccountRepository accountRepository;
    private final AccountMemberRepository accountMemberRepository;

    @Transactional
    public TradingClientResponse create(UUID accountId, UUID userId, TradingClientRequest request) {
        Account account = validateAndGetAccount(accountId, userId);
        TradingClient client = TradingClient.builder()
                .account(account)
                .name(request.name().trim())
                .phone(request.phone())
                .city(request.city())
                .notes(request.notes())
                .build();
        return TradingClientResponse.from(clientRepository.save(client));
    }

    @Transactional(readOnly = true)
    public List<TradingClientResponse> listAll(UUID accountId, UUID userId) {
        validateMembership(accountId, userId);
        return clientRepository.findByAccountIdOrderByNameAsc(accountId).stream()
                .map(TradingClientResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<TradingClientResponse> search(UUID accountId, UUID userId, String name) {
        validateMembership(accountId, userId);
        return clientRepository.searchByName(accountId, name).stream()
                .map(TradingClientResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public TradingClientResponse findById(UUID accountId, UUID userId, UUID clientId) {
        validateMembership(accountId, userId);
        return TradingClientResponse.from(findClient(clientId, accountId));
    }

    @Transactional
    public TradingClientResponse update(UUID accountId, UUID userId, UUID clientId, TradingClientRequest request) {
        validateMembership(accountId, userId);
        TradingClient client = findClient(clientId, accountId);
        client.setName(request.name().trim());
        client.setPhone(request.phone());
        client.setCity(request.city());
        client.setNotes(request.notes());
        return TradingClientResponse.from(clientRepository.save(client));
    }

    @Transactional
    public void delete(UUID accountId, UUID userId, UUID clientId) {
        validateMembership(accountId, userId);
        TradingClient client = findClient(clientId, accountId);
        if (orderRepository.existsByClientId(clientId)) {
            throw new BusinessException(
                    "Não é possível excluir um cliente com pedidos registrados.",
                    HttpStatus.CONFLICT
            );
        }
        clientRepository.delete(client);
    }

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

    private TradingClient findClient(UUID clientId, UUID accountId) {
        return clientRepository.findByIdAndAccountId(clientId, accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente", "id", clientId));
    }
}
```

- [ ] **Step 5: Criar TradingClientController.java**

```java
package br.com.hadryan.agro.manager.domain.trading;

import br.com.hadryan.agro.manager.infra.security.UserPrincipal;
import br.com.hadryan.agro.manager.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/accounts/{accountId}/trading/clients")
@RequiredArgsConstructor
public class TradingClientController {

    private final TradingClientService clientService;

    @PostMapping
    public ResponseEntity<ApiResponse<TradingClientResponse>> create(
            @PathVariable UUID accountId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody TradingClientRequest request) {
        TradingClientResponse response = clientService.create(accountId, principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<TradingClientResponse>>> list(
            @PathVariable UUID accountId,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String search) {
        List<TradingClientResponse> clients = (search != null && !search.isBlank())
                ? clientService.search(accountId, principal.getId(), search)
                : clientService.listAll(accountId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(clients));
    }

    @GetMapping("/{clientId}")
    public ResponseEntity<ApiResponse<TradingClientResponse>> getById(
            @PathVariable UUID accountId,
            @PathVariable UUID clientId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                clientService.findById(accountId, principal.getId(), clientId)));
    }

    @PutMapping("/{clientId}")
    public ResponseEntity<ApiResponse<TradingClientResponse>> update(
            @PathVariable UUID accountId,
            @PathVariable UUID clientId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody TradingClientRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                clientService.update(accountId, principal.getId(), clientId, request)));
    }

    @DeleteMapping("/{clientId}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID accountId,
            @PathVariable UUID clientId,
            @AuthenticationPrincipal UserPrincipal principal) {
        clientService.delete(accountId, principal.getId(), clientId);
        return ResponseEntity.noContent().build();
    }
}
```

- [ ] **Step 6: Escrever TradingClientIntegrationTest.java (testes primeiro)**

```java
package br.com.hadryan.agro.manager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class TradingClientIntegrationTest extends MockMvcIntegrationTestBase {

    private String token;
    private String accountId;
    private String base;

    @BeforeEach
    void setUp() throws Exception {
        String email = uniqueEmail();
        token = registerAndGetToken(email, "senha123");
        accountId = createAccount(token, "Conta Teste");
        base = "/accounts/" + accountId + "/trading/clients";
    }

    @Test
    void createClient_returnsCreated() throws Exception {
        var payload = Map.of("name", "João Silva", "phone", "11999999999");
        mockMvc.perform(post(base)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("João Silva"))
                .andExpect(jsonPath("$.data.phone").value("11999999999"))
                .andExpect(jsonPath("$.data.id").isNotEmpty());
    }

    @Test
    void createClient_missingName_returns400() throws Exception {
        var payload = Map.of("phone", "11999999999");
        mockMvc.perform(post(base)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listClients_returnsAll() throws Exception {
        var p1 = Map.of("name", "Ana", "phone", "11111111111");
        var p2 = Map.of("name", "Bia", "phone", "22222222222");
        mockMvc.perform(post(base).header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(p1)));
        mockMvc.perform(post(base).header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(p2)));

        mockMvc.perform(get(base).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    void searchClients_filtersByName() throws Exception {
        var p1 = Map.of("name", "Carlos Produtor", "phone", "11111111111");
        var p2 = Map.of("name", "Marina Cliente", "phone", "22222222222");
        mockMvc.perform(post(base).header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(p1)));
        mockMvc.perform(post(base).header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(p2)));

        mockMvc.perform(get(base + "?search=carlos").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].name").value("Carlos Produtor"));
    }

    @Test
    void updateClient_changesFields() throws Exception {
        var payload = Map.of("name", "Original", "phone", "11111111111");
        var result = mockMvc.perform(post(base).header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(payload)))
                .andReturn();
        String clientId = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("id").asText();

        var update = Map.of("name", "Atualizado", "phone", "99999999999", "city", "São Paulo");
        mockMvc.perform(put(base + "/" + clientId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Atualizado"))
                .andExpect(jsonPath("$.data.city").value("São Paulo"));
    }

    @Test
    void deleteClient_withoutOrders_returns204() throws Exception {
        var payload = Map.of("name", "Para Deletar", "phone", "11111111111");
        var result = mockMvc.perform(post(base).header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(payload)))
                .andReturn();
        String clientId = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("id").asText();

        mockMvc.perform(delete(base + "/" + clientId).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    @Test
    void getClient_notFound_returns404() throws Exception {
        mockMvc.perform(get(base + "/00000000-0000-0000-0000-000000000000")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }
}
```

- [ ] **Step 7: Rodar os testes (devem falhar — implementação ainda não existe)**

```powershell
.\mvnw.cmd test -pl . -Dtest=TradingClientIntegrationTest -q
```

Expected: FAIL — endpoints não existem ainda.

- [ ] **Step 8: Compilar com a implementação completa**

```powershell
.\mvnw.cmd compile -q
```

Expected: `BUILD SUCCESS`

- [ ] **Step 9: Rodar os testes novamente**

```powershell
.\mvnw.cmd test -pl . -Dtest=TradingClientIntegrationTest -q
```

Expected: todos os testes passam.

- [ ] **Step 10: Commit**

```bash
git add src/
git commit -m "feat: add TradingClient CRUD (request/response/service/controller/tests)"
```

---

## Task 5: Entidades ClientOrder (V20–V22 + Status + entidades + repositories)

**Files:**
- Create: `db/migration/V20__create_client_orders_table.sql`
- Create: `db/migration/V21__create_order_supplier_legs_table.sql`
- Create: `db/migration/V22__create_order_trucks_table.sql`
- Create: `domain/trading/ClientOrderStatus.java`
- Replace: `domain/trading/ClientOrder.java` (substituir o stub da Task 4)
- Create: `domain/trading/OrderSupplierLeg.java`
- Create: `domain/trading/OrderTruck.java`
- Replace: `domain/trading/ClientOrderRepository.java` (expandir o mínimo da Task 4)
- Create: `domain/trading/OrderSupplierLegRepository.java`
- Create: `domain/trading/OrderTruckRepository.java`

- [ ] **Step 1: V20 — client_orders**

```sql
CREATE TABLE client_orders (
    id                   UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id           UUID           NOT NULL REFERENCES accounts (id) ON DELETE CASCADE,
    client_id            UUID           NOT NULL REFERENCES trading_clients (id),
    order_date           DATE           NOT NULL,
    client_price_per_kg  DECIMAL(12, 2) NOT NULL,
    status               VARCHAR(10)    NOT NULL DEFAULT 'OPEN',
    notes                TEXT,
    created_at           TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP      NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_client_orders_account ON client_orders (account_id);
CREATE INDEX idx_client_orders_client  ON client_orders (client_id);
```

- [ ] **Step 2: V21 — order_supplier_legs**

```sql
CREATE TABLE order_supplier_legs (
    id                     UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id               UUID           NOT NULL REFERENCES client_orders (id) ON DELETE CASCADE,
    supplier_id            UUID           NOT NULL REFERENCES trading_suppliers (id),
    supplier_price_per_kg  DECIMAL(12, 2) NOT NULL,
    notes                  TEXT,
    created_at             TIMESTAMP      NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_order_supplier_legs_order ON order_supplier_legs (order_id);
```

- [ ] **Step 3: V22 — order_trucks**

```sql
CREATE TABLE order_trucks (
    id            UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    leg_id        UUID           NOT NULL REFERENCES order_supplier_legs (id) ON DELETE CASCADE,
    truck_plate   VARCHAR(10)    NOT NULL,
    quantity_kg   DECIMAL(12, 2) NOT NULL,
    freight_value DECIMAL(12, 2),
    notes         TEXT,
    created_at    TIMESTAMP      NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_order_trucks_leg ON order_trucks (leg_id);
```

- [ ] **Step 4: Criar ClientOrderStatus.java**

```java
package br.com.hadryan.agro.manager.domain.trading;

public enum ClientOrderStatus {
    OPEN, CLOSED
}
```

- [ ] **Step 5: Substituir ClientOrder.java (versão completa)**

```java
package br.com.hadryan.agro.manager.domain.trading;

import br.com.hadryan.agro.manager.domain.account.Account;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "client_orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private TradingClient client;

    @Column(name = "order_date", nullable = false)
    private LocalDate orderDate;

    @Column(name = "client_price_per_kg", nullable = false, precision = 12, scale = 2)
    private BigDecimal clientPricePerKg;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ClientOrderStatus status = ClientOrderStatus.OPEN;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Builder.Default
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderSupplierLeg> legs = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }

    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }
}
```

- [ ] **Step 6: Criar OrderSupplierLeg.java**

```java
package br.com.hadryan.agro.manager.domain.trading;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "order_supplier_legs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderSupplierLeg {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private ClientOrder order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    private TradingSupplier supplier;

    @Column(name = "supplier_price_per_kg", nullable = false, precision = 12, scale = 2)
    private BigDecimal supplierPricePerKg;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Builder.Default
    @OneToMany(mappedBy = "leg", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderTruck> trucks = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }
}
```

- [ ] **Step 7: Criar OrderTruck.java**

```java
package br.com.hadryan.agro.manager.domain.trading;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "order_trucks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderTruck {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "leg_id", nullable = false)
    private OrderSupplierLeg leg;

    @Column(name = "truck_plate", nullable = false, length = 10)
    private String truckPlate;

    @Column(name = "quantity_kg", nullable = false, precision = 12, scale = 2)
    private BigDecimal quantityKg;

    @Column(name = "freight_value", precision = 12, scale = 2)
    private BigDecimal freightValue;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }
}
```

- [ ] **Step 8: Substituir ClientOrderRepository.java (versão completa)**

```java
package br.com.hadryan.agro.manager.domain.trading;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClientOrderRepository extends JpaRepository<ClientOrder, UUID> {

    boolean existsByClientId(UUID clientId);

    Optional<ClientOrder> findByIdAndAccountId(UUID id, UUID accountId);

    @Query("SELECT o FROM ClientOrder o JOIN FETCH o.client WHERE o.account.id = :accountId ORDER BY o.orderDate DESC")
    Page<ClientOrder> findByAccountIdWithClient(@Param("accountId") UUID accountId, Pageable pageable);

    @Query("SELECT o FROM ClientOrder o JOIN FETCH o.client WHERE o.account.id = :accountId AND o.status = :status ORDER BY o.orderDate DESC")
    Page<ClientOrder> findByAccountIdAndStatusWithClient(@Param("accountId") UUID accountId, @Param("status") ClientOrderStatus status, Pageable pageable);

    @Query("SELECT o FROM ClientOrder o JOIN FETCH o.client WHERE o.account.id = :accountId AND o.client.id = :clientId ORDER BY o.orderDate DESC")
    Page<ClientOrder> findByAccountIdAndClientIdWithClient(@Param("accountId") UUID accountId, @Param("clientId") UUID clientId, Pageable pageable);

    @Query("SELECT o FROM ClientOrder o JOIN FETCH o.client WHERE o.account.id = :accountId AND o.status = :status AND o.client.id = :clientId ORDER BY o.orderDate DESC")
    Page<ClientOrder> findByAccountIdAndStatusAndClientIdWithClient(@Param("accountId") UUID accountId, @Param("status") ClientOrderStatus status, @Param("clientId") UUID clientId, Pageable pageable);

    @Query("SELECT DISTINCT o FROM ClientOrder o JOIN FETCH o.legs l JOIN FETCH l.supplier WHERE o.id = :orderId AND o.account.id = :accountId")
    Optional<ClientOrder> findWithLegsByIdAndAccountId(@Param("orderId") UUID orderId, @Param("accountId") UUID accountId);

    @Query("SELECT t.leg.order.id, COALESCE(SUM(t.quantityKg), 0) FROM OrderTruck t WHERE t.leg.order.id IN :orderIds GROUP BY t.leg.order.id")
    List<Object[]> sumTotalKgByOrderIds(@Param("orderIds") List<UUID> orderIds);

    @Query("SELECT t.leg.order.id, COALESCE(SUM(t.quantityKg * t.leg.supplierPricePerKg), 0) + COALESCE(SUM(t.freightValue), 0) FROM OrderTruck t WHERE t.leg.order.id IN :orderIds GROUP BY t.leg.order.id")
    List<Object[]> sumTotalCostByOrderIds(@Param("orderIds") List<UUID> orderIds);

    long countByAccountId(UUID accountId);

    long countByAccountIdAndStatus(UUID accountId, ClientOrderStatus status);

    @Query("SELECT COALESCE(SUM(t.quantityKg), 0) FROM OrderTruck t WHERE t.leg.order.account.id = :accountId")
    BigDecimal sumTotalKgByAccountId(@Param("accountId") UUID accountId);

    @Query("SELECT COALESCE(SUM(t.quantityKg * t.leg.order.clientPricePerKg), 0) FROM OrderTruck t WHERE t.leg.order.account.id = :accountId")
    BigDecimal sumRevenueByAccountId(@Param("accountId") UUID accountId);

    @Query("SELECT COALESCE(SUM(t.quantityKg * t.leg.supplierPricePerKg), 0) FROM OrderTruck t WHERE t.leg.order.account.id = :accountId")
    BigDecimal sumProductCostByAccountId(@Param("accountId") UUID accountId);

    @Query("SELECT COALESCE(SUM(t.freightValue), 0) FROM OrderTruck t WHERE t.leg.order.account.id = :accountId")
    BigDecimal sumFreightCostByAccountId(@Param("accountId") UUID accountId);
}
```

- [ ] **Step 9: Criar OrderSupplierLegRepository.java**

```java
package br.com.hadryan.agro.manager.domain.trading;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OrderSupplierLegRepository extends JpaRepository<OrderSupplierLeg, UUID> {

    Optional<OrderSupplierLeg> findByIdAndOrderId(UUID id, UUID orderId);

    boolean existsBySupplierId(UUID supplierId);
}
```

- [ ] **Step 10: Criar OrderTruckRepository.java**

```java
package br.com.hadryan.agro.manager.domain.trading;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OrderTruckRepository extends JpaRepository<OrderTruck, UUID> {

    List<OrderTruck> findByLegOrderId(UUID orderId);
}
```

- [ ] **Step 11: Compilar**

```powershell
.\mvnw.cmd compile -q
```

Expected: `BUILD SUCCESS`

- [ ] **Step 12: Commit**

```bash
git add src/
git commit -m "feat: add ClientOrder/OrderSupplierLeg/OrderTruck entities and repositories (V20-V22)"
```

---

## Task 6: DTOs de ClientOrder

**Files:**
- Create: `domain/trading/OrderTruckRequest.java`
- Create: `domain/trading/OrderSupplierLegRequest.java`
- Create: `domain/trading/ClientOrderRequest.java`
- Create: `domain/trading/OrderTruckResponse.java`
- Create: `domain/trading/OrderSupplierLegResponse.java`
- Create: `domain/trading/ClientOrderDetailResponse.java`
- Create: `domain/trading/ClientOrderSummaryResponse.java`

- [ ] **Step 1: OrderTruckRequest.java**

```java
package br.com.hadryan.agro.manager.domain.trading;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record OrderTruckRequest(
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

- [ ] **Step 2: OrderSupplierLegRequest.java**

```java
package br.com.hadryan.agro.manager.domain.trading;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record OrderSupplierLegRequest(
        @NotNull(message = "Fornecedor é obrigatório")
        UUID supplierId,

        @NotNull(message = "Preço por kg do fornecedor é obrigatório")
        @DecimalMin(value = "0.0001", message = "Preço por kg deve ser maior que zero")
        BigDecimal supplierPricePerKg,

        @NotNull
        @Size(min = 1, message = "Cada perna deve ter ao menos um caminhão")
        List<@Valid OrderTruckRequest> trucks,

        String notes
) {
}
```

- [ ] **Step 3: ClientOrderRequest.java**

```java
package br.com.hadryan.agro.manager.domain.trading;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ClientOrderRequest(
        @NotNull(message = "Cliente é obrigatório")
        UUID clientId,

        @NotNull(message = "Data do pedido é obrigatória")
        LocalDate orderDate,

        @NotNull(message = "Preço por kg do cliente é obrigatório")
        @DecimalMin(value = "0.0001", message = "Preço por kg deve ser maior que zero")
        BigDecimal clientPricePerKg,

        @NotNull
        @Size(min = 1, message = "O pedido deve ter ao menos uma perna de fornecedor")
        List<@Valid OrderSupplierLegRequest> legs,

        String notes
) {
}
```

- [ ] **Step 4: OrderTruckResponse.java**

```java
package br.com.hadryan.agro.manager.domain.trading;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderTruckResponse(
        UUID id,
        String truckPlate,
        BigDecimal quantityKg,
        BigDecimal freightValue,
        String notes
) {
    public static OrderTruckResponse from(OrderTruck t) {
        return new OrderTruckResponse(
                t.getId(), t.getTruckPlate(), t.getQuantityKg(), t.getFreightValue(), t.getNotes()
        );
    }
}
```

- [ ] **Step 5: OrderSupplierLegResponse.java**

```java
package br.com.hadryan.agro.manager.domain.trading;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record OrderSupplierLegResponse(
        UUID id,
        UUID supplierId,
        String supplierName,
        String supplierCity,
        BigDecimal supplierPricePerKg,
        List<OrderTruckResponse> trucks,
        BigDecimal totalKg,
        BigDecimal totalCost,
        String notes
) {
    public static OrderSupplierLegResponse from(OrderSupplierLeg leg) {
        List<OrderTruckResponse> truckResponses = leg.getTrucks().stream()
                .map(OrderTruckResponse::from).toList();

        BigDecimal totalKg = leg.getTrucks().stream()
                .map(OrderTruck::getQuantityKg)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCost = leg.getTrucks().stream()
                .map(t -> {
                    BigDecimal product = t.getQuantityKg().multiply(leg.getSupplierPricePerKg());
                    BigDecimal freight = t.getFreightValue() != null ? t.getFreightValue() : BigDecimal.ZERO;
                    return product.add(freight);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new OrderSupplierLegResponse(
                leg.getId(),
                leg.getSupplier().getId(),
                leg.getSupplier().getName(),
                leg.getSupplier().getCity(),
                leg.getSupplierPricePerKg(),
                truckResponses,
                totalKg,
                totalCost,
                leg.getNotes()
        );
    }
}
```

- [ ] **Step 6: ClientOrderDetailResponse.java**

```java
package br.com.hadryan.agro.manager.domain.trading;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ClientOrderDetailResponse(
        UUID id,
        UUID clientId,
        String clientName,
        String clientPhone,
        LocalDate orderDate,
        BigDecimal clientPricePerKg,
        ClientOrderStatus status,
        List<OrderSupplierLegResponse> legs,
        BigDecimal totalKg,
        BigDecimal totalRevenue,
        BigDecimal totalCost,
        BigDecimal grossMargin,
        String notes,
        LocalDateTime createdAt
) {
    public static ClientOrderDetailResponse from(ClientOrder order) {
        List<OrderSupplierLegResponse> legResponses = order.getLegs().stream()
                .map(OrderSupplierLegResponse::from).toList();

        BigDecimal totalKg = legResponses.stream()
                .map(OrderSupplierLegResponse::totalKg)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalRevenue = totalKg.multiply(order.getClientPricePerKg());

        BigDecimal totalCost = legResponses.stream()
                .map(OrderSupplierLegResponse::totalCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new ClientOrderDetailResponse(
                order.getId(),
                order.getClient().getId(),
                order.getClient().getName(),
                order.getClient().getPhone(),
                order.getOrderDate(),
                order.getClientPricePerKg(),
                order.getStatus(),
                legResponses,
                totalKg,
                totalRevenue,
                totalCost,
                totalRevenue.subtract(totalCost),
                order.getNotes(),
                order.getCreatedAt()
        );
    }
}
```

- [ ] **Step 7: ClientOrderSummaryResponse.java**

```java
package br.com.hadryan.agro.manager.domain.trading;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record ClientOrderSummaryResponse(
        UUID id,
        UUID clientId,
        String clientName,
        LocalDate orderDate,
        BigDecimal clientPricePerKg,
        ClientOrderStatus status,
        BigDecimal totalKg,
        BigDecimal totalRevenue,
        BigDecimal totalCost,
        BigDecimal grossMargin,
        String notes,
        LocalDateTime createdAt
) {
    public static ClientOrderSummaryResponse from(ClientOrder order, BigDecimal totalKg, BigDecimal totalCost) {
        BigDecimal totalRevenue = totalKg.multiply(order.getClientPricePerKg());
        return new ClientOrderSummaryResponse(
                order.getId(),
                order.getClient().getId(),
                order.getClient().getName(),
                order.getOrderDate(),
                order.getClientPricePerKg(),
                order.getStatus(),
                totalKg,
                totalRevenue,
                totalCost,
                totalRevenue.subtract(totalCost),
                order.getNotes(),
                order.getCreatedAt()
        );
    }
}
```

- [ ] **Step 8: Compilar**

```powershell
.\mvnw.cmd compile -q
```

Expected: `BUILD SUCCESS`

- [ ] **Step 9: Commit**

```bash
git add src/
git commit -m "feat: add ClientOrder DTOs (request/response records)"
```

---

## Task 7: ClientOrderService — CRUD de pedidos

**Files:**
- Create: `domain/trading/ClientOrderService.java`

- [ ] **Step 1: Criar ClientOrderService.java**

```java
package br.com.hadryan.agro.manager.domain.trading;

import br.com.hadryan.agro.manager.domain.account.Account;
import br.com.hadryan.agro.manager.domain.account.AccountMemberRepository;
import br.com.hadryan.agro.manager.domain.account.AccountRepository;
import br.com.hadryan.agro.manager.shared.dto.PageResponse;
import br.com.hadryan.agro.manager.shared.exception.BusinessException;
import br.com.hadryan.agro.manager.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClientOrderService {

    private final ClientOrderRepository orderRepository;
    private final OrderSupplierLegRepository legRepository;
    private final OrderTruckRepository truckRepository;
    private final TradingClientRepository clientRepository;
    private final TradingSupplierRepository supplierRepository;
    private final AccountRepository accountRepository;
    private final AccountMemberRepository accountMemberRepository;

    // ── Pedidos ───────────────────────────────────────────────────────────────

    @Transactional
    public ClientOrderDetailResponse createOrder(UUID accountId, UUID userId, ClientOrderRequest request) {
        Account account = validateAndGetAccount(accountId, userId);

        TradingClient client = clientRepository.findByIdAndAccountId(request.clientId(), accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente", "id", request.clientId()));

        ClientOrder order = ClientOrder.builder()
                .account(account)
                .client(client)
                .orderDate(request.orderDate())
                .clientPricePerKg(request.clientPricePerKg())
                .notes(request.notes())
                .build();

        addLegsToOrder(order, request.legs(), accountId);

        ClientOrder saved = orderRepository.save(order);
        return loadDetail(saved.getId(), accountId);
    }

    @Transactional(readOnly = true)
    public PageResponse<ClientOrderSummaryResponse> listOrders(
            UUID accountId, UUID userId,
            ClientOrderStatus status, UUID clientId,
            int page, int size) {

        validateMembership(accountId, userId);
        Pageable pageable = PageRequest.of(page, size);

        Page<ClientOrder> orders;
        if (status != null && clientId != null) {
            orders = orderRepository.findByAccountIdAndStatusAndClientIdWithClient(accountId, status, clientId, pageable);
        } else if (status != null) {
            orders = orderRepository.findByAccountIdAndStatusWithClient(accountId, status, pageable);
        } else if (clientId != null) {
            orders = orderRepository.findByAccountIdAndClientIdWithClient(accountId, clientId, pageable);
        } else {
            orders = orderRepository.findByAccountIdWithClient(accountId, pageable);
        }

        List<UUID> orderIds = orders.getContent().stream().map(ClientOrder::getId).toList();

        Map<UUID, BigDecimal> kgMap = orderIds.isEmpty() ? Map.of()
                : orderRepository.sumTotalKgByOrderIds(orderIds).stream()
                        .collect(Collectors.toMap(r -> (UUID) r[0], r -> (BigDecimal) r[1]));

        Map<UUID, BigDecimal> costMap = orderIds.isEmpty() ? Map.of()
                : orderRepository.sumTotalCostByOrderIds(orderIds).stream()
                        .collect(Collectors.toMap(r -> (UUID) r[0], r -> (BigDecimal) r[1]));

        List<ClientOrderSummaryResponse> content = orders.getContent().stream()
                .map(o -> ClientOrderSummaryResponse.from(
                        o,
                        kgMap.getOrDefault(o.getId(), BigDecimal.ZERO),
                        costMap.getOrDefault(o.getId(), BigDecimal.ZERO)))
                .toList();

        return new PageResponse<>(content, orders.getNumber(), orders.getSize(),
                orders.getTotalElements(), orders.getTotalPages(), orders.isLast(), null);
    }

    @Transactional(readOnly = true)
    public ClientOrderDetailResponse getOrderDetail(UUID accountId, UUID userId, UUID orderId) {
        validateMembership(accountId, userId);
        return loadDetail(orderId, accountId);
    }

    @Transactional
    public ClientOrderDetailResponse updateOrder(UUID accountId, UUID userId, UUID orderId, ClientOrderRequest request) {
        validateMembership(accountId, userId);

        ClientOrder order = orderRepository.findByIdAndAccountId(orderId, accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido", "id", orderId));

        if (order.getStatus() == ClientOrderStatus.CLOSED) {
            throw new BusinessException("Pedido encerrado não pode ser editado.", HttpStatus.CONFLICT);
        }

        TradingClient client = clientRepository.findByIdAndAccountId(request.clientId(), accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente", "id", request.clientId()));

        order.setClient(client);
        order.setOrderDate(request.orderDate());
        order.setClientPricePerKg(request.clientPricePerKg());
        order.setNotes(request.notes());

        order.getLegs().clear();
        addLegsToOrder(order, request.legs(), accountId);

        orderRepository.save(order);
        return loadDetail(orderId, accountId);
    }

    @Transactional
    public void closeOrder(UUID accountId, UUID userId, UUID orderId) {
        validateMembership(accountId, userId);
        ClientOrder order = orderRepository.findByIdAndAccountId(orderId, accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido", "id", orderId));
        if (order.getStatus() == ClientOrderStatus.CLOSED) return;
        order.setStatus(ClientOrderStatus.CLOSED);
        orderRepository.save(order);
    }

    @Transactional
    public void deleteOrder(UUID accountId, UUID userId, UUID orderId) {
        validateMembership(accountId, userId);
        ClientOrder order = orderRepository.findByIdAndAccountId(orderId, accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido", "id", orderId));
        if (order.getStatus() == ClientOrderStatus.CLOSED) {
            throw new BusinessException("Pedido encerrado não pode ser excluído.", HttpStatus.CONFLICT);
        }
        orderRepository.delete(order);
    }

    // ── Legs ─────────────────────────────────────────────────────────────────

    @Transactional
    public ClientOrderDetailResponse addLeg(UUID accountId, UUID userId, UUID orderId, OrderSupplierLegRequest request) {
        validateMembership(accountId, userId);

        ClientOrder order = orderRepository.findByIdAndAccountId(orderId, accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido", "id", orderId));

        if (order.getStatus() == ClientOrderStatus.CLOSED) {
            throw new BusinessException("Não é possível adicionar pernas a um pedido encerrado.", HttpStatus.CONFLICT);
        }

        TradingSupplier supplier = supplierRepository.findByIdAndAccountId(request.supplierId(), accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Fornecedor", "id", request.supplierId()));

        OrderSupplierLeg leg = OrderSupplierLeg.builder()
                .order(order)
                .supplier(supplier)
                .supplierPricePerKg(request.supplierPricePerKg())
                .notes(request.notes())
                .build();

        request.trucks().forEach(t -> leg.getTrucks().add(OrderTruck.builder()
                .leg(leg)
                .truckPlate(t.truckPlate().toUpperCase().trim())
                .quantityKg(t.quantityKg())
                .freightValue(t.freightValue())
                .notes(t.notes())
                .build()));

        order.getLegs().add(leg);
        orderRepository.save(order);
        return loadDetail(orderId, accountId);
    }

    @Transactional
    public void removeLeg(UUID accountId, UUID userId, UUID orderId, UUID legId) {
        validateMembership(accountId, userId);

        ClientOrder order = orderRepository.findByIdAndAccountId(orderId, accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido", "id", orderId));

        if (order.getStatus() == ClientOrderStatus.CLOSED) {
            throw new BusinessException("Não é possível remover pernas de um pedido encerrado.", HttpStatus.CONFLICT);
        }

        OrderSupplierLeg leg = legRepository.findByIdAndOrderId(legId, orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Perna de fornecedor", "id", legId));

        order.getLegs().remove(leg);
        orderRepository.save(order);
    }

    // ── Dashboard ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public TradingDashboardResponse getDashboard(UUID accountId, UUID userId) {
        validateMembership(accountId, userId);

        long totalOrders  = orderRepository.countByAccountId(accountId);
        long openOrders   = orderRepository.countByAccountIdAndStatus(accountId, ClientOrderStatus.OPEN);
        long closedOrders = orderRepository.countByAccountIdAndStatus(accountId, ClientOrderStatus.CLOSED);
        long totalClients    = clientRepository.countByAccountId(accountId);
        long totalSuppliers  = supplierRepository.countByAccountId(accountId);

        BigDecimal totalKg      = orderRepository.sumTotalKgByAccountId(accountId);
        BigDecimal totalRevenue = orderRepository.sumRevenueByAccountId(accountId);
        BigDecimal productCost  = orderRepository.sumProductCostByAccountId(accountId);
        BigDecimal freightCost  = orderRepository.sumFreightCostByAccountId(accountId);
        BigDecimal totalCost    = productCost.add(freightCost);

        return new TradingDashboardResponse(
                totalOrders, openOrders, closedOrders,
                totalKg, totalRevenue, totalCost,
                totalRevenue.subtract(totalCost),
                totalClients, totalSuppliers
        );
    }

    // ── Utilitários privados ──────────────────────────────────────────────────

    private void addLegsToOrder(ClientOrder order, List<OrderSupplierLegRequest> legRequests, UUID accountId) {
        legRequests.forEach(lr -> {
            TradingSupplier supplier = supplierRepository.findByIdAndAccountId(lr.supplierId(), accountId)
                    .orElseThrow(() -> new ResourceNotFoundException("Fornecedor", "id", lr.supplierId()));

            OrderSupplierLeg leg = OrderSupplierLeg.builder()
                    .order(order)
                    .supplier(supplier)
                    .supplierPricePerKg(lr.supplierPricePerKg())
                    .notes(lr.notes())
                    .build();

            lr.trucks().forEach(t -> leg.getTrucks().add(OrderTruck.builder()
                    .leg(leg)
                    .truckPlate(t.truckPlate().toUpperCase().trim())
                    .quantityKg(t.quantityKg())
                    .freightValue(t.freightValue())
                    .notes(t.notes())
                    .build()));

            order.getLegs().add(leg);
        });
    }

    private ClientOrderDetailResponse loadDetail(UUID orderId, UUID accountId) {
        ClientOrder order = orderRepository.findWithLegsByIdAndAccountId(orderId, accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido", "id", orderId));
        List<OrderTruck> trucks = truckRepository.findByLegOrderId(orderId);
        trucks.forEach(t -> order.getLegs().stream()
                .filter(l -> l.getId().equals(t.getLeg().getId()))
                .findFirst()
                .ifPresent(l -> {
                    if (!l.getTrucks().contains(t)) l.getTrucks().add(t);
                }));
        return ClientOrderDetailResponse.from(order);
    }

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

- [ ] **Step 2: Criar TradingDashboardResponse.java** (necessário para o service compilar)

```java
package br.com.hadryan.agro.manager.domain.trading;

import java.math.BigDecimal;

public record TradingDashboardResponse(
        long totalOrders,
        long openOrders,
        long closedOrders,
        BigDecimal totalKg,
        BigDecimal totalRevenue,
        BigDecimal totalCost,
        BigDecimal grossMargin,
        long totalClients,
        long totalSuppliers
) {
}
```

- [ ] **Step 3: Adicionar countByAccountId em TradingClientRepository**

Adicionar ao final da interface (antes do `}`):

```java
long countByAccountId(UUID accountId);
```

- [ ] **Step 4: Compilar**

```powershell
.\mvnw.cmd compile -q
```

Expected: `BUILD SUCCESS`

- [ ] **Step 5: Commit**

```bash
git add src/
git commit -m "feat: add ClientOrderService with order CRUD, leg management and dashboard"
```

---

## Task 8: ClientOrderController + restaurar proteção do fornecedor

**Files:**
- Create: `domain/trading/ClientOrderController.java`
- Modify: `domain/trading/TradingSupplierRepository.java`
- Modify: `domain/trading/TradingSupplierService.java`

- [ ] **Step 1: Criar ClientOrderController.java**

```java
package br.com.hadryan.agro.manager.domain.trading;

import br.com.hadryan.agro.manager.infra.security.UserPrincipal;
import br.com.hadryan.agro.manager.shared.dto.ApiResponse;
import br.com.hadryan.agro.manager.shared.dto.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/accounts/{accountId}/trading")
@RequiredArgsConstructor
public class ClientOrderController {

    private final ClientOrderService orderService;

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<TradingDashboardResponse>> getDashboard(
            @PathVariable UUID accountId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                orderService.getDashboard(accountId, principal.getId())));
    }

    @PostMapping("/orders")
    public ResponseEntity<ApiResponse<ClientOrderDetailResponse>> create(
            @PathVariable UUID accountId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ClientOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                orderService.createOrder(accountId, principal.getId(), request)));
    }

    @GetMapping("/orders")
    public ResponseEntity<ApiResponse<PageResponse<ClientOrderSummaryResponse>>> list(
            @PathVariable UUID accountId,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) ClientOrderStatus status,
            @RequestParam(required = false) UUID clientId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                orderService.listOrders(accountId, principal.getId(), status, clientId, page, size)));
    }

    @GetMapping("/orders/{orderId}")
    public ResponseEntity<ApiResponse<ClientOrderDetailResponse>> getDetail(
            @PathVariable UUID accountId,
            @PathVariable UUID orderId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                orderService.getOrderDetail(accountId, principal.getId(), orderId)));
    }

    @PutMapping("/orders/{orderId}")
    public ResponseEntity<ApiResponse<ClientOrderDetailResponse>> update(
            @PathVariable UUID accountId,
            @PathVariable UUID orderId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ClientOrderRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                orderService.updateOrder(accountId, principal.getId(), orderId, request)));
    }

    @PatchMapping("/orders/{orderId}/close")
    public ResponseEntity<Void> close(
            @PathVariable UUID accountId,
            @PathVariable UUID orderId,
            @AuthenticationPrincipal UserPrincipal principal) {
        orderService.closeOrder(accountId, principal.getId(), orderId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/orders/{orderId}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID accountId,
            @PathVariable UUID orderId,
            @AuthenticationPrincipal UserPrincipal principal) {
        orderService.deleteOrder(accountId, principal.getId(), orderId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/orders/{orderId}/legs")
    public ResponseEntity<ApiResponse<ClientOrderDetailResponse>> addLeg(
            @PathVariable UUID accountId,
            @PathVariable UUID orderId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody OrderSupplierLegRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                orderService.addLeg(accountId, principal.getId(), orderId, request)));
    }

    @DeleteMapping("/orders/{orderId}/legs/{legId}")
    public ResponseEntity<Void> removeLeg(
            @PathVariable UUID accountId,
            @PathVariable UUID orderId,
            @PathVariable UUID legId,
            @AuthenticationPrincipal UserPrincipal principal) {
        orderService.removeLeg(accountId, principal.getId(), orderId, legId);
        return ResponseEntity.noContent().build();
    }
}
```

- [ ] **Step 2: Restaurar proteção de exclusão de fornecedor em TradingSupplierRepository**

Adicionar à interface (antes do `}`):

```java
@Query("SELECT COUNT(l) > 0 FROM OrderSupplierLeg l WHERE l.supplier.id = :supplierId")
boolean hasAssociatedLegs(@Param("supplierId") UUID supplierId);
```

- [ ] **Step 3: Restaurar chamada em TradingSupplierService.delete()**

```java
@Transactional
public void delete(UUID accountId, UUID userId, UUID supplierId) {
    validateMembership(accountId, userId);
    TradingSupplier supplier = findSupplier(supplierId, accountId);
    if (supplierRepository.hasAssociatedLegs(supplierId)) {
        throw new BusinessException(
                "Não é possível excluir um fornecedor com pedidos registrados.",
                HttpStatus.CONFLICT
        );
    }
    supplierRepository.delete(supplier);
}
```

- [ ] **Step 4: Compilar**

```powershell
.\mvnw.cmd compile -q
```

Expected: `BUILD SUCCESS`

- [ ] **Step 5: Commit**

```bash
git add src/
git commit -m "feat: add ClientOrderController and restore supplier delete protection"
```

---

## Task 9: Testes de integração para ClientOrder

**Files:**
- Create: `test/.../ClientOrderIntegrationTest.java`

- [ ] **Step 1: Criar ClientOrderIntegrationTest.java**

```java
package br.com.hadryan.agro.manager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;

import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ClientOrderIntegrationTest extends MockMvcIntegrationTestBase {

    private String token;
    private String accountId;
    private String clientId;
    private String supplierId;
    private String ordersBase;

    @BeforeEach
    void setUp() throws Exception {
        String email = uniqueEmail();
        token = registerAndGetToken(email, "senha123");
        accountId = createAccount(token, "Conta Teste");
        ordersBase = "/accounts/" + accountId + "/trading/orders";

        clientId = createClient("Cliente Teste", "11999999999");
        supplierId = createSupplier("Fornecedor Teste");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String createClient(String name, String phone) throws Exception {
        var payload = Map.of("name", name, "phone", phone);
        var result = mockMvc.perform(post("/accounts/" + accountId + "/trading/clients")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("id").asText();
    }

    private String createSupplier(String name) throws Exception {
        var payload = Map.of("name", name, "phone", "11888888888");
        var result = mockMvc.perform(post("/accounts/" + accountId + "/trading/suppliers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("id").asText();
    }

    private Map<String, Object> orderPayload() {
        return Map.of(
                "clientId", clientId,
                "orderDate", "2026-05-14",
                "clientPricePerKg", 1.20,
                "legs", List.of(Map.of(
                        "supplierId", supplierId,
                        "supplierPricePerKg", 0.85,
                        "trucks", List.of(Map.of(
                                "truckPlate", "ABC1D23",
                                "quantityKg", 3000.0,
                                "freightValue", 150.0
                        ))
                ))
        );
    }

    private String createOrder() throws Exception {
        var result = mockMvc.perform(post(ordersBase)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderPayload())))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("id").asText();
    }

    // ── Testes ────────────────────────────────────────────────────────────────

    @Test
    void createOrder_returnsCreatedWithLegsAndTrucks() throws Exception {
        mockMvc.perform(post(ordersBase)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderPayload())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.clientName").value("Cliente Teste"))
                .andExpect(jsonPath("$.data.status").value("OPEN"))
                .andExpect(jsonPath("$.data.legs.length()").value(1))
                .andExpect(jsonPath("$.data.legs[0].trucks.length()").value(1))
                .andExpect(jsonPath("$.data.totalKg").value(3000.0))
                .andExpect(jsonPath("$.data.totalRevenue").value(3600.0))
                .andExpect(jsonPath("$.data.legs[0].trucks[0].freightValue").value(150.0));
    }

    @Test
    void createOrder_missingClientId_returns400() throws Exception {
        var payload = Map.of(
                "orderDate", "2026-05-14",
                "clientPricePerKg", 1.20,
                "legs", List.of()
        );
        mockMvc.perform(post(ordersBase)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listOrders_returnsPagedResults() throws Exception {
        createOrder();
        createOrder();

        mockMvc.perform(get(ordersBase).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(2));
    }

    @Test
    void listOrders_filterByStatus_returnsOnlyOpen() throws Exception {
        String orderId = createOrder();
        createOrder();

        mockMvc.perform(patch(ordersBase + "/" + orderId + "/close")
                .header("Authorization", "Bearer " + token));

        mockMvc.perform(get(ordersBase + "?status=OPEN").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1));
    }

    @Test
    void getOrderDetail_returnsFullDetail() throws Exception {
        String orderId = createOrder();
        mockMvc.perform(get(ordersBase + "/" + orderId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(orderId))
                .andExpect(jsonPath("$.data.legs[0].supplierName").value("Fornecedor Teste"));
    }

    @Test
    void closeOrder_changesStatusToClosed() throws Exception {
        String orderId = createOrder();
        mockMvc.perform(patch(ordersBase + "/" + orderId + "/close")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(ordersBase + "/" + orderId).header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.data.status").value("CLOSED"));
    }

    @Test
    void deleteClosedOrder_returns409() throws Exception {
        String orderId = createOrder();
        mockMvc.perform(patch(ordersBase + "/" + orderId + "/close")
                .header("Authorization", "Bearer " + token));

        mockMvc.perform(delete(ordersBase + "/" + orderId).header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict());
    }

    @Test
    void deleteOpenOrder_returns204() throws Exception {
        String orderId = createOrder();
        mockMvc.perform(delete(ordersBase + "/" + orderId).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    @Test
    void addLeg_toOpenOrder_returns201WithUpdatedDetail() throws Exception {
        String orderId = createOrder();
        String supplier2Id = createSupplier("Segundo Fornecedor");

        var legPayload = Map.of(
                "supplierId", supplier2Id,
                "supplierPricePerKg", 0.90,
                "trucks", List.of(Map.of("truckPlate", "XYZ9H87", "quantityKg", 2000.0))
        );

        mockMvc.perform(post(ordersBase + "/" + orderId + "/legs")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(legPayload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.legs.length()").value(2))
                .andExpect(jsonPath("$.data.totalKg").value(5000.0));
    }

    @Test
    void removeLeg_fromOpenOrder_returns204() throws Exception {
        String orderId = createOrder();
        var detail = mockMvc.perform(get(ordersBase + "/" + orderId)
                        .header("Authorization", "Bearer " + token))
                .andReturn();
        String legId = objectMapper.readTree(detail.getResponse().getContentAsString())
                .path("data").path("legs").get(0).path("id").asText();

        mockMvc.perform(delete(ordersBase + "/" + orderId + "/legs/" + legId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteClient_withOrders_returns409() throws Exception {
        createOrder();
        mockMvc.perform(delete("/accounts/" + accountId + "/trading/clients/" + clientId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict());
    }

    @Test
    void deleteSupplier_withLegs_returns409() throws Exception {
        createOrder();
        mockMvc.perform(delete("/accounts/" + accountId + "/trading/suppliers/" + supplierId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict());
    }

    @Test
    void getDashboard_returnsAggregatedMetrics() throws Exception {
        createOrder();
        mockMvc.perform(get("/accounts/" + accountId + "/trading/dashboard")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalOrders").value(1))
                .andExpect(jsonPath("$.data.openOrders").value(1))
                .andExpect(jsonPath("$.data.totalKg").value(3000.0))
                .andExpect(jsonPath("$.data.totalClients").value(1))
                .andExpect(jsonPath("$.data.totalSuppliers").value(1));
    }
}
```

- [ ] **Step 2: Rodar os testes**

```powershell
.\mvnw.cmd test -pl . -Dtest="TradingClientIntegrationTest,ClientOrderIntegrationTest" -q
```

Expected: todos os testes passam.

- [ ] **Step 3: Rodar a suite completa**

```powershell
.\mvnw.cmd test -q
```

Expected: `BUILD SUCCESS` — nenhum teste pré-existente quebrado.

- [ ] **Step 4: Commit**

```bash
git add src/
git commit -m "test: add ClientOrderIntegrationTest covering full order lifecycle"
```

---

## Checklist de verificação final

Após todos os commits, confirme manualmente:

- [ ] `GET /accounts/{id}/trading/dashboard` retorna `totalOrders`, `totalClients` (não mais `totalLots`)
- [ ] `POST /accounts/{id}/trading/orders` com múltiplas legs funciona
- [ ] `DELETE /accounts/{id}/trading/clients/{id}` com pedidos retorna 409
- [ ] `DELETE /accounts/{id}/trading/suppliers/{id}` com legs retorna 409
- [ ] Flyway roda sem erros: V1 → V22 em banco zerado
- [ ] Nenhuma referência a `PurchaseLot`, `LotSale`, `purchase_lots`, `lot_sales` restante no código Java

```powershell
# Verificar referências residuais
Select-String -Path "src\main\java\**\*.java" -Pattern "PurchaseLot|LotSale" -Recurse
```

Expected: nenhum resultado.
