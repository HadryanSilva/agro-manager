# Rename "atravessador" → "comprador" Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Substituir o termo "atravessador" por "comprador" em todos os comentários do código para refletir o vocabulário do domínio de produção de melancia.

**Architecture:** Mudança puramente cosmética — todas as 10 ocorrências estão em comentários Javadoc e SQL, sem impacto em nomes de classe, campos de API, colunas de banco ou contratos externos. Zero risco de regressão.

**Tech Stack:** Java 21, Spring Boot, Flyway (SQL migrations), Lombok

---

## Mapeamento de Arquivos

| Arquivo | Ocorrências | Tipo de mudança |
|---------|-------------|-----------------|
| `src/main/java/.../trading/TradingSupplier.java` | 2 | Javadoc |
| `src/main/java/.../trading/PurchaseLot.java` | 1 | Javadoc |
| `src/main/java/.../trading/PurchaseLotStatus.java` | 1 | Javadoc |
| `src/main/java/.../trading/TradingDashboardResponse.java` | 1 | Javadoc |
| `src/main/java/.../trading/TradingSupplierController.java` | 1 | Javadoc |
| `src/main/java/.../trading/TradingSupplierRepository.java` | 1 | Javadoc |
| `src/main/java/.../trading/TradingSupplierService.java` | 1 | Javadoc |
| `src/main/resources/db/migration/V12__create_trading_suppliers_table.sql` | 2 | SQL comment |
| `src/main/resources/db/migration/V13__create_purchase_lots_table.sql` | 1 | SQL comment |

**Total: 10 substituições em 9 arquivos. Nenhum teste necessário (comentários não afetam comportamento).**

---

### Task 1: Atualizar comentários nos arquivos Java

**Files:**
- Modify: `src/main/java/br/com/hadryan/agro/manager/domain/trading/TradingSupplier.java:12`
- Modify: `src/main/java/br/com/hadryan/agro/manager/domain/trading/PurchaseLot.java:16`
- Modify: `src/main/java/br/com/hadryan/agro/manager/domain/trading/PurchaseLotStatus.java:6`
- Modify: `src/main/java/br/com/hadryan/agro/manager/domain/trading/TradingDashboardResponse.java:6`
- Modify: `src/main/java/br/com/hadryan/agro/manager/domain/trading/TradingSupplierController.java:17`
- Modify: `src/main/java/br/com/hadryan/agro/manager/domain/trading/TradingSupplierRepository.java:12`
- Modify: `src/main/java/br/com/hadryan/agro/manager/domain/trading/TradingSupplierService.java:17`

- [ ] **Step 1: Atualizar TradingSupplier.java**

Substituir linha 12:
```java
// ANTES:
 * Fornecedor do modo atravessador — produtor de quem o atravessador compra melancia.

// DEPOIS:
 * Fornecedor do modo comprador — produtor de quem o comprador compra melancia.
```

- [ ] **Step 2: Atualizar PurchaseLot.java**

Substituir a linha do Javadoc que contém "atravessador":
```java
// ANTES:
 * Lote de compra do modo atravessador.

// DEPOIS:
 * Lote de compra do modo comprador.
```

- [ ] **Step 3: Atualizar PurchaseLotStatus.java**

Substituir linha 6:
```java
// ANTES:
 * e pode ser encerrado manualmente pelo atravessador.

// DEPOIS:
 * e pode ser encerrado manualmente pelo comprador.
```

- [ ] **Step 4: Atualizar TradingDashboardResponse.java**

Substituir a linha do Javadoc que contém "atravessador":
```java
// ANTES:
 * Resposta do dashboard do modo atravessador.

// DEPOIS:
 * Resposta do dashboard do modo comprador.
```

- [ ] **Step 5: Atualizar TradingSupplierController.java**

Substituir a linha do Javadoc que contém "atravessador":
```java
// ANTES:
 * Endpoints para gerenciamento de fornecedores do modo atravessador.

// DEPOIS:
 * Endpoints para gerenciamento de fornecedores do modo comprador.
```

- [ ] **Step 6: Atualizar TradingSupplierRepository.java**

Substituir a linha do Javadoc que contém "atravessador":
```java
// ANTES:
 * Repositório de fornecedores do modo atravessador.

// DEPOIS:
 * Repositório de fornecedores do modo comprador.
```

- [ ] **Step 7: Atualizar TradingSupplierService.java**

