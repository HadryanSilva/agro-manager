package br.com.hadryan.agro.manager;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Despesas — CRUD de despesas de lavoura e gerais")
class ExpenseIntegrationTest extends MockMvcIntegrationTestBase {

    // Contexto com usuário, conta e lavoura já criados
    private record TestContext(String token, String accountId, String farmId) {}

    private TestContext setup() throws Exception {
        String token = registerAndGetToken(uniqueEmail(), "senha12345");
        String accountId = createAccount(token, "Fazenda Teste");
        String farmId = createFarm(token, accountId, "Lavoura Principal");
        return new TestContext(token, accountId, farmId);
    }

    @Test
    @DisplayName("Deve criar despesa de lavoura (INSUMO) com sucesso — reproduz o bug de account_id nulo")
    void shouldCreateFarmExpenseSuccessfully() throws Exception {
        var ctx = setup();

        // Reproduz exatamente o cenário que foi para produção com bug:
        // despesa do tipo INSUMO vinculada a uma lavoura → account_id estava nulo
        Map<String, Object> payload = new HashMap<>();
        payload.put("description", "Sementes de melancia Tropikalia");
        payload.put("category", "INSUMO");
        payload.put("value", 8500.00);
        payload.put("competenceDate", "2025-04-18");

        mockMvc.perform(post("/accounts/" + ctx.accountId() + "/farms/" + ctx.farmId() + "/expenses")
                        .header("Authorization", "Bearer " + ctx.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.description").value("Sementes de melancia Tropikalia"))
                .andExpect(jsonPath("$.data.category").value("INSUMO"))
                .andExpect(jsonPath("$.data.paid").value(false))
                .andExpect(jsonPath("$.data.farmId").value(ctx.farmId()))
                .andExpect(jsonPath("$.data.id").isNotEmpty());
    }

    @Test
    @DisplayName("Deve criar despesa do tipo SERVICO com sucesso")
    void shouldCreateServiceExpenseSuccessfully() throws Exception {
        var ctx = setup();

        Map<String, Object> payload = new HashMap<>();
        payload.put("description", "Aluguel de trator");
        payload.put("category", "SERVICO");
        payload.put("value", 3200.00);
        payload.put("competenceDate", "2025-03-10");

        mockMvc.perform(post("/accounts/" + ctx.accountId() + "/farms/" + ctx.farmId() + "/expenses")
                        .header("Authorization", "Bearer " + ctx.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.category").value("SERVICO"))
                .andExpect(jsonPath("$.data.farmId").value(ctx.farmId()));
    }

    @Test
    @DisplayName("Deve criar despesa com data de pagamento e retornar como paga")
    void shouldCreateExpenseWithPaymentDateAsPaid() throws Exception {
        var ctx = setup();

        Map<String, Object> payload = new HashMap<>();
        payload.put("description", "Fertilizante pago");
        payload.put("category", "INSUMO");
        payload.put("value", 1200.00);
        payload.put("competenceDate", "2025-01-10");
        payload.put("paymentDate", "2025-01-12");

        mockMvc.perform(post("/accounts/" + ctx.accountId() + "/farms/" + ctx.farmId() + "/expenses")
                        .header("Authorization", "Bearer " + ctx.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.paid").value(true))
                .andExpect(jsonPath("$.data.paymentDate").isNotEmpty());
    }

    @Test
    @DisplayName("Deve listar despesas de uma lavoura ordenadas pela competência mais recente")
    void shouldListFarmExpenses() throws Exception {
        var ctx = setup();
        createExpense(ctx.token(), ctx.accountId(), ctx.farmId(),
                "Despesa Mais Antiga", "INSUMO", 1000.0, "2025-01-10");
        createExpense(ctx.token(), ctx.accountId(), ctx.farmId(),
                "Despesa Mais Recente", "SERVICO", 2000.0, "2025-02-15");

        mockMvc.perform(get("/accounts/" + ctx.accountId() + "/farms/" + ctx.farmId() + "/expenses")
                        .header("Authorization", "Bearer " + ctx.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(2));
    }

    @Test
    @DisplayName("Deve buscar despesa pelo ID")
    void shouldGetExpenseById() throws Exception {
        var ctx = setup();
        String expenseId = createExpense(ctx.token(), ctx.accountId(), ctx.farmId(),
                "Fertilizante específico", "INSUMO", 500.0, "2025-02-01");

        mockMvc.perform(get("/accounts/" + ctx.accountId() + "/farms/" + ctx.farmId() + "/expenses/" + expenseId)
                        .header("Authorization", "Bearer " + ctx.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(expenseId))
                .andExpect(jsonPath("$.data.description").value("Fertilizante específico"));
    }

    @Test
    @DisplayName("Deve marcar despesa como paga e preencher a data de pagamento")
    void shouldMarkExpenseAsPaid() throws Exception {
        var ctx = setup();
        String expenseId = createExpense(ctx.token(), ctx.accountId(), ctx.farmId(),
                "Defensivo agrícola", "INSUMO", 2500.0, "2025-02-15");

        mockMvc.perform(patch("/accounts/" + ctx.accountId() + "/farms/" + ctx.farmId()
                        + "/expenses/" + expenseId + "/pay")
                        .header("Authorization", "Bearer " + ctx.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.paid").value(true))
                .andExpect(jsonPath("$.data.paymentDate").isNotEmpty());
    }

    @Test
    @DisplayName("Deve atualizar dados de uma despesa existente")
    void shouldUpdateExpense() throws Exception {
        var ctx = setup();
        String expenseId = createExpense(ctx.token(), ctx.accountId(), ctx.farmId(),
                "Descrição Original", "INSUMO", 100.0, "2025-01-01");

        Map<String, Object> updatePayload = new HashMap<>();
        updatePayload.put("description", "Descrição Corrigida");
        updatePayload.put("category", "SERVICO");
        updatePayload.put("value", 150.0);
        updatePayload.put("competenceDate", "2025-01-05");

        mockMvc.perform(put("/accounts/" + ctx.accountId() + "/farms/" + ctx.farmId()
                        + "/expenses/" + expenseId)
                        .header("Authorization", "Bearer " + ctx.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatePayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.description").value("Descrição Corrigida"))
                .andExpect(jsonPath("$.data.category").value("SERVICO"));
    }

    @Test
    @DisplayName("Deve excluir despesa e remover da listagem — retorna 204 No Content")
    void shouldDeleteExpenseAndRemoveFromList() throws Exception {
        var ctx = setup();
        String expenseId = createExpense(ctx.token(), ctx.accountId(), ctx.farmId(),
                "Despesa Para Deletar", "INSUMO", 200.0, "2025-01-20");

        // DELETE retorna 204 sem body (padrão REST correto)
        mockMvc.perform(delete("/accounts/" + ctx.accountId() + "/farms/" + ctx.farmId()
                        + "/expenses/" + expenseId)
                        .header("Authorization", "Bearer " + ctx.token()))
                .andExpect(status().isNoContent());

        // Verifica que a listagem ficou vazia após a exclusão
        mockMvc.perform(get("/accounts/" + ctx.accountId() + "/farms/" + ctx.farmId() + "/expenses")
                        .header("Authorization", "Bearer " + ctx.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(0));
    }

    @Test
    @DisplayName("Deve criar despesa geral da conta sem lavoura vinculada")
    void shouldCreateGeneralAccountExpense() throws Exception {
        String token = registerAndGetToken(uniqueEmail(), "senha12345");
        String accountId = createAccount(token, "Conta Geral");

        Map<String, Object> payload = new HashMap<>();
        payload.put("description", "Material de escritório");
        payload.put("category", "SERVICO");
        payload.put("value", 350.0);
        payload.put("competenceDate", "2025-03-01");

        // Endpoint de despesa geral (sem farmId no path)
        mockMvc.perform(post("/accounts/" + accountId + "/expenses")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.description").value("Material de escritório"))
                // farmId deve ser nulo em despesas gerais
                .andExpect(jsonPath("$.data.farmId").doesNotExist());
    }

    @Test
    @DisplayName("Deve rejeitar criação de despesa sem campos obrigatórios")
    void shouldRejectExpenseWithMissingRequiredFields() throws Exception {
        var ctx = setup();

        // Payload incompleto — faltam description, value e competenceDate
        Map<String, Object> payload = Map.of("category", "INSUMO");

        mockMvc.perform(post("/accounts/" + ctx.accountId() + "/farms/" + ctx.farmId() + "/expenses")
                        .header("Authorization", "Bearer " + ctx.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Não deve registrar atividade no histórico quando a criação da despesa falha")
    void shouldNotRecordActivityWhenExpenseCreationFails() throws Exception {
        var ctx = setup();

        // Tentativa de criar despesa com dados inválidos (sem campos obrigatórios)
        mockMvc.perform(post("/accounts/" + ctx.accountId() + "/farms/" + ctx.farmId() + "/expenses")
                        .header("Authorization", "Bearer " + ctx.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("category", "INSUMO"))))
                .andExpect(status().isBadRequest());

        // O histórico da lavoura deve continuar vazio
        mockMvc.perform(get("/accounts/" + ctx.accountId() + "/farms/" + ctx.farmId() + "/activities")
                        .header("Authorization", "Bearer " + ctx.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    @DisplayName("Deve criar despesa como compra no prazo com data de vencimento")
    void shouldCreateCreditPurchaseWithDueDate() throws Exception {
        var ctx = setup();

        Map<String, Object> payload = new HashMap<>();
        payload.put("description", "Sementes a prazo");
        payload.put("category", "INSUMO");
        payload.put("value", 5000.00);
        payload.put("competenceDate", "2025-04-01");
        payload.put("creditPurchase", true);
        payload.put("dueDate", "2025-05-15");

        mockMvc.perform(post("/accounts/" + ctx.accountId() + "/farms/" + ctx.farmId() + "/expenses")
                        .header("Authorization", "Bearer " + ctx.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.creditPurchase").value(true))
                .andExpect(jsonPath("$.data.dueDate").value("2025-05-15"));
    }

    @Test
    @DisplayName("Deve retornar creditPurchase false em despesas normais")
    void shouldReturnFalseForCreditPurchaseOnNormalExpenses() throws Exception {
        var ctx = setup();
        String expenseId = createExpense(ctx.token(), ctx.accountId(), ctx.farmId(),
                "Despesa Normal", "INSUMO", 1000.0, "2025-01-01");

        mockMvc.perform(get("/accounts/" + ctx.accountId() + "/farms/" + ctx.farmId() + "/expenses/" + expenseId)
                        .header("Authorization", "Bearer " + ctx.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.creditPurchase").value(false));
    }

    @Test
    @DisplayName("Deve rejeitar compra no prazo sem data de vencimento — retorna 400")
    void shouldRejectCreditPurchaseWithoutDueDate() throws Exception {
        var ctx = setup();

        Map<String, Object> payload = new HashMap<>();
        payload.put("description", "Compra sem vencimento");
        payload.put("category", "INSUMO");
        payload.put("value", 1000.00);
        payload.put("competenceDate", "2025-04-01");
        payload.put("creditPurchase", true);
        // dueDate ausente

        mockMvc.perform(post("/accounts/" + ctx.accountId() + "/farms/" + ctx.farmId() + "/expenses")
                        .header("Authorization", "Bearer " + ctx.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Deve retornar despesas próximas do vencimento via /upcoming")
    void shouldReturnUpcomingCreditExpenses() throws Exception {
        var ctx = setup();

        // Despesa com vencimento em 3 dias — deve aparecer no /upcoming (janela padrão: 7 dias)
        Map<String, Object> upcoming = new HashMap<>();
        upcoming.put("description", "Insumo com vencimento próximo");
        upcoming.put("category", "INSUMO");
        upcoming.put("value", 3000.00);
        upcoming.put("competenceDate", "2025-04-01");
        upcoming.put("creditPurchase", true);
        upcoming.put("dueDate", LocalDate.now().plusDays(3).toString());

        mockMvc.perform(post("/accounts/" + ctx.accountId() + "/farms/" + ctx.farmId() + "/expenses")
                        .header("Authorization", "Bearer " + ctx.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(upcoming)))
                .andExpect(status().isCreated());

        // Despesa paga não deve aparecer
        Map<String, Object> paid = new HashMap<>();
        paid.put("description", "Despesa Já Paga");
        paid.put("category", "INSUMO");
        paid.put("value", 500.00);
        paid.put("competenceDate", "2025-04-01");
        paid.put("creditPurchase", true);
        paid.put("dueDate", LocalDate.now().plusDays(2).toString());
        paid.put("paymentDate", "2025-04-15");

        mockMvc.perform(post("/accounts/" + ctx.accountId() + "/farms/" + ctx.farmId() + "/expenses")
                        .header("Authorization", "Bearer " + ctx.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paid)))
                .andExpect(status().isCreated());

        // Despesa sem creditPurchase não deve aparecer
        createExpense(ctx.token(), ctx.accountId(), ctx.farmId(),
                "Despesa Normal", "SERVICO", 200.0, "2025-04-01");

        mockMvc.perform(get("/accounts/" + ctx.accountId() + "/expenses/upcoming")
                        .header("Authorization", "Bearer " + ctx.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].description").value("Insumo com vencimento próximo"));
    }

    @Test
    @DisplayName("Deve negar acesso a /upcoming de outra conta — retorna 403")
    void shouldDenyUpcomingAccessFromDifferentAccount() throws Exception {
        // Account A — creates a credit expense
        var ctxA = setup();
        Map<String, Object> payload = new HashMap<>();
        payload.put("description", "Despesa da conta A");
        payload.put("category", "INSUMO");
        payload.put("value", 1000.00);
        payload.put("competenceDate", "2025-04-01");
        payload.put("creditPurchase", true);
        payload.put("dueDate", java.time.LocalDate.now().plusDays(3).toString());

        mockMvc.perform(post("/accounts/" + ctxA.accountId() + "/farms/" + ctxA.farmId() + "/expenses")
                        .header("Authorization", "Bearer " + ctxA.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated());

        // Account B — attempts to read Account A's /upcoming
        var ctxB = setup();
        mockMvc.perform(get("/accounts/" + ctxA.accountId() + "/expenses/upcoming")
                        .header("Authorization", "Bearer " + ctxB.token()))
                .andExpect(status().isForbidden());
    }
}