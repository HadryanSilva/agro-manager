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