Substituir a linha do Javadoc que contém "atravessador":
```java
// ANTES:
 * Serviço de gerenciamento de fornecedores do modo atravessador.

// DEPOIS:
 * Serviço de gerenciamento de fornecedores do modo comprador.
```

- [ ] **Step 8: Verificar que nenhuma ocorrência restou nos arquivos Java**

```powershell
Select-String -Path "src\main\java\**\*.java" -Pattern "atravessador" -Recurse
```

Esperado: nenhum resultado.

---

### Task 2: Atualizar comentários nas migrations SQL

**Files:**
- Modify: `src/main/resources/db/migration/V12__create_trading_suppliers_table.sql:1-2`
- Modify: `src/main/resources/db/migration/V13__create_purchase_lots_table.sql:1`

> **Nota:** Migrations Flyway já executadas não re-executam. Alterar o comentário no arquivo é seguro — o Flyway usa checksum apenas para detectar alterações em arquivos já aplicados e lançaria erro. Verifique se a migration já foi aplicada antes de editar.
>
> **Ação segura:** Se a migration já foi aplicada ao banco de produção/homologação, atualize o checksum no schema_version ou use `flyway repair` após a alteração. Em desenvolvimento local, basta editar normalmente.

- [ ] **Step 1: Atualizar V12__create_trading_suppliers_table.sql**

Substituir linhas 1-2:
```sql
-- ANTES:
-- Cadastro de fornecedores do modo atravessador.
-- Cada fornecedor é um produtor de quem o atravessador compra melancia.

-- DEPOIS:
-- Cadastro de fornecedores do modo comprador.
-- Cada fornecedor é um produtor de quem o comprador compra melancia.
```

- [ ] **Step 2: Atualizar V13__create_purchase_lots_table.sql**

Substituir linha 1:
```sql
-- ANTES:
-- Lotes de compra do modo atravessador.

-- DEPOIS:
-- Lotes de compra do modo comprador.
```

- [ ] **Step 3: Executar `flyway repair` se as migrations já foram aplicadas**

```powershell
./mvnw flyway:repair
```

Esperado: `Successfully repaired schema history table` (ou mensagem indicando que não havia nada para reparar).

Se o ambiente for apenas local/desenvolvimento e as migrations não foram aplicadas ainda, este passo pode ser ignorado.

- [ ] **Step 4: Verificar que nenhuma ocorrência restou**

```powershell
Select-String -Path "src\main\resources\**\*.sql" -Pattern "atravessador" -Recurse
```

Esperado: nenhum resultado.

---

### Task 3: Verificação final e commit

- [ ] **Step 1: Verificação global — zero ocorrências restantes**

```powershell
Select-String -Path "src\**" -Pattern "atravessador" -Recurse
```

Esperado: nenhum resultado em nenhum arquivo.

- [ ] **Step 2: Build para garantir que nenhum arquivo foi corrompido**

```powershell
./mvnw compile -q
```

Esperado: `BUILD SUCCESS`

- [ ] **Step 3: Commit**

```powershell
git add `
  src/main/java/br/com/hadryan/agro/manager/domain/trading/TradingSupplier.java `
  src/main/java/br/com/hadryan/agro/manager/domain/trading/PurchaseLot.java `
  src/main/java/br/com/hadryan/agro/manager/domain/trading/PurchaseLotStatus.java `
  src/main/java/br/com/hadryan/agro/manager/domain/trading/TradingDashboardResponse.java `
  src/main/java/br/com/hadryan/agro/manager/domain/trading/TradingSupplierController.java `
  src/main/java/br/com/hadryan/agro/manager/domain/trading/TradingSupplierRepository.java `
  src/main/java/br/com/hadryan/agro/manager/domain/trading/TradingSupplierService.java `
  src/main/resources/db/migration/V12__create_trading_suppliers_table.sql `
  src/main/resources/db/migration/V13__create_purchase_lots_table.sql

git commit -m "docs: rename atravessador to comprador in comments"
```

---

## Resumo de Viabilidade

| Aspecto | Avaliação |
|---------|-----------|
| Risco | **Nenhum** — apenas comentários |
| Impacto em API | **Nenhum** — nomes de campos não mudam |
| Impacto em banco | **Nenhum** — colunas não mudam; ver nota sobre Flyway checksum |
| Testes necessários | **Nenhum** — comportamento não muda |
| Esforço estimado | ~10 minutos |
