package br.com.hadryan.agro.manager;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Dashboard — Métricas agregadas da conta")
class DashboardIntegrationTest extends MockMvcIntegrationTestBase {

    @Test
    @DisplayName("Deve retornar métricas zeradas quando a conta não tem lavouras")
    void shouldReturnEmptyDashboardWhenNoFarmsExist() throws Exception {
        String token = registerAndGetToken(uniqueEmail(), "senha12345");
        String accountId = createAccount(token, "Conta Vazia");

        mockMvc.perform(get("/accounts/" + accountId + "/dashboard")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalFarms").value(0))
                .andExpect(jsonPath("$.data.emPreparacao").value(0))
                .andExpect(jsonPath("$.data.emAndamento").value(0))
                .andExpect(jsonPath("$.data.colhida").value(0))
                .andExpect(jsonPath("$.data.cancelada").value(0))
                .andExpect(jsonPath("$.data.totalExpenses").value(0))
                .andExpect(jsonPath("$.data.totalExpensesPaid").value(0))
                .andExpect(jsonPath("$.data.totalExpensesPending").value(0))
                .andExpect(jsonPath("$.data.recentFarms").isArray())
                .andExpect(jsonPath("$.data.recentFarms.length()").value(0));
    }

    @Test
    @DisplayName("Deve contabilizar lavouras por status corretamente")
    void shouldCountFarmsByStatusCorrectly() throws Exception {
        String token = registerAndGetToken(uniqueEmail(), "senha12345");
        String accountId = createAccount(token, "Fazenda Métricas");

        // Lavoura em preparação (sem datas de plantio)
        createFarm(token, accountId, "Lavoura Preparação");

        // Lavoura em andamento (com data de plantio)
        Map<String, Object> farmAndamento = new HashMap<>();
        farmAndamento.put("name", "Lavoura Em Andamento");
        farmAndamento.put("areaValue", 30.0);
        farmAndamento.put("areaUnit", "HECTARE");
        farmAndamento.put("cancelled", false);
        farmAndamento.put("plantingStartDate", "2025-02-01");

        mockMvc.perform(post("/accounts/" + accountId + "/farms")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(farmAndamento)))
                .andExpect(status().isCreated());

        // Lavoura com colheita iniciada
        Map<String, Object> farmColhida = new HashMap<>();
        farmColhida.put("name", "Lavoura Colhida");
        farmColhida.put("areaValue", 20.0);
        farmColhida.put("areaUnit", "HECTARE");
        farmColhida.put("cancelled", false);
        farmColhida.put("plantingStartDate", "2025-01-01");
        farmColhida.put("harvestStartDate", "2025-04-01");

        mockMvc.perform(post("/accounts/" + accountId + "/farms")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(farmColhida)))
                .andExpect(status().isCreated());

        // Lavoura cancelada
        Map<String, Object> farmCancelada = new HashMap<>();
        farmCancelada.put("name", "Lavoura Cancelada");
        farmCancelada.put("areaValue", 10.0);
        farmCancelada.put("areaUnit", "HECTARE");
        farmCancelada.put("cancelled", true);

        mockMvc.perform(post("/accounts/" + accountId + "/farms")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(farmCancelada)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/accounts/" + accountId + "/dashboard")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalFarms").value(4))
                .andExpect(jsonPath("$.data.emPreparacao").value(1))
                .andExpect(jsonPath("$.data.emAndamento").value(1))
                .andExpect(jsonPath("$.data.colhida").value(1))
                .andExpect(jsonPath("$.data.cancelada").value(1));
    }

    @Test
    @DisplayName("Deve calcular totais financeiros corretamente após criação de despesas")
    void shouldCalculateFinancialTotalsCorrectly() throws Exception {
        String token = registerAndGetToken(uniqueEmail(), "senha12345");
        String accountId = createAccount(token, "Fazenda Financeira");
        String farmId = createFarm(token, accountId, "Lavoura Principal");

        // Despesa paga (com data de pagamento definida na criação)
        Map<String, Object> expensePaga = new HashMap<>();
        expensePaga.put("description", "Sementes pagas");
        expensePaga.put("category", "INSUMO");
        expensePaga.put("value", 5000.0);
        expensePaga.put("competenceDate", "2025-01-10");
        expensePaga.put("paymentDate", "2025-01-15");

        mockMvc.perform(post("/accounts/" + accountId + "/farms/" + farmId + "/expenses")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(expensePaga)))
                .andExpect(status().isCreated());

        // Despesa pendente (sem data de pagamento)
        createExpense(token, accountId, farmId,
                "Fertilizante pendente", "INSUMO", 3000.0, "2025-01-20");

        mockMvc.perform(get("/accounts/" + accountId + "/dashboard")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalExpenses").value(8000.0))
                .andExpect(jsonPath("$.data.totalExpensesPaid").value(5000.0))
                .andExpect(jsonPath("$.data.totalExpensesPending").value(3000.0));
    }

    @Test
    @DisplayName("Deve retornar as lavouras recentes com seus totais individuais")
    void shouldReturnRecentFarmsWithIndividualTotals() throws Exception {
        String token = registerAndGetToken(uniqueEmail(), "senha12345");
        String accountId = createAccount(token, "Fazenda Recentes");
        String farmId = createFarm(token, accountId, "Lavoura Com Despesas");

        createExpense(token, accountId, farmId, "Insumo 1", "INSUMO", 1000.0, "2025-01-10");
        createExpense(token, accountId, farmId, "Insumo 2", "INSUMO", 500.0, "2025-01-15");

        mockMvc.perform(get("/accounts/" + accountId + "/dashboard")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recentFarms").isArray())
                .andExpect(jsonPath("$.data.recentFarms.length()").value(1))
                .andExpect(jsonPath("$.data.recentFarms[0].name").value("Lavoura Com Despesas"))
                .andExpect(jsonPath("$.data.recentFarms[0].totalExpenses").value(1500.0));
    }

    @Test
    @DisplayName("Deve retornar erro 403 ao tentar acessar dashboard de conta que o usuário não pertence")
    void shouldReturn403WhenAccessingAnotherAccountDashboard() throws Exception {
        String tokenA = registerAndGetToken(uniqueEmail(), "senha12345");
        String tokenB = registerAndGetToken(uniqueEmail(), "senha12345");
        String accountIdA = createAccount(tokenA, "Conta do Usuário A");

        // Usuário B tenta acessar dashboard da conta A
        mockMvc.perform(get("/accounts/" + accountIdA + "/dashboard")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden());
    }
}