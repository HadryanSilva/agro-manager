package br.com.hadryan.agro.manager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Classe base para testes de integração via MockMvc.
 * Fornece utilitários para operações comuns como autenticação e criação de entidades,
 * evitando duplicação de código entre as classes de teste.
 */
@AutoConfigureMockMvc
public abstract class MockMvcIntegrationTestBase extends IntegrationTestBase {

    @Autowired
    protected MockMvc mockMvc;

    protected final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    // Gera um email único para evitar colisões entre testes paralelos
    protected String uniqueEmail() {
        return "test-" + UUID.randomUUID() + "@agro.com";
    }

    // Registra um novo usuário e retorna o access token JWT
    protected String registerAndGetToken(String email, String password) throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("name", "Usuário Teste");
        payload.put("email", email);
        payload.put("password", password);

        MvcResult result = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();
    }

    // Registra um usuário, cria uma conta e retorna o ID da conta
    protected String createAccount(String token, String accountName) throws Exception {
        Map<String, Object> payload = Map.of("name", accountName);

        MvcResult result = mockMvc.perform(post("/accounts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("id").asText();
    }

    // Cria uma lavoura simples (sem datas) e retorna o ID
    protected String createFarm(String token, String accountId, String farmName) throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("name", farmName);
        payload.put("areaValue", 50.0);
        payload.put("areaUnit", "HECTARE");
        payload.put("cancelled", false);

        MvcResult result = mockMvc.perform(
                        post("/accounts/" + accountId + "/farms")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("id").asText();
    }

    // Cria uma despesa vinculada a uma lavoura e retorna o ID
    protected String createExpense(String token, String accountId, String farmId,
                                   String description, String category,
                                   double value, String competenceDate) throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("description", description);
        payload.put("category", category);
        payload.put("value", value);
        payload.put("competenceDate", competenceDate);

        MvcResult result = mockMvc.perform(
                        post("/accounts/" + accountId + "/farms/" + farmId + "/expenses")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("id").asText();
    }
}