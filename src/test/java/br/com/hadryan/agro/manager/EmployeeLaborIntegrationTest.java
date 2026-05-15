package br.com.hadryan.agro.manager;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Mao de obra - funcionarios, diarias e pagamentos")
class EmployeeLaborIntegrationTest extends MockMvcIntegrationTestBase {

    private record LaborContext(String token, String accountId, String farmId) {}

    private LaborContext setup() throws Exception {
        String token = registerAndGetToken(uniqueEmail(), "senha12345");
        String accountId = createAccount(token, "Fazenda Labor");
        String farmId = createFarm(token, accountId, "Lavoura Labor");
        return new LaborContext(token, accountId, farmId);
    }

    @Test
    @DisplayName("Deve criar, listar, atualizar, desativar e ativar funcionario")
    void shouldManageEmployeeLifecycle() throws Exception {
        var ctx = setup();

        String employeeId = createEmployee(ctx.token(), ctx.accountId(), "Joao Diarista", 120.00);

        mockMvc.perform(get("/accounts/" + ctx.accountId() + "/employees")
                        .header("Authorization", "Bearer " + ctx.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(employeeId))
                .andExpect(jsonPath("$.data[0].name").value("Joao Diarista"))
                .andExpect(jsonPath("$.data[0].dailyRate").value(120.00))
                .andExpect(jsonPath("$.data[0].active").value(true));

        mockMvc.perform(get("/accounts/" + ctx.accountId() + "/employees")
                        .param("active", "true")
                        .header("Authorization", "Bearer " + ctx.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(employeeId));

        mockMvc.perform(get("/accounts/" + ctx.accountId() + "/employees/" + employeeId)
                        .header("Authorization", "Bearer " + ctx.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(employeeId))
                .andExpect(jsonPath("$.data.notes").doesNotExist());

        Map<String, Object> updatePayload = new HashMap<>();
        updatePayload.put("name", "Joao Atualizado");
        updatePayload.put("dailyRate", 130.00);
        updatePayload.put("notes", "Equipe de colheita");

        mockMvc.perform(put("/accounts/" + ctx.accountId() + "/employees/" + employeeId)
                        .header("Authorization", "Bearer " + ctx.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatePayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Joao Atualizado"))
                .andExpect(jsonPath("$.data.dailyRate").value(130.00))
                .andExpect(jsonPath("$.data.notes").value("Equipe de colheita"));

        mockMvc.perform(patch("/accounts/" + ctx.accountId() + "/employees/" + employeeId + "/deactivate")
                        .header("Authorization", "Bearer " + ctx.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active").value(false));

        mockMvc.perform(get("/accounts/" + ctx.accountId() + "/employees")
                        .param("active", "true")
                        .header("Authorization", "Bearer " + ctx.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));

        mockMvc.perform(patch("/accounts/" + ctx.accountId() + "/employees/" + employeeId + "/activate")
                        .header("Authorization", "Bearer " + ctx.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active").value(true));
    }

    private String createEmployee(String token, String accountId, String name, double dailyRate) throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("name", name);
        payload.put("dailyRate", dailyRate);

        MvcResult result = mockMvc.perform(post("/accounts/" + accountId + "/employees")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("id").asText();
    }

    @Test
    @DisplayName("Deve criar diaria geral e diaria vinculada a lavoura")
    void shouldCreateGeneralAndFarmWorkEntries() throws Exception {
        var ctx = setup();
        String employeeId = createEmployee(ctx.token(), ctx.accountId(), "Maria Diarista", 100.00);

        String generalEntryId = createWorkEntry(ctx.token(), ctx.accountId(), employeeId, null, "2026-05-11", null);
        String farmEntryId = createWorkEntry(ctx.token(), ctx.accountId(), employeeId, ctx.farmId(), "2026-05-12", 140.00);

        mockMvc.perform(get("/accounts/" + ctx.accountId() + "/employee-work-entries")
                        .param("employeeId", employeeId)
                        .param("paid", "false")
                        .header("Authorization", "Bearer " + ctx.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.content[0].id").value(farmEntryId))
                .andExpect(jsonPath("$.data.content[0].farmId").value(ctx.farmId()))
                .andExpect(jsonPath("$.data.content[0].dailyRate").value(140.00))
                .andExpect(jsonPath("$.data.content[0].paid").value(false))
                .andExpect(jsonPath("$.data.content[1].id").value(generalEntryId))
                .andExpect(jsonPath("$.data.content[1].farmId").doesNotExist())
                .andExpect(jsonPath("$.data.content[1].dailyRate").value(100.00));
    }

    @Test
    @DisplayName("Deve rejeitar diaria duplicada e diaria para funcionario inativo")
    void shouldRejectDuplicateAndInactiveEmployeeWorkEntry() throws Exception {
        var ctx = setup();
        String employeeId = createEmployee(ctx.token(), ctx.accountId(), "Pedro Diarista", 90.00);
        createWorkEntry(ctx.token(), ctx.accountId(), employeeId, null, "2026-05-13", null);

        Map<String, Object> duplicatePayload = new HashMap<>();
        duplicatePayload.put("employeeId", employeeId);
        duplicatePayload.put("workDate", "2026-05-13");

        mockMvc.perform(post("/accounts/" + ctx.accountId() + "/employee-work-entries")
                        .header("Authorization", "Bearer " + ctx.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicatePayload)))
                .andExpect(status().isConflict());

        mockMvc.perform(patch("/accounts/" + ctx.accountId() + "/employees/" + employeeId + "/deactivate")
                        .header("Authorization", "Bearer " + ctx.token()))
                .andExpect(status().isOk());

        Map<String, Object> inactivePayload = new HashMap<>();
        inactivePayload.put("employeeId", employeeId);
        inactivePayload.put("workDate", "2026-05-14");

        mockMvc.perform(post("/accounts/" + ctx.accountId() + "/employee-work-entries")
                        .header("Authorization", "Bearer " + ctx.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inactivePayload)))
                .andExpect(status().isBadRequest());
    }

    private String createWorkEntry(String token, String accountId, String employeeId, String farmId,
                                   String workDate, Double dailyRate) throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("employeeId", employeeId);
        if (farmId != null) {
            payload.put("farmId", farmId);
        }
        payload.put("workDate", workDate);
        if (dailyRate != null) {
            payload.put("dailyRate", dailyRate);
        }

        MvcResult result = mockMvc.perform(post("/accounts/" + accountId + "/employee-work-entries")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("id").asText();
    }
}
