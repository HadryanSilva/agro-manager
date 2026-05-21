package br.com.hadryan.agro.manager;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Lavouras — CRUD de lavouras")
class FarmIntegrationTest extends MockMvcIntegrationTestBase {

    // Contexto reutilizável para reduzir setup repetitivo
    private record TestContext(String token, String accountId) {}

    private TestContext setup() throws Exception {
        String token = registerAndGetToken(uniqueEmail(), "senha12345");
        String accountId = createAccount(token, "Fazenda Teste");
        return new TestContext(token, accountId);
    }

    @Test
    @DisplayName("Deve criar lavoura com sucesso e retornar status EM_PREPARACAO")
    void shouldCreateFarmSuccessfully() throws Exception {
        var ctx = setup();

        Map<String, Object> payload = new HashMap<>();
        payload.put("name", "Lavoura São João");
        payload.put("areaValue", 75.5);
        payload.put("areaUnit", "HECTARE");
        payload.put("cancelled", false);

        mockMvc.perform(post("/accounts/" + ctx.accountId() + "/farms")
                        .header("Authorization", "Bearer " + ctx.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Lavoura São João"))
                .andExpect(jsonPath("$.data.areaUnit").value("HECTARE"))
                // Sem data de plantio → status calculado deve ser EM_PREPARACAO
                .andExpect(jsonPath("$.data.status").value("EM_PREPARACAO"))
                .andExpect(jsonPath("$.data.cancelled").value(false))
                .andExpect(jsonPath("$.data.id").isNotEmpty());
    }

    @Test
    @DisplayName("Deve listar todas as lavouras da conta")
    void shouldListAllFarms() throws Exception {
        var ctx = setup();
        createFarm(ctx.token(), ctx.accountId(), "Lavoura A");
        createFarm(ctx.token(), ctx.accountId(), "Lavoura B");
        createFarm(ctx.token(), ctx.accountId(), "Lavoura C");

        mockMvc.perform(get("/accounts/" + ctx.accountId() + "/farms")
                        .header("Authorization", "Bearer " + ctx.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(3));
    }

    @Test
    @DisplayName("Deve buscar lavoura pelo ID")
    void shouldGetFarmById() throws Exception {
        var ctx = setup();
        String farmId = createFarm(ctx.token(), ctx.accountId(), "Lavoura Específica");

        mockMvc.perform(get("/accounts/" + ctx.accountId() + "/farms/" + farmId)
                        .header("Authorization", "Bearer " + ctx.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(farmId))
                .andExpect(jsonPath("$.data.name").value("Lavoura Específica"));
    }

    @Test
    @DisplayName("Deve atualizar lavoura e refletir novo status calculado")
    void shouldUpdateFarmAndReflectNewStatus() throws Exception {
        var ctx = setup();
        String farmId = createFarm(ctx.token(), ctx.accountId(), "Lavoura Para Atualizar");

        // Adiciona data de plantio → status passa para EM_ANDAMENTO
        Map<String, Object> updatePayload = new HashMap<>();
        updatePayload.put("name", "Lavoura Atualizada");
        updatePayload.put("areaValue", 100.0);
        updatePayload.put("areaUnit", "ALQUEIRE");
        updatePayload.put("cancelled", false);
        updatePayload.put("plantingStartDate", "2025-03-01");

        mockMvc.perform(put("/accounts/" + ctx.accountId() + "/farms/" + farmId)
                        .header("Authorization", "Bearer " + ctx.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatePayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Lavoura Atualizada"))
                .andExpect(jsonPath("$.data.areaUnit").value("ALQUEIRE"))
                .andExpect(jsonPath("$.data.status").value("EM_ANDAMENTO"));
    }

    @Test
    @DisplayName("Deve excluir lavoura e removê-la da listagem — retorna 204 No Content")
    void shouldDeleteFarmAndRemoveFromList() throws Exception {
        var ctx = setup();
        String farmId = createFarm(ctx.token(), ctx.accountId(), "Lavoura Para Deletar");

        // DELETE retorna 204 sem body (padrão REST correto)
        mockMvc.perform(delete("/accounts/" + ctx.accountId() + "/farms/" + farmId)
                        .header("Authorization", "Bearer " + ctx.token()))
                .andExpect(status().isNoContent());

        // Verifica que a lista ficou vazia após a exclusão
        mockMvc.perform(get("/accounts/" + ctx.accountId() + "/farms")
                        .header("Authorization", "Bearer " + ctx.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    @DisplayName("Deve filtrar lavouras por status corretamente")
    void shouldFilterFarmsByStatus() throws Exception {
        var ctx = setup();

        // Lavoura em preparação (sem datas)
        createFarm(ctx.token(), ctx.accountId(), "Lavoura Preparação");

        // Lavoura em andamento (com data de plantio)
        Map<String, Object> farmAndamento = new HashMap<>();
        farmAndamento.put("name", "Lavoura Em Andamento");
        farmAndamento.put("areaValue", 30.0);
        farmAndamento.put("areaUnit", "HECTARE");
        farmAndamento.put("cancelled", false);
        farmAndamento.put("plantingStartDate", "2025-02-01");

        mockMvc.perform(post("/accounts/" + ctx.accountId() + "/farms")
                        .header("Authorization", "Bearer " + ctx.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(farmAndamento)))
                .andExpect(status().isCreated());

        // Filtra apenas EM_PREPARACAO — deve retornar apenas 1
        mockMvc.perform(get("/accounts/" + ctx.accountId() + "/farms")
                        .param("status", "EM_PREPARACAO")
                        .header("Authorization", "Bearer " + ctx.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].status").value("EM_PREPARACAO"));

        // Filtra apenas EM_ANDAMENTO — deve retornar apenas 1
        mockMvc.perform(get("/accounts/" + ctx.accountId() + "/farms")
                        .param("status", "EM_ANDAMENTO")
                        .header("Authorization", "Bearer " + ctx.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].status").value("EM_ANDAMENTO"));
    }

    @Test
    @DisplayName("Deve retornar 404 ao buscar lavoura de outra conta")
    void shouldReturn404WhenFarmBelongsToAnotherAccount() throws Exception {
        String tokenA = registerAndGetToken(uniqueEmail(), "senha12345");
        String tokenB = registerAndGetToken(uniqueEmail(), "senha12345");
        String accountA = createAccount(tokenA, "Conta A");
        String accountB = createAccount(tokenB, "Conta B");

        String farmIdFromA = createFarm(tokenA, accountA, "Lavoura Secreta");

        // Usuário B tenta acessar lavoura da conta A
        mockMvc.perform(get("/accounts/" + accountB + "/farms/" + farmIdFromA)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Deve criar lavoura como terra própria sem dados de arrendamento")
    void shouldCreateFarmAsOwnLand() throws Exception {
        var ctx = setup();

        Map<String, Object> payload = new HashMap<>();
        payload.put("name", "Lavoura Terra Própria");
        payload.put("areaValue", 50.0);
        payload.put("areaUnit", "HECTARE");
        payload.put("cancelled", false);
        payload.put("ownLand", true);

        mockMvc.perform(post("/accounts/" + ctx.accountId() + "/farms")
                        .header("Authorization", "Bearer " + ctx.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.ownLand").value(true))
                .andExpect(jsonPath("$.data.lessorName", nullValue()));
    }

    @Test
    @DisplayName("Deve atualizar lavoura para terra própria limpando dados de arrendamento")
    void shouldUpdateFarmToOwnLandClearingLeaseData() throws Exception {
        var ctx = setup();

        // Cria com arrendamento
        Map<String, Object> createPayload = new HashMap<>();
        createPayload.put("name", "Lavoura Arrendada");
        createPayload.put("areaValue", 30.0);
        createPayload.put("areaUnit", "HECTARE");
        createPayload.put("cancelled", false);
        createPayload.put("ownLand", false);
        createPayload.put("lessorName", "João da Silva");
        createPayload.put("leaseValue", 5000.0);

        String farmId = objectMapper.readTree(
                mockMvc.perform(post("/accounts/" + ctx.accountId() + "/farms")
                                .header("Authorization", "Bearer " + ctx.token())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(createPayload)))
                        .andReturn().getResponse().getContentAsString()
        ).at("/data/id").asText();

        // Atualiza para terra própria (frontend zera campos de arrendamento)
        Map<String, Object> updatePayload = new HashMap<>();
        updatePayload.put("name", "Lavoura Arrendada");
        updatePayload.put("areaValue", 30.0);
        updatePayload.put("areaUnit", "HECTARE");
        updatePayload.put("cancelled", false);
        updatePayload.put("ownLand", true);

        mockMvc.perform(put("/accounts/" + ctx.accountId() + "/farms/" + farmId)
                        .header("Authorization", "Bearer " + ctx.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatePayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ownLand").value(true))
                .andExpect(jsonPath("$.data.lessorName", nullValue()));
    }
}