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
}
